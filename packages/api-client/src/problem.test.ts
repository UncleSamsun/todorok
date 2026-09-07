import assert from 'node:assert/strict'
import test from 'node:test'
import { classifyApiError } from './problem.ts'
import { FetchApiTransport } from './transport.ts'

const problem = {
  type: 'about:blank', title: 'Validation failed', status: 400,
  code: 'VALIDATION_FAILED', traceId: 'trace-1', retryable: false,
  fieldErrors: [{ field: 'name', code: 'NOT_BLANK', message: 'must not be blank' }],
}

test('classifies a valid Problem Details response', () => {
  assert.deepEqual(classifyApiError(problem), { kind: 'problem', problem })
})

test('classifies network and non-JSON failures separately', () => {
  assert.deepEqual(classifyApiError(new TypeError('fetch failed')), { kind: 'network', cause: new TypeError('fetch failed') })
  assert.deepEqual(classifyApiError('<html>bad gateway</html>'), { kind: 'invalid-response', payload: '<html>bad gateway</html>' })
})

test('rejects objects that violate the exact Problem Details schema', () => {
  const invalidProblems: unknown[] = [
    { ...problem, unexpected: true },
    { ...problem, code: 'lowercase' },
    { ...problem, traceId: '' },
    { ...problem, status: 400.5 },
    { ...problem, type: 'not a uri reference' },
    { ...problem, instance: '%' },
    { ...problem, fieldErrors: [{ field: '', code: 'NOT_BLANK', message: 'required' }] },
    { ...problem, fieldErrors: [{ field: 'name', code: 'bad-code', message: 'required' }] },
    { ...problem, fieldErrors: [{ field: 'name', code: 'REQUIRED', message: '', extra: true }] },
  ]

  for (const payload of invalidProblems) {
    assert.equal(classifyApiError(payload).kind, 'invalid-response')
  }
})

test('accepts empty title and relative URI references allowed by the schema', () => {
  const valid = { ...problem, type: '', title: '', instance: '../tasks/1' }
  assert.equal(classifyApiError(valid).kind, 'problem')
})

test('does not automatically retry a failed mutation', async () => {
  let attempts = 0
  const fetcher: typeof fetch = async () => {
    attempts += 1
    throw new TypeError('offline')
  }
  const transport = new FetchApiTransport('https://api.example.test', fetcher)
  await assert.rejects(transport.request({ path: '/tasks', method: 'POST' }), (error: unknown) => {
    assert.equal(classifyApiError(error).kind, 'network')
    return true
  })
  assert.equal(attempts, 1)
})
