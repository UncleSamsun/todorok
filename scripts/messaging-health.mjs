import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { resolve } from 'node:path'

const GIB = 1024 ** 3
const DAY = 24 * 60 * 60
const CONNECTOR_NAME = 'todorok-postgres-outbox'
const TOPIC_POLICIES = new Map([
  ['todorok.task.v1', { retentionMs: '604800000', retentionBytes: '1073741824' }],
  ['todorok.activity.v1', { retentionMs: '604800000', retentionBytes: '1073741824' }],
  ['todorok.dead-letter', { retentionMs: '2592000000', retentionBytes: '1073741824' }],
])

export function evaluateMessagingHealth(state) {
  const critical = []
  const warning = []

  if (state.connectorState !== 'RUNNING') critical.push('connector_not_running')
  if (!state.taskStates?.length || state.taskStates.some((value) => value !== 'RUNNING')) {
    critical.push('connector_task_not_running')
  }
  if (!state.slotActive) critical.push('slot_inactive')
  if (state.retainedWalBytes >= 2 * GIB) critical.push('wal_limit_reached')
  else if (state.retainedWalBytes >= 1.5 * GIB) warning.push('wal_retention_high')
  if (!state.topicPoliciesValid) critical.push('topic_policy_invalid')
  if (state.consumerLag > 100) critical.push('consumer_lag_high')
  else if (state.consumerLag > 0) warning.push('consumer_lag_detected')
  if (state.outboxOldestAgeSeconds >= 7 * DAY) warning.push('outbox_retention_old')
  if (state.inboxOldestAgeSeconds >= 30 * DAY) warning.push('inbox_retention_old')

  if (critical.length) return { status: 'critical', reasons: [...critical, ...warning] }
  if (warning.length) return { status: 'warning', reasons: warning }
  return { status: 'healthy', reasons: [] }
}

function composeArgs(args) {
  const envFile = process.env.MESSAGING_ENV_FILE ?? '.env'
  return ['compose', '--env-file', envFile, '-f', 'infra/docker/compose.yml', ...args]
}

function runCompose(args, input) {
  const result = spawnSync('docker', composeArgs(args), {
    encoding: 'utf8',
    input,
  })
  if (result.status !== 0) {
    throw new Error(result.stderr || result.stdout || `docker compose ${args.join(' ')} failed`)
  }
  return result.stdout
}

async function readConnectorState() {
  const connectUrl = process.env.CONNECT_URL ?? 'http://localhost:8084'
  const response = await fetch(`${connectUrl}/connectors/${CONNECTOR_NAME}/status`)
  if (!response.ok) throw new Error(`Connect status failed: HTTP ${response.status}`)
  const status = await response.json()
  return {
    connectorState: status.connector?.state ?? 'UNKNOWN',
    taskStates: (status.tasks ?? []).map((task) => task.state ?? 'UNKNOWN'),
  }
}

function readDatabaseState() {
  const query = `
with slot_state as (
  select active,
         coalesce(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn), 0) as retained_wal_bytes
    from pg_replication_slots
   where slot_name = 'todorok_outbox_slot'
), outbox_age as (
  select coalesce(extract(epoch from clock_timestamp() - min(recorded_at)), 0) as seconds
    from (
      select min(created_at) as recorded_at from planner.outbox_event
      union all
      select min(created_at) from activity.outbox_event
    ) records
), inbox_age as (
  select coalesce(extract(epoch from clock_timestamp() - min(recorded_at)), 0) as seconds
    from (
      select min(processed_at) as recorded_at from planner.processed_event
      union all
      select min(processed_at) from activity.processed_event
      union all
      select min(processed_at) from notification.processed_event
    ) records
)
select coalesce((select active from slot_state), false),
       coalesce((select retained_wal_bytes from slot_state), 0),
       (select seconds from outbox_age),
       (select seconds from inbox_age);
`
  const output = runCompose([
    'exec', '-T', 'postgres', 'sh', '-lc',
    'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -F , -f -',
  ], query).trim()
  const [slotActive, retainedWalBytes, outboxAge, inboxAge] = output.split(',')
  return {
    slotActive: slotActive === 't',
    retainedWalBytes: Number(retainedWalBytes),
    outboxOldestAgeSeconds: Number(outboxAge),
    inboxOldestAgeSeconds: Number(inboxAge),
  }
}

function readTopicPolicies() {
  for (const [topic, expected] of TOPIC_POLICIES) {
    const output = runCompose([
      'exec', '-T', 'kafka', '/opt/kafka/bin/kafka-configs.sh',
      '--bootstrap-server', 'localhost:9092', '--entity-type', 'topics',
      '--entity-name', topic, '--describe',
    ])
    if (!output.includes(`retention.ms=${expected.retentionMs}`)
        || !output.includes(`retention.bytes=${expected.retentionBytes}`)) {
      return false
    }
  }
  return true
}

function readConsumerLag() {
  const output = runCompose([
    'exec', '-T', 'kafka', '/opt/kafka/bin/kafka-consumer-groups.sh',
    '--bootstrap-server', 'localhost:9092', '--all-groups', '--describe',
  ])
  const lines = output.split(/\r?\n/).filter(Boolean)
  const header = lines.find((line) => /\bLAG\b/.test(line))
  if (!header) return 0
  const columns = header.trim().split(/\s+/)
  const lagIndex = columns.indexOf('LAG')
  return lines.slice(lines.indexOf(header) + 1).reduce((sum, line) => {
    const fields = line.trim().split(/\s+/)
    const value = Number(fields[lagIndex])
    return sum + (Number.isFinite(value) ? value : 0)
  }, 0)
}

export async function readMessagingHealth() {
  const state = {
    ...await readConnectorState(),
    ...readDatabaseState(),
    topicPoliciesValid: readTopicPolicies(),
    consumerLag: readConsumerLag(),
  }
  return { ...state, ...evaluateMessagingHealth(state) }
}

async function main() {
  try {
    const result = await readMessagingHealth()
    console.log(JSON.stringify(result, null, 2))
    if (result.status === 'critical') process.exitCode = 1
  } catch (error) {
    console.error(JSON.stringify({
      status: 'critical',
      reasons: ['health_probe_failed'],
      error: error.message,
    }, null, 2))
    process.exitCode = 1
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  await main()
}
