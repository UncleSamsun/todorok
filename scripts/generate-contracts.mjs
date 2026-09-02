import { spawnSync } from 'node:child_process'
import path from 'node:path'
import process from 'node:process'

const outputIndex = process.argv.indexOf('--output-root')
if (outputIndex >= 0 && !process.argv[outputIndex + 1]) {
  process.stderr.write('--output-root 다음에 경로가 필요합니다.\n')
  process.exit(2)
}

const outputRoot = outputIndex >= 0
  ? path.resolve(process.argv[outputIndex + 1])
  : path.resolve('.')
const wrapper = process.platform === 'win32' ? 'gradlew.bat' : './gradlew'
const result = spawnSync(
  wrapper,
  [
    `-PcontractsOutputRoot=${outputRoot}`,
    'generateContracts',
    '--no-daemon',
    '--no-configuration-cache',
  ],
  { cwd: path.resolve('.'), encoding: 'utf8', shell: process.platform === 'win32' },
)

process.stdout.write(result.stdout ?? '')
process.stderr.write(result.stderr ?? '')
process.exit(result.status ?? 1)
