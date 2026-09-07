import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

test('Nginx가 planner와 activity 외부 API prefix를 보존한다', async () => {
  const nginx = await readFile('infra/nginx/nginx.conf', 'utf8')
  assert.match(
    nginx,
    /location \/api\/planner\/\s*\{[\s\S]*?proxy_pass http:\/\/planner_backend;/,
  )
  assert.match(
    nginx,
    /location \/api\/activity\/\s*\{[\s\S]*?proxy_pass http:\/\/activity_backend;/,
  )
  assert.doesNotMatch(nginx, /proxy_pass http:\/\/(planner|activity)_backend\//)
})

test('서비스 context path가 OpenAPI server 경로와 일치한다', async () => {
  const planner = await readFile(
    'services/planner-service/src/main/resources/application.yml',
    'utf8',
  )
  const activity = await readFile(
    'services/activity-service/src/main/resources/application.yml',
    'utf8',
  )
  assert.match(planner, /context-path: \/api\/planner\/v1/)
  assert.match(activity, /context-path: \/api\/activity\/v1/)
})
