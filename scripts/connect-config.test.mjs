import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { spawnSync } from 'node:child_process'
import test from 'node:test'

function composeServices() {
  const result = spawnSync('docker', [
    'compose', '--env-file', '.env.example',
    '-f', 'infra/docker/compose.yml', 'config', '--format', 'json',
  ], { encoding: 'utf8' })
  assert.equal(result.status, 0, result.stderr)
  return JSON.parse(result.stdout).services
}

test('Connect와 topic 초기화 순서가 고정된다', () => {
  const services = composeServices()
  assert.equal(services.connect.image, 'quay.io/debezium/connect:3.6.2.Final')
  assert.equal(services.connect.mem_limit, '805306368')
  assert.match(services.connect.environment.KAFKA_HEAP_OPTS, /-Xmx384m/)
  assert.equal(
    services.connect.depends_on['kafka-init'].condition,
    'service_completed_successfully',
  )
  assert.equal(
    services.connect.depends_on['replication-init'].condition,
    'service_completed_successfully',
  )
  assert.equal(services['connect-init'].depends_on.connect.condition, 'service_healthy')
  assert.equal(services['kafka-init'].depends_on.kafka.condition, 'service_healthy')
})

test('connector는 단일 slot과 Outbox Event Router만 사용한다', async () => {
  const connector = JSON.parse(await readFile(
    'infra/docker/connect/connector-template.json',
    'utf8',
  ))
  const config = connector.config
  assert.equal(connector.name, 'todorok-postgres-outbox')
  assert.equal(config['slot.name'], 'todorok_outbox_slot')
  assert.equal(config['publication.name'], 'todorok_outbox')
  assert.equal(config['publication.autocreate.mode'], 'disabled')
  assert.equal(
    config['table.include.list'],
    'planner.outbox_event,activity.outbox_event',
  )
  assert.equal(config['snapshot.mode'], 'when_needed')
  assert.equal(
    config['transforms.outbox.type'],
    'io.debezium.transforms.outbox.EventRouter',
  )
  assert.equal(
    config['transforms.outbox.route.topic.replacement'],
    'todorok.${routedByValue}.v1',
  )
  assert.equal(config['transforms.outbox.table.expand.json.payload'], 'true')
  assert.equal(config['transforms.outbox.table.op.invalid.behavior'], 'fatal')
  assert.equal(config['errors.tolerance'], 'none')
  assert.equal(config['errors.log.include.messages'], 'false')
})

test('connector 비밀번호는 JSON-safe하게 주입하고 갱신은 명시적으로만 허용한다', async () => {
  const script = await readFile('infra/docker/connect/register-connector.sh', 'utf8')
  assert.doesNotMatch(script, /envsubst/)
  assert.match(
    script,
    /jq --arg database "\$POSTGRES_DB" --arg password "\$DEBEZIUM_DB_PASSWORD"/,
  )
  assert.match(script, /CONNECTOR_CONFIG_UPDATE:-false/)

  const services = composeServices()
  assert.equal(services['connect-init'].environment.CONNECTOR_CONFIG_UPDATE, 'false')
})
