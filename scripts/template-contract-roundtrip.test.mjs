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

test('생성 planner 선택 요청과 링크 응답 및 activity record-template은 identity를 보존한다', async () => {
  const output = await mkdtemp(path.join(os.tmpdir(), 'todorok-binding-contract-'))
  const uuid = n => `10000000-0000-0000-0000-${String(n).padStart(12, '0')}`
  const link = { bindingId: uuid(1), templateId: uuid(2), selectedTemplateVersion: 1, name: '선택 당시 정의', fieldSummary: '문제 (개)' }
  try {
    for (const service of ['planner', 'activity']) {
      const target = path.join(output, service)
      const compiled = spawnSync(process.execPath, [path.resolve('packages/api-client/node_modules/typescript/lib/tsc.js'),
        '-p', `packages/api-client/src/generated/${service}/tsconfig.json`, '--outDir', target], { encoding: 'utf8' })
      assert.equal(compiled.status, 0, compiled.stderr || compiled.stdout)
      const generated = createRequire(import.meta.url)(path.join(target, 'index.js'))
      const cases = service === 'planner' ? [
        ['CreateTaskRequest', { commandId: uuid(3), title: '공부', taskType: 'STUDY', scheduledDate: '2026-09-07',
          templateSelection: { templateId: uuid(2), expectedTemplateVersion: 1 } }],
        ['TaskResponse', { taskId: uuid(4), userId: uuid(5), title: '공부', taskType: 'STUDY', scheduledDate: '2026-09-07', status: 'PLANNED', version: 0, templateLink: link }],
      ] : [['TaskRecordTemplateResponse', { linked: true, templateLink: link, template: {
        templateId: uuid(2), domain: 'STUDY', kind: 'STUDY_CATEGORY', archived: true, revision: 2,
        currentVersion: { templateId: uuid(2), templateVersion: 2, name: '현재 정의', fields: [] },
      } }]]
      for (const [name, json] of cases) {
        assert.deepEqual(JSON.parse(JSON.stringify(generated[`${name}ToJSON`](generated[`${name}FromJSON`](json)))), json)
      }
    }
  } finally { await rm(output, { recursive: true, force: true }) }
})
