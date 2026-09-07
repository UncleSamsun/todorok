import { AuthApi } from './generated/planner/src/apis/AuthApi'
import { Configuration, ResponseError } from './generated/planner/src/runtime'

export interface MemorySession { accessToken: string; userId: string; expiresAt: number }
export interface SessionSnapshot {
  status: 'restoring' | 'anonymous' | 'authenticated'
  userId: string | null
  generation: number
  error?: 'network' | 'expired'
}
// Browser facilities are injected; this package never reads navigator or storage.
export interface SessionCoordinator {
  exclusive<T>(operation: () => Promise<T>): Promise<T>
  latest(): Promise<MemorySession | null | undefined>
  publish(session: MemorySession | null): void
  subscribe(listener: (session: MemorySession | null) => void): () => void
}
export class SessionError extends Error {
  readonly status: number
  constructor(status = 0) { super(status === 401 ? '인증이 필요합니다.' : '요청을 완료하지 못했습니다.'); this.name = 'SessionError'; this.status = status }
}

export class SessionClient {
  private memory: MemorySession | null = null
  private snapshot: SessionSnapshot = { status: 'restoring', userId: null, generation: 0 }
  private listeners = new Set<() => void>()
  private refreshing: Promise<void> | null = null
  private queue = Promise.resolve()
  private readonly fetcher: typeof fetch
  private readonly coordinator?: SessionCoordinator
  private readonly auth: AuthApi
  private readonly basePath: string

  constructor(options: { fetcher?: typeof fetch; coordinator?: SessionCoordinator; basePath?: string } = {}) {
    this.fetcher = options.fetcher ?? fetch
    this.coordinator = options.coordinator
    this.basePath = options.basePath ?? '/api/planner/v1'
    this.auth = new AuthApi(new Configuration({ basePath: this.basePath, credentials: 'include', fetchApi: this.rawFetch }))
  }
  getSnapshot = () => this.snapshot
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener) } }
  connect = () => this.coordinator?.subscribe(session => this.accept(session, false)) ?? (() => {})
  private accept(session: MemorySession | null, broadcast = true, error?: 'network' | 'expired') {
    const changed = this.snapshot.userId !== (session?.userId ?? null) || this.snapshot.status === 'restoring'
    this.memory = session
    this.snapshot = { status: session ? 'authenticated' : 'anonymous', userId: session?.userId ?? null,
      generation: this.snapshot.generation + (changed ? 1 : 0), error }
    this.listeners.forEach(listener => listener())
    if (broadcast) this.coordinator?.publish(session)
  }
  private rawFetch: typeof fetch = async (input, init) => {
    // Native browser fetch rejects a SessionClient receiver (Illegal invocation).
    const fetcher = this.fetcher
    try { return await fetcher(input, { ...init, credentials: 'include', cache: 'no-store' }) }
    catch { throw new SessionError() }
  }
  private async exclusive<T>(fn: () => Promise<T>) {
    if (this.coordinator) return this.coordinator.exclusive(fn)
    const previous = this.queue
    let release!: () => void
    this.queue = new Promise<void>(resolve => { release = resolve })
    await previous
    try { return await fn() } finally { release() }
  }
  private async issueRefresh() {
    const response = await this.auth.refreshSession()
    const expiresAt = response.expiresAt.getTime()
    if (!response.accessToken || !response.userId || !Number.isFinite(expiresAt)) throw new SessionError()
    this.accept({ accessToken: response.accessToken, userId: response.userId, expiresAt })
  }
  private refresh = (failedToken: string | null) => {
    if (this.refreshing) return this.refreshing
    const generation = this.snapshot.generation
    this.refreshing = this.exclusive(async () => {
      if (generation !== this.snapshot.generation) return
      const latest = await this.coordinator?.latest()
      if (latest !== undefined) this.accept(latest, false)
      if (latest === null) throw new SessionError(401)
      if (this.memory && this.memory.accessToken !== failedToken && this.memory.expiresAt > Date.now() + 30_000) return
      await this.issueRefresh()
    }).catch(error => {
      const status = error instanceof ResponseError ? error.response.status : error instanceof SessionError ? error.status : 0
      this.accept(null, true, status === 401 ? 'expired' : 'network')
      throw new SessionError(status)
    }).finally(() => { this.refreshing = null })
    return this.refreshing
  }
  restore = async () => { try { await this.refresh(null) } catch { /* snapshot is safe for rendering */ } }
  login = async (email: string, password: string) => {
    try {
      await this.exclusive(async () => {
        const result = await this.auth.login({ loginRequest: { email, password } })
        this.accept({ accessToken: result.accessToken, userId: result.userId, expiresAt: result.expiresAt.getTime() })
      })
    } catch (error) { throw new SessionError(error instanceof ResponseError ? error.response.status : 0) }
  }
  logout = async () => {
    // A 401 can rotate once before taking the logout lock. Never nest that lock.
    if (!this.memory || this.memory.expiresAt <= Date.now() + 30_000) await this.refresh(this.memory?.accessToken ?? null)
    await this.exclusive(async () => {
      const latest = await this.coordinator?.latest()
      if (latest !== undefined) this.accept(latest, false)
      if (!this.memory) { this.accept(null); return }
      const api = new AuthApi(new Configuration({ basePath: this.basePath, credentials: 'include', fetchApi: this.rawFetch,
        accessToken: async () => this.memory?.accessToken ?? '' }))
      try {
        try { await api.logout() }
        catch (error) {
          if (!(error instanceof ResponseError) || error.response.status !== 401) throw error
          try { await this.issueRefresh() }
          catch { this.accept(null, true, 'expired'); throw new SessionError(401) }
          await api.logout()
        }
        this.accept(null)
      } catch (error) { throw new SessionError(error instanceof ResponseError ? error.response.status : error instanceof SessionError ? error.status : 0) }
    })
  }
  fetch: typeof fetch = async (input, init) => {
    const generation = this.snapshot.generation
    const token = this.memory?.accessToken ?? null
    const send = () => {
      const headers = new Headers(init?.headers)
      if (this.memory) headers.set('Authorization', `Bearer ${this.memory.accessToken}`)
      else headers.delete('Authorization')
      return this.rawFetch(input, { ...init, headers })
    }
    let response = await send()
    if (generation !== this.snapshot.generation) throw new SessionError(401)
    if (response.status === 401) {
      await this.refresh(token)
      if (generation !== this.snapshot.generation || !this.memory) throw new SessionError(401)
      response = await send()
      if (generation !== this.snapshot.generation) throw new SessionError(401)
      if (response.status === 401) this.accept(null, true, 'expired')
    }
    return response
  }
}
