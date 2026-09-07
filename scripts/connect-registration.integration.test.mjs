import assert from 'node:assert/strict'
import { spawn } from 'node:child_process'
import { readFile } from 'node:fs/promises'
import { createServer } from 'node:http'
import { after, before, test } from 'node:test'

const image = `todorok-connect-registration-test-${process.pid}`

before(async () => {
  const result = await run('docker', [
    'build', '--quiet', '--tag', image, 'infra/docker/connect',
  ])
  assert.equal(result.code, 0, result.stderr)
})

after(async () => {
  await run('docker', ['image', 'rm', '--force', image])
})

test('없는 connector를 JSON-safe 비밀번호로 생성한다', async () => {
  const password = 'quote"slash\\value'
  let created
  await withServer(async (request, response) => {
    if (request.method === 'GET') {
      response.writeHead(404).end('{}')
      return
    }
    created = await jsonBody(request)
    response.writeHead(201).end('{}')
  }, async (connectUrl) => {
    const result = await runContainer(connectUrl, password)
    assert.equal(result.code, 0, result.stderr)
  })

  assert.equal(created.config['database.password'], password)
  assert.equal(created.config['database.dbname'], 'todorok')
})

test('기존 connector drift를 명시적 갱신 없이 거부한다', async () => {
  await withServer(async (request, response) => {
    response.setHeader('Content-Type', 'application/json')
    response.end(JSON.stringify({
      'connector.class': 'wrong.Connector',
    }))
  }, async (connectUrl) => {
    const result = await runContainer(connectUrl, 'password')
    assert.notEqual(result.code, 0)
    assert.match(result.stderr, /connector config mismatch/)
  })
})

test('명시적 갱신에서 전체 config와 새 비밀번호를 PUT한다', async () => {
  const password = 'rotated"password\\value'
  let updated
  await withServer(async (request, response) => {
    response.setHeader('Content-Type', 'application/json')
    if (request.method === 'GET') {
      response.end(JSON.stringify({ 'connector.class': 'wrong.Connector' }))
      return
    }
    updated = await jsonBody(request)
    response.writeHead(200).end('{}')
  }, async (connectUrl) => {
    const result = await runContainer(connectUrl, password, true)
    assert.equal(result.code, 0, result.stderr)
  })

  assert.equal(updated['database.password'], password)
  assert.equal(updated['connector.class'], 'io.debezium.connector.postgresql.PostgresConnector')
})

test('Connect의 일시적인 빈 응답 뒤 config 조회를 재시도한다', async () => {
  const template = JSON.parse(await readFile(
    'infra/docker/connect/connector-template.json', 'utf8',
  ))
  const current = {
    ...template.config,
    'database.dbname': 'todorok',
    'database.password': 'password',
  }
  let attempts = 0
  await withServer(async (request, response) => {
    attempts += 1
    if (attempts === 1) {
      request.socket.destroy()
      return
    }
    response.setHeader('Content-Type', 'application/json')
    response.end(JSON.stringify(current))
  }, async (connectUrl) => {
    const result = await runContainer(connectUrl, 'password')
    assert.equal(result.code, 0, result.stderr)
  })
  assert.ok(attempts >= 2)
})

async function runContainer(connectUrl, password, update = false) {
  return run('docker', [
    'run', '--rm', '--add-host', 'host.docker.internal:host-gateway',
    '--env', `CONNECT_URL=${connectUrl}`,
    '--env', 'POSTGRES_DB=todorok',
    '--env', `DEBEZIUM_DB_PASSWORD=${password}`,
    '--env', `CONNECTOR_CONFIG_UPDATE=${update}`,
    image,
  ])
}

async function withServer(handler, action) {
  const server = createServer((request, response) => {
    Promise.resolve(handler(request, response)).catch((error) => {
      response.writeHead(500).end(error.stack)
    })
  })
  await new Promise((resolve) => server.listen(0, '0.0.0.0', resolve))
  try {
    const { port } = server.address()
    await action(`http://host.docker.internal:${port}`)
  } finally {
    await new Promise((resolve, reject) => server.close((error) => {
      if (error) reject(error)
      else resolve()
    }))
  }
}

async function jsonBody(request) {
  let body = ''
  for await (const chunk of request) body += chunk
  return JSON.parse(body)
}

function run(command, args) {
  return new Promise((resolve) => {
    const child = spawn(command, args, { shell: false })
    let stdout = ''
    let stderr = ''
    child.stdout.on('data', (chunk) => { stdout += chunk })
    child.stderr.on('data', (chunk) => { stderr += chunk })
    child.on('close', (code) => resolve({ code, stdout, stderr }))
  })
}
