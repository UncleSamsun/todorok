import { describe, expect, it } from 'vitest'
import { SessionClient } from '@todorok/api-client'
import { requestJson } from '@todorok/api-client'

const data = (token = 'memory-only', userId = 'user-a') => ({ accessToken: token, userId, expiresAt: '2099-01-01T00:00:00Z' })
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } })

describe('session transport', () => {
  it('invokes native fetch without binding its receiver to SessionClient', async () => {
    const fetcher = function(this: unknown) {
      if (this !== undefined && this !== globalThis) throw new TypeError('Illegal invocation')
      return Promise.resolve(json(data()))
    }
    const client = new SessionClient({ fetcher })
    await client.restore()
    expect(client.getSnapshot().status).toBe('authenticated')
  })
  it('refreshes an expired bearer once when generated logout returns 401', async () => {
    let logouts = 0
    let refreshes = 0
    const client = new SessionClient({ fetcher: async url => {
      if (String(url).endsWith('/login')) return json(data('old'))
      if (String(url).endsWith('/refresh')) { refreshes++; return json(data('new')) }
      logouts++
      return logouts === 1 ? json({}, 401) : new Response(null, { status: 204 })
    } })
    await client.login('owner@example.com', 'password')
    await client.logout()
    expect(logouts).toBe(2)
    expect(refreshes).toBe(1)
    expect(client.getSnapshot().status).toBe('anonymous')
  })
  it('accepts an empty 204 even when its content type is JSON', async () => {
    expect(await requestJson('/logout', {}, async () => new Response(null, { status: 204, headers: { 'content-type': 'application/json' } }))).toBeUndefined()
  })
  it('coalesces concurrent 401s and never retries a second 401', async () => {
    let refreshes = 0
    let requests = 0
    const client = new SessionClient({ fetcher: async (url, init) => {
      if (String(url).endsWith('/login')) return json(data('old'))
      if (String(url).endsWith('/refresh')) { refreshes++; return json(data('new')) }
      requests++
      return new Headers(init?.headers).get('Authorization') === 'Bearer new' ? json({ ok: true }) : json({}, 401)
    } })
    await client.login('owner@example.com', 'password')
    const results = await Promise.all([client.fetch('/api/planner/v1/tasks'), client.fetch('/api/planner/v1/tasks')])
    expect(results.map(r => r.status)).toEqual([200, 200])
    expect(refreshes).toBe(1)
    expect(requests).toBe(4)
    let attempts = 0
    const denied = new SessionClient({ fetcher: async url => String(url).endsWith('/refresh') ? json(data()) : (attempts++, json({}, 401)) })
    await denied.restore()
    expect((await denied.fetch('/api/planner/v1/tasks')).status).toBe(401)
    expect(attempts).toBe(2)
    expect(denied.getSnapshot().status).toBe('anonymous')
  })
  it('does not replay a mutation whose response was lost', async () => {
    let posts = 0
    const client = new SessionClient({ fetcher: async url => {
      if (String(url).endsWith('/refresh')) return json(data())
      posts++; throw new TypeError('connection lost')
    } })
    await client.restore()
    await expect(client.fetch('/api/planner/v1/tasks', { method: 'POST', body: '{}' })).rejects.toThrow()
    expect(posts).toBe(1)
  })
  it('replays a 401 mutation with the same body and rejects old-user in-flight responses', async () => {
    const bodies: (BodyInit | null | undefined)[] = []
    let resolve!: (response: Response) => void
    const client = new SessionClient({ fetcher: async (url, init) => {
      if (String(url).endsWith('/login')) return json(data('old', JSON.parse(String(init?.body)).email))
      if (String(url).endsWith('/refresh')) return json(data('new', 'owner@example.com'))
      if (String(url).endsWith('/slow')) return new Promise<Response>(r => { resolve = r })
      bodies.push(init?.body)
      return bodies.length === 1 ? json({}, 401) : json({ created: true })
    } })
    await client.login('owner@example.com', 'password')
    expect((await client.fetch('/api/planner/v1/tasks', { method: 'POST', body: '{"title":"task"}' })).status).toBe(200)
    expect(bodies).toEqual(['{"title":"task"}', '{"title":"task"}'])
    const pending = client.fetch('/api/planner/v1/slow')
    await client.login('other@example.com', 'password')
    resolve(json({ private: 'old user' }))
    await expect(pending).rejects.toThrow('인증이 필요합니다.')
  })
  it.each([401, 'network'])('clears failed restoration (%s)', async failure => {
    const client = new SessionClient({ fetcher: async () => { if (failure === 'network') throw new TypeError(); return json({}, 401) } })
    await client.restore()
    expect(client.getSnapshot().status).toBe('anonymous')
  })
  it('uses generated logout with cookie credentials and bearer, clears only after 204', async () => {
    let logoutInit: RequestInit | undefined
    const client = new SessionClient({ fetcher: async (url, init) => {
      if (String(url).endsWith('/refresh')) return json(data())
      logoutInit = init
      expect(client.getSnapshot().status).toBe('authenticated')
      return new Response(null, { status: 204, headers: { 'content-type': 'application/json' } })
    } })
    await client.restore()
    await client.logout()
    expect(logoutInit?.credentials).toBe('include')
    expect(new Headers(logoutInit?.headers).get('Authorization')).toBe('Bearer memory-only')
    expect(client.getSnapshot().status).toBe('anonymous')
    expect(JSON.stringify(client.getSnapshot())).not.toContain('memory-only')
  })
})
