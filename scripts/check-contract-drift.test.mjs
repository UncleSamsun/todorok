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

test('운영체제에 따른 텍스트 줄바꿈 차이는 drift로 보지 않는다', async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), 'todorok-line-ending-'))
  const expected = path.join(root, 'expected')
  const actual = path.join(root, 'actual')
  await mkdir(expected)
  await mkdir(actual)
  await writeFile(path.join(expected, 'FILES'), 'one\ntwo\n')
  await writeFile(path.join(actual, 'FILES'), 'one\r\ntwo\r\n')

  assert.deepEqual(await compareTrees(expected, actual), [])
})
