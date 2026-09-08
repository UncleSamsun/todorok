import { spawnSync } from 'node:child_process'

const gradleCommand = process.platform === 'win32' ? 'gradlew.bat' : './gradlew'
const commands = [
  ['node', ['--test',
    'scripts/contracts-generation.test.mjs',
    'scripts/openapi-contract.test.mjs',
    'scripts/check-contract-drift.test.mjs',
    'scripts/api-routing.test.mjs',
    'scripts/template-contract-roundtrip.test.mjs',
  ]],
  ['node', ['scripts/check-contract-drift.mjs']],
  ['node', ['--test',
    'scripts/persistence-compose.test.mjs',
    'scripts/connect-config.test.mjs',
    'scripts/postgres-messaging-config.test.mjs',
    'scripts/messaging-health.test.mjs',
    'scripts/connect-registration.integration.test.mjs',
    'scripts/compose-smoke.test.mjs',
    'scripts/check-record-policy.test.mjs',
    'scripts/template-nginx.integration.test.mjs',
  ]],
  [gradleCommand, ['test', '--no-daemon', '--max-workers=1']],
  ['corepack', ['pnpm', 'test:packages']],
  ['corepack', ['pnpm', 'test:web']],
  ['corepack', ['pnpm', 'build:packages']],
  ['corepack', ['pnpm', 'build:web']],
  ['node', ['scripts/check-bundle-budget.mjs']],
  ['node', ['--test', 'scripts/runtime-health.test.mjs']],
  ['docker', ['compose', '--env-file', '.env.example', '-f', 'infra/docker/compose.yml', 'config', '--quiet']],
  ['node', ['scripts/check-record-policy.mjs']],
]

for (const [command, args] of commands) {
  console.log(`\n> ${command} ${args.join(' ')}`)
  const needsWindowsCommandShell =
    process.platform === 'win32' && (command === 'corepack' || command.endsWith('.bat'))
  const executable = needsWindowsCommandShell ? 'cmd.exe' : command
  const executableArgs = needsWindowsCommandShell
    ? ['/d', '/s', '/c', `${command} ${args.join(' ')}`]
    : args
  const result = spawnSync(executable, executableArgs, {
    stdio: 'inherit',
  })
  if (result.status !== 0) {
    process.exit(result.status ?? 1)
  }
}

console.log('\n전체 검증을 통과했습니다.')
