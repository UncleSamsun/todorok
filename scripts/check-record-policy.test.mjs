import assert from 'node:assert/strict'
import { mkdtemp, rm, writeFile } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import test from 'node:test'

const checker = path.resolve('scripts/check-record-policy.mjs')

for (const scenario of [
  { name: 'untracked file', prepare: (root) => writeFile(path.join(root, 'new.txt'), 'blocked') },
  {
    name: 'staged diff',
    prepare: async (root) => {
      await writeFile(path.join(root, 'staged.txt'), 'blocked')
      git(root, 'add', 'staged.txt')
    },
  },
  {
    name: 'working tree diff',
    prepare: async (root) => {
      await writeFile(path.join(root, 'safe.txt'), 'blocked')
    },
  },
]) {
  test(`${scenario.name}의 정책 위반을 거부한다`, async () => {
    const root = await repository()
    try {
      await scenario.prepare(root)
      const result = spawnSync(process.execPath, [checker], {
        cwd: root,
        encoding: 'utf8',
        env: { ...process.env, RECORD_POLICY_FORBIDDEN_TERMS: 'blocked' },
      })
      assert.notEqual(result.status, 0)
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })
}

async function repository() {
  const root = await mkdtemp(path.join(os.tmpdir(), 'record-policy-'))
  git(root, 'init')
  git(root, 'config', 'user.name', 'Test User')
  git(root, 'config', 'user.email', 'test@example.com')
  await writeFile(path.join(root, 'safe.txt'), 'safe')
  git(root, 'add', 'safe.txt')
  git(root, 'commit', '-m', 'safe commit')
  return root
}

function git(root, ...args) {
  const result = spawnSync('git', args, { cwd: root, encoding: 'utf8' })
  assert.equal(result.status, 0, result.stderr)
}
