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

test('템플릿 요청 크기 제한과 공통 413은 템플릿 경로에만 적용된다', async () => {
  const nginx = await readFile('infra/nginx/nginx.conf', 'utf8')
  assert.match(
    nginx,
    /location ~ \^\/api\/activity\/v1\/templates[\s\S]*?client_max_body_size 1m;[\s\S]*?error_page 413 = @template_payload_too_large;/,
  )
  assert.match(nginx, /location @template_payload_too_large[\s\S]*?application\/problem\+json;[\s\S]*?"code":"PAYLOAD_TOO_LARGE"/)
  const generalActivity = nginx.match(/location \/api\/activity\/ \{([\s\S]*?)\n        \}/)?.[1] ?? ''
  assert.doesNotMatch(generalActivity, /client_max_body_size|template_payload_too_large/)
})
