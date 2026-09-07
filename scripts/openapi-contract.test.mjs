import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

test('planner 계약은 핵심 operationId와 공통 오류 참조를 제공한다', async () => {
  const yaml = await readFile('contracts/openapi/planner-v1.yaml', 'utf8')
  for (const operationId of [
    'login',
    'refreshSession',
    'logout',
    'getCalendarSummary',
    'getDayDetail',
    'createTask',
    'updateTask',
    'deleteTask',
    'completeTask',
    'reopenTask',
    'getDailyNote',
    'updateDailyNote',
  ]) {
    assert.match(yaml, new RegExp(`operationId: ${operationId}\\b`))
  }
  assert.match(yaml, /common-v1\.yaml#\/components\/schemas\/ProblemDetails/)
  assert.match(yaml, /enum: \[GENERAL, WORKOUT, STUDY, CLIMBING\]/)
  assert.doesNotMatch(yaml, /enum: \[TODO, WORKOUT, STUDY, CLIMBING\]/)
  assert.match(yaml, /required: \[date, totalCount, completedCount, categoryProgress\]/)
  assert.match(yaml, /CategoryProgress:/)
})

test('activity 계약은 생성·조회·목록·무효화 operation을 제공한다', async () => {
  const yaml = await readFile('contracts/openapi/activity-v1.yaml', 'utf8')
  for (const operationId of [
    'createActivity',
    'getActivity',
    'listActivities',
    'voidActivity',
    'createTemplate',
    'listTemplates',
    'getTemplate',
    'getTemplateVersion',
    'createTemplateVersion',
    'archiveTemplate',
  ]) {
    assert.match(yaml, new RegExp(`operationId: ${operationId}\\b`))
  }
  assert.match(yaml, /activityType:/)
  assert.match(yaml, /additionalProperties: true/)
  assert.match(yaml, /enum: \[COMPLETED, PARTIAL, VOIDED\]/)
  assert.match(
    yaml,
    /required: \[commandId, taskId, activityType, completionStatus, performedAt, detail\]/,
  )
  assert.match(yaml, /completionStatus:/)
  assert.match(yaml, /enum: \[NUMBER, TIME, SHORT_TEXT, CHECK, MEMO\]/)
  assert.match(yaml, /enum: \[STUDY_CATEGORY, FREE_WORKOUT, FREE_HANGBOARD, CLIMBING_SESSION\]/)
  assert.match(yaml, /CreateTemplateRequest:[\s\S]*?additionalProperties: false/)
  assert.match(yaml, /FieldDefinitionInput:[\s\S]*?additionalProperties: false/)
  assert.match(yaml, /maximum: 100, default: 20/)
  assert.doesNotMatch(yaml.match(/FieldDefinitionInput:[\s\S]*?FieldDefinition:/)?.[0] ?? '', /maxItems:/)
})
