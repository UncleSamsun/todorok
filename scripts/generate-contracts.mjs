import { spawnSync } from 'node:child_process'
import { readFile, readdir, writeFile } from 'node:fs/promises'
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
if (result.status !== 0) process.exit(result.status ?? 1)

for (const generatedRoot of [
  'services/planner-service/src/generated',
  'services/activity-service/src/generated',
  'packages/api-client/src/generated/planner',
  'packages/api-client/src/generated/activity',
]) {
  await normalizeFiles(path.join(outputRoot, generatedRoot))
}

async function normalizeFiles(directory) {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const target = path.join(directory, entry.name)
    if (entry.isDirectory()) {
      await normalizeFiles(target)
      continue
    }
    const content = await readFile(target, 'utf8')
    const normalized = content.replace(/[ \t]+$/gm, '')
    if (normalized !== content) await writeFile(target, normalized)
  }
}
