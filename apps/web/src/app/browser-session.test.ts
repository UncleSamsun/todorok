import { expect, it } from 'vitest'
import { createBrowserSessionCoordinator } from './browser-session'
import { SessionClient } from '@todorok/api-client'

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
