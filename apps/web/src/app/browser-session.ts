import type { MemorySession, SessionCoordinator } from '@todorok/api-client'

interface Channel { onmessage: ((event: MessageEvent) => void) | null; postMessage(data: unknown): void }
interface Environment {
  locks?: { request<T>(name: string, callback: () => Promise<T>): Promise<T> }
  createChannel?: () => Channel
}
interface RecordValue { stamp: number; session: MemorySession | null }
function validRecord(value: unknown): value is RecordValue {
  if (!value || typeof value !== 'object') return false
  const record = value as RecordValue
  return Number.isFinite(record.stamp) && (record.session === null || (typeof record.session?.accessToken === 'string'
    && typeof record.session.userId === 'string' && Number.isFinite(record.session.expiresAt)))
}
export function createBrowserSessionCoordinator(environment: Environment = {
  locks: navigator.locks,
  createChannel: typeof BroadcastChannel === 'undefined' ? undefined : () => new BroadcastChannel('todorok-session'),
}): SessionCoordinator {
  const channel = environment.createChannel?.()
  let current: RecordValue | undefined
  let tail = Promise.resolve()
  const listeners = new Set<(session: MemorySession | null) => void>()
  if (channel) channel.onmessage = event => {
    const message = event.data as { type?: string; value?: unknown } | null
    if (message?.type === 'read') { if (current) channel.postMessage({ type: 'state', value: current }); return }
    if (message?.type === 'state' && validRecord(message.value) && (!current || message.value.stamp > current.stamp)) {
      current = message.value
      listeners.forEach(listener => listener(current!.session))
    }
  }
  return {
    async exclusive<T>(operation: () => Promise<T>) {
      const previous = tail
      let release!: () => void
      tail = new Promise<void>(resolve => { release = resolve })
      await previous
      try { return environment.locks ? await environment.locks.request('todorok-session', operation) : await operation() }
      finally { release() }
    },
    async latest() {
      // Run inside the lock. A tab opened after the last broadcast asks live peers.
      if (channel) { channel.postMessage({ type: 'read' }); await new Promise(resolve => setTimeout(resolve, 60)) }
      return current?.session
    },
    publish(session) {
      current = { stamp: Math.max(Date.now(), (current?.stamp ?? 0) + 1), session }
      channel?.postMessage({ type: 'state', value: current })
    },
    subscribe(listener) { listeners.add(listener); return () => { listeners.delete(listener) } },
  }
}
