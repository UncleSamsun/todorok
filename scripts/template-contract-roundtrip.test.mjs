import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import { mkdtemp, rm } from 'node:fs/promises'
import { createRequire } from 'node:module'
import os from 'node:os'
import path from 'node:path'
import test from 'node:test'

test('생성 TypeScript 템플릿 응답은 version과 fields를 함께 왕복한다', async () => {
  const output = await mkdtemp(path.join(os.tmpdir(), 'todorok-template-contract-'))
  try {
    const result = spawnSync(process.execPath, [
      path.resolve('packages/api-client/node_modules/typescript/lib/tsc.js'),
      '-p', 'packages/api-client/src/generated/activity/tsconfig.json',
      '--outDir', output,
    ], { encoding: 'utf8' })
    assert.equal(result.status, 0, result.stderr || result.stdout)

    const generated = createRequire(import.meta.url)(path.join(output, 'index.js'))
    const response = {
      templateId: '10000000-0000-0000-0000-000000000001',
      domain: 'STUDY',
      kind: 'STUDY_CATEGORY',
      archived: false,
      revision: 2,
      currentVersion: {
        templateId: '10000000-0000-0000-0000-000000000001',
        templateVersion: 3,
        name: '알고리즘',
        fields: [
          { fieldId: '20000000-0000-0000-0000-000000000001', name: '문제 수', type: 'NUMBER', unit: '문제', position: 0 },
          { fieldId: '20000000-0000-0000-0000-000000000002', name: '복습', type: 'CHECK', unit: undefined, position: 1 },
        ],
      },
    }
    const json = JSON.parse(JSON.stringify(generated.TemplateResponseToJSON(response)))
    assert.deepEqual(generated.TemplateResponseFromJSON(json), response)
  } finally {
    await rm(output, { recursive: true, force: true })
  }
})
