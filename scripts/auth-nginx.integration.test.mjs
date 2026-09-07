import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import test from 'node:test'

test('실제 Nginx 인증 제한은 공통 429와 trace 및 Retry-After를 반환한다', async () => {
  const name = `todorok-auth-nginx-${process.pid}-${Date.now()}`
  const docker = (...args) => {
    const result = spawnSync('docker', args, { encoding: 'utf8', timeout: 120_000 })
    assert.equal(result.status, 0, result.stderr || result.stdout)
    return result.stdout.trim()
  }
  try {
    docker('run', '-d', '--name', name, '--add-host', 'planner-service:127.0.0.1',
      '--add-host', 'activity-service:127.0.0.1', '-p', '127.0.0.1::80',
      '--mount', `type=bind,src=${path.resolve('infra/nginx/nginx.conf')},dst=/etc/nginx/nginx.conf,readonly`,
      'nginx:1.28.0-alpine')
    docker('exec', name, 'nginx', '-t')
    const address = docker('port', name, '80/tcp')
    let limited
    for (let i = 0; i < 20; i++) {
      const response = await fetch(`http://${address}/api/planner/v1/auth/login`, {
        method: 'POST', headers: { Origin: 'https://todorok.test', 'Content-Type': 'application/json' }, body: '{}',
      })
      if (response.status === 429) { limited = response; break }
      await response.text()
    }
    assert.ok(limited, 'auth route should be rate-limited after burst capacity is consumed')
    assert.match(limited.headers.get('content-type'), /^application\/problem\+json/)
    assert.equal(limited.headers.get('retry-after'), '60')
    const body = await limited.json()
    assert.equal(body.code, 'RATE_LIMITED')
    assert.equal(body.status, 429)
    assert.equal(body.retryable, true)
    assert.equal(body.traceId, limited.headers.get('x-trace-id'))
    assert.match(body.traceId, /^[a-f0-9]{32}$/)
  } finally {
    docker('rm', '-f', name)
  }
})
