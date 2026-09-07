import assert from 'node:assert/strict'
import test from 'node:test'

import * as smoke from './compose-smoke.mjs'

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
