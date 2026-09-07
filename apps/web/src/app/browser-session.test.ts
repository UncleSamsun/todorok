import { expect, it } from 'vitest'
import { createBrowserSessionCoordinator } from './browser-session'
import { SessionClient } from '@todorok/api-client'

function channelFactory() {
  const peers = new Set<{ onmessage: ((event: MessageEvent) => void) | null; postMessage(data: unknown): void }>()
  return () => {
    const channel = { onmessage: null as ((event: MessageEvent) => void) | null,
      postMessage(data: unknown) { for (const peer of peers) if (peer !== channel) queueMicrotask(() => peer.onmessage?.(new MessageEvent('message', { data }))) } }
    peers.add(channel)
    return channel
  }
}
function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>(r => { resolve = r })
  return { promise, resolve }
}
const sessionResponse = (token: string, userId = 'owner') => new Response(JSON.stringify({ accessToken: token, userId, expiresAt: '2099-01-01T00:00:00Z' }), { headers: { 'content-type': 'application/json' } })

it.each(['late-success', 'late-401'])('does not let %s refresh overwrite a remote logout or new login without Web Locks', async outcome => {
  const createChannel = channelFactory()
  const refreshStarted = deferred<void>()
  const refreshResponse = deferred<Response>()
  const a = new SessionClient({ coordinator: createBrowserSessionCoordinator({ createChannel }), fetcher: async url => {
    if (String(url).endsWith('/login')) return sessionResponse('old')
    if (String(url).endsWith('/refresh')) { refreshStarted.resolve(); return refreshResponse.promise }
    return new Response('{}', { status: 401 })
  } })
  const b = new SessionClient({ coordinator: createBrowserSessionCoordinator({ createChannel }), fetcher: async url =>
    String(url).endsWith('/logout') ? new Response(null, { status: 204 }) : sessionResponse('new-login', 'other-user') })
  const stopA = a.connect(), stopB = b.connect()
  try {
    await a.login('owner@example.test', 'password')
    const pending = a.fetch('/api/planner/v1/tasks').then(() => 'resolved', () => 'rejected')
    await refreshStarted.promise
    await b.logout()
    if (outcome === 'late-401') await b.login('other@example.test', 'password')
    refreshResponse.resolve(outcome === 'late-success' ? sessionResponse('late-refresh') : new Response('{}', { status: 401 }))
    expect(await pending).toBe('rejected')
    // Allow BroadcastChannel messages from any incorrect stale write to reach the other tab.
    await Promise.resolve()
    const expected = outcome === 'late-success' ? 'anonymous' : 'authenticated'
    expect(a.getSnapshot().status).toBe(expected)
    expect(b.getSnapshot().status).toBe(expected)
    if (outcome === 'late-401') {
      expect(a.getSnapshot().userId).toBe('other-user')
      expect(b.getSnapshot().userId).toBe('other-user')
    }
  } finally { stopA(); stopB() }
})

it('checks peer memory after acquiring a browser lock and coalesces two tab restores', async () => {
  let tail = Promise.resolve()
  const locks = { request: async <T,>(_name: string, callback: () => Promise<T>) => {
    const previous = tail
    let release!: () => void
    tail = new Promise<void>(r => { release = r })
    await previous
    try { return await callback() } finally { release() }
  } }
  const channels = new Set<{ onmessage: ((event: MessageEvent) => void) | null; postMessage(data: unknown): void }>()
  const createChannel = () => {
    const channel = { onmessage: null as ((event: MessageEvent) => void) | null,
      postMessage(data: unknown) { for (const peer of channels) if (peer !== channel) queueMicrotask(() => peer.onmessage?.(new MessageEvent('message', { data }))) } }
    channels.add(channel)
    return channel
  }
  let refreshes = 0
  const fetcher: typeof fetch = async () => { refreshes++; return new Response(JSON.stringify({ accessToken: 'shared-in-memory', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' }), { headers: { 'content-type': 'application/json' } }) }
  const a = new SessionClient({ fetcher, coordinator: createBrowserSessionCoordinator({ locks, createChannel }) })
  const b = new SessionClient({ fetcher, coordinator: createBrowserSessionCoordinator({ locks, createChannel }) })
  const stopA = a.connect(), stopB = b.connect()
  await Promise.all([a.restore(), b.restore()])
  expect(refreshes).toBe(1)
  expect(a.getSnapshot().status).toBe('authenticated')
  expect(b.getSnapshot().status).toBe('authenticated')
  stopA(); stopB()
})

it('falls back to same-tab single-flight without browser locks or channels', async () => {
  let calls = 0
  const client = new SessionClient({ coordinator: createBrowserSessionCoordinator({}), fetcher: async () => {
    calls++; return new Response('{}', { status: 401 })
  } })
  await Promise.all([client.restore(), client.restore()])
  expect(calls).toBe(1)
  expect(client.getSnapshot().status).toBe('anonymous')
})
