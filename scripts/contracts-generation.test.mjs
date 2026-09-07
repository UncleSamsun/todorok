import assert from 'node:assert/strict'
import { access, mkdir, mkdtemp, readFile, readdir, stat, writeFile } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import test from 'node:test'

test('계약 생성기가 결정된 출력 루트를 만든다', async () => {
  const outputRoot = await mkdtemp(path.join(os.tmpdir(), 'todorok-contracts-'))
  const staleFile = path.join(
    outputRoot,
    'services/planner-service/src/generated/java/Stale.java',
  )
  await mkdir(path.dirname(staleFile), { recursive: true })
  await writeFile(staleFile, 'final class Stale {}')
  const result = spawnSync(
    process.execPath,
    ['scripts/generate-contracts.mjs', '--output-root', outputRoot],
    { cwd: path.resolve('.'), encoding: 'utf8' },
  )

  assert.equal(result.status, 0, result.stderr)
  await stat(path.join(outputRoot, 'services/planner-service/src/generated/java'))
  await stat(path.join(outputRoot, 'services/activity-service/src/generated/java'))
  await stat(path.join(outputRoot, 'packages/api-client/src/generated/planner'))
  await stat(path.join(outputRoot, 'packages/api-client/src/generated/activity'))
  await assert.rejects(access(staleFile), { code: 'ENOENT' })
  await assert.rejects(
    access(path.join(outputRoot, 'packages/api-client/src/generated/planner/.gitignore')),
    { code: 'ENOENT' },
  )

  for (const generatedRoot of [
    'libs/web-support/src/generated',
    'libs/event-contracts/src/generated',
    'services/planner-service/src/generated',
    'services/activity-service/src/generated',
    'packages/api-client/src/generated/planner',
    'packages/api-client/src/generated/activity',
  ]) {
    for (const file of await filesUnder(path.join(outputRoot, generatedRoot))) {
      const content = await readFile(file, 'utf8')
      assert.doesNotMatch(content, /[ \t]+$/m, `${file}에 행 끝 공백이 있습니다.`)
      assert.ok(content.endsWith('\n'), `${file}의 마지막 줄은 LF로 끝나야 합니다.`)
      assert.doesNotMatch(content, /[\r\n]\n$/, `${file}의 EOF에는 LF 한 개만 있어야 합니다.`)
    }
  }
})

async function filesUnder(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    const target = path.join(directory, entry.name)
    if (entry.isDirectory()) files.push(...await filesUnder(target))
    else files.push(target)
  }
  return files
}
