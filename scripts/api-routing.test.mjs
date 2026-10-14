import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

test('notification API는 별도 서비스로 전달한다', async () => {
  const nginx = await readFile('infra/nginx/nginx.conf', 'utf8')
  assert.match(nginx, /location \/api\/notification\/v1\/\s*\{[\s\S]*?proxy_pass http:\/\/notification_backend;/)
})

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

test('템플릿과 활동 기록 요청에 1 MiB 제한과 공통 413을 적용한다', async () => {
  const nginx = await readFile('infra/nginx/nginx.conf', 'utf8')
  const locations = [...nginx.matchAll(/location ~ (\S+) \{([^}]+)\}/g)]
  for (const request of ['/api/activity/v1/templates', '/api/activity/v1/templates/id/versions',
    '/api/activity/v1/activities', '/api/activity/v1/activities/id']) {
    const location = locations.find(([, pattern]) => new RegExp(pattern).test(request))
    assert.ok(location, `Missing route for ${request}`)
    assert.match(location[2], /client_max_body_size 1m;/)
    assert.match(location[2], /error_page 413 = @template_payload_too_large;/)
  }
  const limited = locations.filter(([, , body]) => body.includes('client_max_body_size'))
  for (const request of ['/api/activity/v1/templates-extra', '/api/activity/v1/internal/templates']) {
    assert.ok(limited.every(([, pattern]) => !new RegExp(pattern).test(request)))
  }
  assert.match(nginx, /location @template_payload_too_large[\s\S]*?application\/problem\+json;[\s\S]*?"code":"PAYLOAD_TOO_LARGE"/)
  const generalActivity = nginx.match(/location \/api\/activity\/ \{([\s\S]*?)\n        \}/)?.[1] ?? ''
  assert.doesNotMatch(generalActivity, /client_max_body_size|template_payload_too_large/)
})
