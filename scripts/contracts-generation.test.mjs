import assert from 'node:assert/strict'
import { mkdtemp, stat } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import test from 'node:test'

test('네 계약 생성기가 결정된 출력 루트를 만든다', async () => {
  const outputRoot = await mkdtemp(path.join(os.tmpdir(), 'todorok-contracts-'))
  const result = spawnSync(
    process.execPath,
    ['scripts/generate-contracts.mjs', '--output-root', outputRoot],
    { cwd: path.resolve('.'), encoding: 'utf8' },
  )

  assert.equal(result.status, 0, result.stderr)
  await stat(path.join(outputRoot, 'services/planner-service/src/generated/java'))
  await stat(path.join(outputRoot, 'services/activity-service/src/generated/java'))
  await stat(path.join(outputRoot, 'packages/api-client/src/generated/planner'))
  await stat(path.join(outputRoot, 'packages/api-client/src/generated/activity'))
})
