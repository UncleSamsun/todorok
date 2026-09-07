import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import http from 'node:http'
import path from 'node:path'
import test from 'node:test'

const MEBIBYTE = 1_048_576

test('실제 Nginx 템플릿 경로는 1 MiB를 허용하고 chunked 초과를 공통 413으로 거절한다', async () => {
  const name = `todorok-template-nginx-${process.pid}-${Date.now()}`
  const backend = `${name}-backend`
  const network = `${name}-network`
  const docker = (...args) => {
    const result = spawnSync('docker', args, { encoding: 'utf8', timeout: 120_000 })
    assert.equal(result.status, 0, result.stderr || result.stdout)
    return result.stdout.trim()
  }
  try {
    docker('network', 'create', network)
    docker('run', '-d', '--name', backend, '--network', network,
      '--network-alias', 'activity-service', '--network-alias', 'planner-service',
      'node:24-alpine', 'node', '-e',
      "require('http').createServer((q,s)=>{let n=0;q.on('data',c=>n+=c.length);q.on('end',()=>{s.setHeader('content-type','application/json');s.end(JSON.stringify({bytes:n}))})}).listen(8082)")
    docker('run', '-d', '--name', name, '--network', network,
      '-p', '127.0.0.1::80',
      '--mount', `type=bind,src=${path.resolve('infra/nginx/nginx.conf')},dst=/etc/nginx/nginx.conf,readonly`,
      'nginx:1.28.0-alpine')
    docker('exec', name, 'nginx', '-t')
    const address = docker('port', name, '80/tcp')
    const [host, port] = address.split(':')

    const exact = await post(host, Number(port), Buffer.alloc(MEBIBYTE, 0x20), true)
    assert.equal(exact.status, 200)
    assert.deepEqual(JSON.parse(exact.body), { bytes: MEBIBYTE })

    const over = await post(host, Number(port), Buffer.alloc(MEBIBYTE + 1, 0x20), false)
    assert.equal(over.status, 413)
    assert.match(over.headers['content-type'], /^application\/problem\+json/)
    const body = JSON.parse(over.body)
    assert.equal(body.code, 'PAYLOAD_TOO_LARGE')
    assert.equal(body.status, 413)
    assert.equal(body.retryable, false)
    assert.equal(body.traceId, over.headers['x-trace-id'])
    assert.match(body.traceId, /^[a-f0-9]{32}$/)
  } finally {
    spawnSync('docker', ['rm', '-f', name, backend], { encoding: 'utf8', timeout: 120_000 })
    spawnSync('docker', ['network', 'rm', network], { encoding: 'utf8', timeout: 120_000 })
  }
})

function post(host, port, body, contentLength) {
  return new Promise((resolve, reject) => {
    const request = http.request({
      host,
      port,
      path: '/api/activity/v1/templates',
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        ...(contentLength ? { 'content-length': body.length } : {}),
      },
    }, response => {
      const chunks = []
      response.on('data', chunk => chunks.push(chunk))
      response.on('end', () => resolve({
        status: response.statusCode,
        headers: response.headers,
        body: Buffer.concat(chunks).toString('utf8'),
      }))
    })
    request.on('error', reject)
    for (let offset = 0; offset < body.length; offset += 16_384) {
      request.write(body.subarray(offset, Math.min(offset + 16_384, body.length)))
    }
    request.end()
  })
}
