import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

import { evaluateMessagingHealth } from './messaging-health.mjs'

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
