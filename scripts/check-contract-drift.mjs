import { createHash } from 'node:crypto'
import { mkdtemp, readFile, readdir, rm } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { spawnSync } from 'node:child_process'

async function filesUnder(root, relative = '') {
  let entries
  try {
    entries = await readdir(path.join(root, relative), { withFileTypes: true })
  } catch (error) {
    if (error?.code === 'ENOENT') return []
    throw error
  }

  const files = []
  for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
    const child = path.join(relative, entry.name)
    if (entry.isDirectory()) {
      files.push(...await filesUnder(root, child))
    } else if (entry.isFile()) {
      files.push(child.split(path.sep).join('/'))
    }
  }
  return files
}

async function digest(file) {
  const content = await readFile(file)
  let comparable = content
  if (!content.includes(0)) {
    try {
      const text = new TextDecoder('utf-8', { fatal: true }).decode(content)
      comparable = Buffer.from(text.replace(/\r\n?/g, '\n'))
    } catch {
      comparable = content
    }
  }
  return createHash('sha256').update(comparable).digest('hex')
}

export async function compareTrees(expected, actual) {
  const expectedFiles = await filesUnder(expected)
  const actualFiles = await filesUnder(actual)
  const paths = [...new Set([...expectedFiles, ...actualFiles])].sort()
  const differences = []

  for (const relative of paths) {
    if (!expectedFiles.includes(relative) || !actualFiles.includes(relative)) {
      differences.push(relative)
      continue
    }
    const expectedHash = await digest(path.join(expected, relative))
    const actualHash = await digest(path.join(actual, relative))
    if (expectedHash !== actualHash) differences.push(relative)
  }
  return differences
}

async function main() {
  const repositoryRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
  const generatedRoot = await mkdtemp(path.join(os.tmpdir(), 'todorok-contracts-check-'))
  const targets = [
    'services/planner-service/src/generated',
    'services/activity-service/src/generated',
    'packages/api-client/src/generated/planner',
    'packages/api-client/src/generated/activity',
  ]

  try {
    const result = spawnSync(
      process.execPath,
      ['scripts/generate-contracts.mjs', '--output-root', generatedRoot],
      { cwd: repositoryRoot, encoding: 'utf8' },
    )
    if (result.status !== 0) {
      process.stdout.write(result.stdout ?? '')
      process.stderr.write(result.stderr ?? '')
      process.exitCode = result.status ?? 1
      return
    }

    const differences = []
    for (const target of targets) {
      for (const relative of await compareTrees(
        path.join(repositoryRoot, target),
        path.join(generatedRoot, target),
      )) {
        differences.push(`${target}/${relative}`)
      }
    }

    if (differences.length > 0) {
      process.stderr.write('생성 계약이 원본과 다릅니다. 다음 파일을 확인하세요.\n')
      for (const difference of differences) process.stderr.write(`- ${difference}\n`)
      process.exitCode = 1
      return
    }
    process.stdout.write('생성 계약이 원본과 일치합니다.\n')
  } finally {
    await rm(generatedRoot, { recursive: true, force: true })
  }
}

if (process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url) {
  await main()
}
