import assert from 'node:assert/strict'
import { mkdtemp, mkdir, writeFile } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import test from 'node:test'
import { compareTrees } from './check-contract-drift.mjs'

test('생성 파일 내용이 다르면 경로를 반환한다', async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), 'todorok-drift-'))
  const expected = path.join(root, 'expected')
  const actual = path.join(root, 'actual')
  await mkdir(expected)
  await mkdir(actual)
  await writeFile(path.join(expected, 'Api.java'), 'version-one')
  await writeFile(path.join(actual, 'Api.java'), 'version-two')

  assert.deepEqual(await compareTrees(expected, actual), ['Api.java'])
})
