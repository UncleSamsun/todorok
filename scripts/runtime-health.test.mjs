import assert from 'node:assert/strict'
import test from 'node:test'
import { fetchWithRetry } from './runtime-health.mjs'

test('일시적인 네트워크 실패 뒤 요청을 다시 시도한다', async () => {
  let attempts = 0
  const fetcher = async () => {
    attempts += 1
    if (attempts === 1) {
      throw new Error('temporary network failure')
    }
    return new Response('ok', { status: 200 })
  }

  const response = await fetchWithRetry('http://127.0.0.1/health', {
    attempts: 3,
    delayMs: 0,
    fetcher,
  })

  assert.equal(response.status, 200)
  assert.equal(attempts, 2)
})
