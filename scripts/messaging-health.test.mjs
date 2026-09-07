import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

import { evaluateMessagingHealth } from './messaging-health.mjs'
import * as healthModule from './messaging-health.mjs'

function healthyState(overrides = {}) {
  return {
    connectorState: 'RUNNING',
    taskStates: ['RUNNING'],
    slotActive: true,
    retainedWalBytes: 0,
    topicPoliciesValid: true,
    consumerLag: 0,
    outboxOldestAgeSeconds: 0,
    inboxOldestAgeSeconds: 0,
    outboxRowCount: 0,
    inboxRowCount: 0,
    ...overrides,
  }
}

test('중단 connector와 inactive slot, WAL 상한 도달은 critical이다', () => {
  assert.deepEqual(evaluateMessagingHealth(healthyState({
    connectorState: 'FAILED',
    slotActive: false,
    retainedWalBytes: 2 * 1024 ** 3,
  })), {
    status: 'critical',
    reasons: [
      'connector_not_running',
      'slot_inactive',
      'wal_limit_reached',
    ],
  })
})

test('WAL 1.5GB부터 warning이다', () => {
  assert.deepEqual(evaluateMessagingHealth(healthyState({
    retainedWalBytes: 1.5 * 1024 ** 3,
  })), {
    status: 'warning',
    reasons: ['wal_retention_high'],
  })
})

test('오래된 outbox와 inbox는 삭제하지 않고 warning으로 드러낸다', () => {
  assert.deepEqual(evaluateMessagingHealth(healthyState({
    outboxOldestAgeSeconds: 8 * 24 * 60 * 60,
    inboxOldestAgeSeconds: 31 * 24 * 60 * 60,
  })), {
    status: 'warning',
    reasons: ['outbox_retention_old', 'inbox_retention_old'],
  })
})

test('task와 topic, consumer lag 이상을 모두 보고한다', () => {
  assert.deepEqual(evaluateMessagingHealth(healthyState({
    taskStates: ['FAILED'],
    topicPoliciesValid: false,
    consumerLag: 101,
  })), {
    status: 'critical',
    reasons: [
      'connector_task_not_running',
      'topic_policy_invalid',
      'consumer_lag_high',
    ],
  })
})

test('보존 점검 SQL은 메시지 데이터를 변경하지 않는다', async () => {
  const sql = await readFile(
    'infra/docker/postgres/maintenance/inspect-messaging-retention.sql',
    'utf8',
  )
  assert.match(sql, /planner\.outbox_event/)
  assert.match(sql, /notification\.processed_event/)
  assert.doesNotMatch(sql, /\b(delete|update|truncate|drop)\b/i)
})

test('읽을 수 없는 수치를 critical로 판정한다', () => {
  assert.deepEqual(evaluateMessagingHealth(healthyState({
    retainedWalBytes: Number.NaN,
    consumerLag: Number.NaN,
  })), {
    status: 'critical',
    reasons: ['measurement_invalid'],
  })
})

test('PostgreSQL 상태 출력에 건수와 유효한 수치가 모두 있어야 한다', () => {
  assert.deepEqual(healthModule.parseDatabaseState('t,12,30.5,40.5,3,4'), {
    slotActive: true,
    retainedWalBytes: 12,
    outboxOldestAgeSeconds: 30.5,
    inboxOldestAgeSeconds: 40.5,
    outboxRowCount: 3,
    inboxRowCount: 4,
  })
  assert.throws(() => healthModule.parseDatabaseState('t,broken,0,0,0,0'))
})

test('topic 보존 값은 완전한 key/value로 비교한다', () => {
  const expected = { retentionMs: '604800000', retentionBytes: '1073741824' }
  assert.equal(healthModule.topicPolicyMatches(
    'Dynamic configs are retention.ms=604800000,retention.bytes=1073741824 sensitive=false',
    expected,
  ), true)
  assert.equal(healthModule.topicPolicyMatches(
    'Dynamic configs are retention.ms=6048000000,retention.bytes=10737418240 sensitive=false',
    expected,
  ), false)
})

test('consumer lag 출력 형식을 알 수 없으면 실패한다', () => {
  assert.equal(healthModule.parseConsumerLag('No consumer groups found.'), 0)
  assert.equal(healthModule.parseConsumerLag(`
GROUP TOPIC PARTITION CURRENT-OFFSET LOG-END-OFFSET LAG CONSUMER-ID HOST CLIENT-ID
group-a topic-a 0 3 5 2 - - -
`), 2)
  assert.throws(() => healthModule.parseConsumerLag('unexpected output'))
})
