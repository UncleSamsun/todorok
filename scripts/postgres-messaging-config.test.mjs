import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import test from 'node:test'

test('PostgreSQL logical replication과 초기화 순서가 고정된다', () => {
  const result = spawnSync('docker', [
    'compose', '--env-file', '.env.example',
    '-f', 'infra/docker/compose.yml', 'config', '--format', 'json',
  ], { encoding: 'utf8' })
  assert.equal(result.status, 0, result.stderr)
  const services = JSON.parse(result.stdout).services

  assert.deepEqual(services.postgres.command, [
    'postgres',
    '-c', 'wal_level=logical',
    '-c', 'max_wal_senders=1',
    '-c', 'max_replication_slots=1',
    '-c', 'max_slot_wal_keep_size=2048MB',
  ])
  assert.equal(services['replication-init'].restart, 'no')
  assert.equal(
    services['replication-init'].depends_on['planner-migration'].condition,
    'service_completed_successfully',
  )
  assert.equal(
    services['replication-init'].depends_on['activity-migration'].condition,
    'service_completed_successfully',
  )
  assert.equal(
    services['replication-init'].environment.DEBEZIUM_DB_PASSWORD,
    'replace-with-debezium-password',
  )
})
