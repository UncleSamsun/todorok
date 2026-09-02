import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import test from 'node:test'

test('각 서비스는 자기 migration 성공 뒤 시작한다', () => {
  const result = spawnSync('docker', [
    'compose',
    '--env-file',
    '.env.example',
    '-f',
    'infra/docker/compose.yml',
    'config',
    '--format',
    'json',
  ], { encoding: 'utf8' })

  assert.equal(result.status, 0, result.stderr)
  const services = JSON.parse(result.stdout).services
  for (const name of ['planner', 'activity', 'notification']) {
    const migration = services[`${name}-migration`]
    const application = services[`${name}-service`]
    assert.deepEqual(migration.entrypoint, [
      'java',
      '-jar',
      '/app/migration.jar',
      '--spring.profiles.active=migration',
    ])
    assert.equal(
      application.depends_on[`${name}-migration`].condition,
      'service_completed_successfully',
    )
    assert.equal(application.environment.SPRING_FLYWAY_ENABLED, 'false')
  }
})
