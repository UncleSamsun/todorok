import { spawnSync } from 'node:child_process'

const gradleCommand = process.platform === 'win32' ? 'gradlew.bat' : './gradlew'
const commands = [
  [gradleCommand, ['test', '--no-daemon', '--max-workers=1']],
  ['corepack', ['pnpm', 'test:packages']],
  ['corepack', ['pnpm', 'test:web']],
  ['corepack', ['pnpm', 'build:packages']],
  ['corepack', ['pnpm', 'build:web']],
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
