import assert from 'node:assert/strict'
import test from 'node:test'
import { mkdtemp, readFile, rm } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { sign, verify } from 'node:crypto'

import * as smoke from './compose-smoke.mjs'

test('smoke 인증 fixture는 실행마다 새 키를 만들고 공개키로 서명을 검증한다', async () => {
  const directory = await mkdtemp(path.join(os.tmpdir(), 'todorok-auth-fixture-'))
  try {
    const fixture = await smoke.provisionSmokeAuth(directory)
    const privateKey = await readFile(fixture.privateKeyFile, 'utf8')
    const publicKey = await readFile(fixture.publicKeyFile, 'utf8')
    const message = Buffer.from('smoke-auth-verification')
    assert.ok(verify('RSA-SHA256', message, publicKey, sign('RSA-SHA256', message, privateKey)))
    assert.match(privateKey, /BEGIN PRIVATE KEY/)
    assert.equal(fixture.origin, 'http://localhost')
  } finally {
    await rm(directory, { recursive: true, force: true })
  }
})

test('smoke 계획은 격리 project와 volume cleanup을 고정한다', () => {
  const plan = smoke.buildSmokePlan({
    projectName: 'todorok-smoke-test',
    envFile: 'C:/temp/smoke.env',
  })

  assert.deepEqual(plan.baseArgs, [
    'compose', '--project-name', 'todorok-smoke-test',
    '--env-file', 'C:/temp/smoke.env',
    '-f', 'infra/docker/compose.yml',
    '-f', 'infra/docker/compose.smoke.yml',
  ])
  assert.deepEqual(plan.cleanupArgs, [
    ...plan.baseArgs,
    'down', '--volumes', '--remove-orphans',
  ])
})

test('smoke 실패와 cleanup 실패를 모두 보존한다', () => {
  const smokeError = new Error('smoke failed')
  const cleanupError = new Error('cleanup failed')
  const combined = smoke.combineSmokeErrors(smokeError, cleanupError)

  assert.ok(combined instanceof AggregateError)
  assert.deepEqual(combined.errors, [smokeError, cleanupError])
  assert.equal(smoke.combineSmokeErrors(smokeError), smokeError)
  assert.equal(smoke.combineSmokeErrors(undefined, cleanupError), cleanupError)
})
