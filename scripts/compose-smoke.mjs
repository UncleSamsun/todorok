import { spawnSync } from 'node:child_process'
import { mkdtemp, rm, writeFile } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

export function buildSmokePlan({ projectName, envFile }) {
  const baseArgs = [
    'compose', '--project-name', projectName,
    '--env-file', envFile,
    '-f', 'infra/docker/compose.yml',
    '-f', 'infra/docker/compose.smoke.yml',
  ]
  return {
    baseArgs,
    cleanupArgs: [...baseArgs, 'down', '--volumes', '--remove-orphans'],
  }
}

export function combineSmokeErrors(primaryError, cleanupError) {
  if (primaryError && cleanupError) {
    return new AggregateError(
      [primaryError, cleanupError],
      'Compose smoke와 cleanup이 모두 실패했습니다.',
    )
  }
  return primaryError ?? cleanupError
}

function runDocker(args, options = {}) {
  const result = spawnSync('docker', args, {
    cwd: path.resolve('.'),
    encoding: 'utf8',
    timeout: options.timeout ?? 900_000,
    stdio: options.capture ? 'pipe' : 'inherit',
  })
  if (result.status !== 0) {
    throw new Error(result.stderr || result.stdout || `docker ${args.join(' ')} failed`)
  }
  return result.stdout?.trim() ?? ''
}

function compose(plan, args, options) {
  return runDocker([...plan.baseArgs, ...args], options)
}

function assertServiceCompleted(plan, service) {
  const output = compose(plan, ['ps', '--all', '--format', 'json', service], {
    capture: true,
  })
  const parsed = JSON.parse(output)
  const rows = Array.isArray(parsed) ? parsed : [parsed]
  if (rows.length !== 1 || rows[0].ExitCode !== 0) {
    throw new Error(`${service} did not complete successfully: ${output}`)
  }
}

function verifyRuntime(plan) {
  for (const service of [
    'postgres-provision', 'planner-migration', 'activity-migration',
    'notification-migration', 'replication-init', 'kafka-init', 'connect-init',
  ]) assertServiceCompleted(plan, service)

  const healthPaths = [
    '/health',
    '/api/planner/v1/actuator/health',
    '/api/activity/v1/actuator/health',
  ]
  for (const healthPath of healthPaths) {
    compose(plan, [
      'exec', '-T', 'nginx', 'wget', '--quiet', '--output-document=-',
      `http://127.0.0.1${healthPath}`,
    ], { capture: true })
  }

  const connector = JSON.parse(compose(plan, [
    'run', '--rm', '--no-deps', '--entrypoint', 'curl', 'connect-init',
    '--fail', '--show-error', '--silent',
    '--retry', '10', '--retry-all-errors', '--retry-delay', '1', '--max-time', '60',
    'http://connect:8083/connectors/todorok-postgres-outbox/status',
  ], { capture: true }))
  if (connector.connector?.state !== 'RUNNING'
      || connector.tasks?.some((task) => task.state !== 'RUNNING')
      || !connector.tasks?.length) {
    throw new Error(`connector is not running: ${JSON.stringify(connector)}`)
  }

  const slotActive = compose(plan, [
    'exec', '-T', 'postgres', 'sh', '-lc',
    'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc '
      + '"select active from pg_replication_slots where slot_name = '
      + "'todorok_outbox_slot'" + ';"',
  ], { capture: true })
  if (slotActive !== 't') throw new Error(`replication slot is not active: ${slotActive}`)
}

function insertMarker(plan) {
  compose(plan, [
    'exec', '-T', 'postgres', 'sh', '-lc',
    'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c '
      + "'create table if not exists planner.smoke_marker(id integer primary key); "
      + "insert into planner.smoke_marker(id) values (1) on conflict do nothing;'",
  ], { capture: true })
}

function verifyMarker(plan) {
  const count = compose(plan, [
    'exec', '-T', 'postgres', 'sh', '-lc',
    'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc '
      + "'select count(*) from planner.smoke_marker where id = 1;'",
  ], { capture: true })
  if (count !== '1') throw new Error(`existing volume marker was not preserved: ${count}`)
}

export async function runComposeSmoke() {
  const directory = await mkdtemp(path.join(os.tmpdir(), 'todorok-compose-smoke-'))
  const envFile = path.join(directory, 'smoke.env')
  const projectName = `todorok-smoke-${process.pid}-${Date.now()}`.toLowerCase()
  const plan = buildSmokePlan({ projectName, envFile })
  const skipBuild = process.argv.includes('--skip-build')
  await writeFile(envFile, [
    'POSTGRES_DB=todorok',
    'POSTGRES_USER=postgres',
    'POSTGRES_PASSWORD=smoke-admin-password',
    'PLANNER_DB_PASSWORD=smoke-planner-password',
    'ACTIVITY_DB_PASSWORD=smoke-activity-password',
    'NOTIFICATION_DB_PASSWORD=smoke-notification-password',
    'DEBEZIUM_DB_PASSWORD=smoke-debezium-password',
    'DATABASE_CREDENTIAL_UPDATE=false',
    'CONNECTOR_CONFIG_UPDATE=false',
    '',
  ].join('\n'))

  let primaryError
  try {
    const firstUp = ['up', '--detach']
    if (!skipBuild) firstUp.push('--build')
    firstUp.push('--wait', '--wait-timeout', '300')
    compose(plan, firstUp)
    verifyRuntime(plan)
    insertMarker(plan)

    compose(plan, ['down', '--remove-orphans'])
    compose(plan, ['up', '--detach', '--wait', '--wait-timeout', '300'])
    verifyRuntime(plan)
    verifyMarker(plan)
  } catch (error) {
    primaryError = error
  }

  let cleanupError
  try {
    runDocker(plan.cleanupArgs, { timeout: 300_000 })
  } catch (error) {
    cleanupError = error
  } finally {
    try {
      await rm(directory, { recursive: true, force: true })
    } catch (error) {
      cleanupError = combineSmokeErrors(cleanupError, error)
    }
  }

  const failure = combineSmokeErrors(primaryError, cleanupError)
  if (failure) throw failure
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  await runComposeSmoke()
  console.log('격리 Compose 전체 조립 검증을 통과했습니다.')
}
