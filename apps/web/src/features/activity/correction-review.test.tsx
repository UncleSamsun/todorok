import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { QueryClient } from '@tanstack/react-query'
import { activity, SessionClient } from '@todorok/api-client'
import { App } from '../../App'

afterEach(() => { cleanup(); history.replaceState({}, '', '/'); vi.restoreAllMocks() })
const initial = () => ({ activityId: 'edit-1', taskId: 'task-1', userId: 'owner', activityType: 'STUDY', performedAt: '2026-09-06T01:00:00.123456Z', startedAt: '2026-09-06T01:00:59.123456Z', endedAt: '2026-09-06T01:01:01.456789Z', detail: { study: { subject: '원본' } }, note: '원본 메모', status: 'COMPLETED', version: 4, syncState: 'APPLIED' })
async function open(options: { patch?: (body: any) => Promise<Response>; cancel?: () => Promise<Response>; read?: (value: ReturnType<typeof initial>) => Promise<Response>; initial?: Partial<ReturnType<typeof initial>> } = {}) {
  let remote = { ...initial(), ...options.initial }
  const writes: any[] = []
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/logout')) return new Response(null, { status: 204 })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-09-01', to: '2026-09-30', days: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-09-07', tasks: [] })
    if (path.endsWith('/activities/edit-1') && init?.method === 'PATCH') { const body = JSON.parse(String(init.body)); writes.push(body); return options.patch?.(body) ?? Response.json({ ...remote, ...body, version: 5 }) }
    if (path.endsWith('/activities/edit-1/void')) return options.cancel?.() ?? Response.json({ ...remote, version: 5, status: 'VOIDED' })
    if (path.endsWith('/activities/edit-1')) return options.read?.(remote) ?? Response.json(remote)
    if (path.includes('/activities?')) return Response.json({ items: [remote] })
    if (path.includes('/activities/summary')) return Response.json({ month: '2026-09', activityType: 'STUDY', completedCount: 1, durationSeconds: 2 })
    return Response.json({}, { status: 404 })
  } })
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: 30_000 } } })
  await act(async () => { render(<App session={session} queryClient={queries} />) })
  await act(async () => { history.pushState({}, '', '/study?activityId=edit-1'); dispatchEvent(new PopStateEvent('popstate')) })
  await screen.findByLabelText('기록 메모')
  return { queries, writes, setRemote: (value: Partial<ReturnType<typeof initial>>) => { remote = { ...remote, ...value } } }
}
const submit = () => fireEvent.click(screen.getByRole('button', { name: '수정 저장' }))

it('keeps v4 edit baseline when a background v5 arrives', async () => {
  const { queries, writes } = await open({ patch: async () => Response.json({ code: 'VERSION_CONFLICT' }, { status: 409 }) })
  fireEvent.change(screen.getByLabelText('기록 메모'), { target: { value: 'v4 초안' } })
  await act(async () => { queries.setQueryData(['activity', 'owner', 'edit-1'], activity.ActivityResponseFromJSON({ ...initial(), version: 5, note: '다른 변경' })); await new Promise((resolve) => setTimeout(resolve, 0)) })
  submit()
  await screen.findByText(/충돌/)
  expect(writes[0].expectedVersion).toBe(4)
  expect(screen.getByLabelText('기록 메모')).toHaveValue('v4 초안')
})

it('preserves exact seconds and fractional timestamps for a note-only correction', async () => {
  const { writes } = await open()
  fireEvent.change(screen.getByLabelText('기록 메모'), { target: { value: '메모만 수정' } })
  submit()
  await waitFor(() => expect(writes).toHaveLength(1))
  expect(writes[0]).toMatchObject({ performedAt: initial().performedAt, startedAt: initial().startedAt, endedAt: initial().endedAt })
})

it('builds corrected input after a confirmed validation rejection', async () => {
  const { writes } = await open({ patch: async () => Response.json({ code: 'INVALID_INTERVAL', retryable: false }, { status: 400 }) })
  submit()
  await screen.findByText(/입력/)
  fireEvent.change(screen.getByLabelText('기록 메모'), { target: { value: '수정한 입력' } })
  submit()
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[1].note).toBe('수정한 입력')
  expect(writes[1].expectedVersion).toBe(4)
})

it.each(['time', 'status', 'revision'])('does not accept another writer’s %s as the result of an uncertain PATCH', async (difference) => {
  const { setRemote } = await open({ patch: async () => new Response(null, { status: 503 }) })
  setRemote({ version: difference === 'revision' ? 6 : 5, ...(difference === 'time' ? { endedAt: '2026-09-06T01:05:00Z' } : {}), ...(difference === 'status' ? { status: 'VOIDED' } : {}) })
  submit()
  await screen.findByText(/확인하지 못|초안과 달라/)
  expect(screen.getByLabelText('기록 메모')).toHaveValue('원본 메모')
  expect(screen.getByLabelText('기록 메모')).toBeDisabled()
})

it('rejects a partial explicit time edit without clearing the original interval', async () => {
  const { writes } = await open()
  fireEvent.change(screen.getByLabelText('시작 시'), { target: { value: '' } })
  submit()
  await screen.findByText(/시와 분/)
  expect(writes).toHaveLength(0)
})

it.each(['PATCH', 'void'])('observes %s PENDING through APPLIED and invalidates calendar once for that revision', async (operation) => {
  const pending = { ...initial(), version: 5, syncState: 'PENDING', ...(operation === 'void' ? { status: 'VOIDED' } : {}) }
  const { queries, setRemote } = await open({ patch: async () => Response.json(pending), cancel: async () => Response.json(pending) })
  const invalidations = vi.spyOn(queries, 'invalidateQueries')
  if (operation === 'void') fireEvent.click(screen.getByRole('button', { name: '기록 취소' })); else submit()
  await screen.findByText('기록됨 · 일정 반영 중')
  setRemote({ ...pending, syncState: 'APPLIED' })
  await screen.findByText('일정 반영 완료', {}, { timeout: 4000 })
  const calendarPasses = () => invalidations.mock.calls.filter(([filters]) => filters?.predicate?.({ queryKey: ['calendar', 'range', '2026-08-01', '2026-09-30'] } as any))
  expect(calendarPasses()).toHaveLength(1)
  await act(async () => { await queries.refetchQueries({ queryKey: ['activity', 'owner', 'edit-1'] }) })
  expect(calendarPasses()).toHaveLength(1)
})

it.each(['PATCH', 'void'])('shows %s CONFLICT with manual status confirmation', async (operation) => {
  const pending = { ...initial(), version: 5, syncState: 'PENDING', ...(operation === 'void' ? { status: 'VOIDED' } : {}) }
  const { setRemote } = await open({ patch: async () => Response.json(pending), cancel: async () => Response.json(pending) })
  if (operation === 'void') fireEvent.click(screen.getByRole('button', { name: '기록 취소' })); else submit()
  await screen.findByText('기록됨 · 일정 반영 중')
  setRemote({ ...pending, syncState: 'CONFLICT' })
  await screen.findByText('기록됨 · 일정과 충돌', {}, { timeout: 4000 })
  expect(screen.getByRole('button', { name: '상태 다시 확인' })).toBeEnabled()
})

it('applies an explicitly edited end minute while preserving the untouched start precision', async () => {
  const { writes } = await open()
  fireEvent.change(screen.getByLabelText('종료 분'), { target: { value: '05' } })
  submit()
  await waitFor(() => expect(writes).toHaveLength(1))
  expect(writes[0].endedAt).toBe('2026-09-06T01:05:00.000Z')
  expect(writes[0].startedAt).toBe(initial().startedAt)
})

it('preserves a positive sub-millisecond interval during a note edit', async () => {
  const { writes } = await open({ initial: { startedAt: '2026-09-06T01:00:00.123456Z', endedAt: '2026-09-06T01:00:00.123457Z' } })
  submit()
  await waitFor(() => expect(writes).toHaveLength(1))
  expect(writes[0].startedAt).toBe('2026-09-06T01:00:00.123456Z')
  expect(writes[0].endedAt).toBe('2026-09-06T01:00:00.123457Z')
})

it('retains an uncertain edit across navigation and accepts only the exact next revision', async () => {
  const { setRemote } = await open({ patch: async () => { throw new TypeError('network') } })
  fireEvent.change(screen.getByLabelText('기록 메모'), { target: { value: '보존할 수정' } })
  submit()
  await screen.findByText(/초안과 달라/)
  fireEvent.click(screen.getByRole('link', { name: '오늘' }))
  await waitFor(() => expect(location.pathname).toBe('/today'))
  await act(async () => { history.pushState({}, '', '/study?activityId=edit-1'); dispatchEvent(new PopStateEvent('popstate')) })
  expect(await screen.findByLabelText('기록 메모')).toHaveValue('보존할 수정')
  expect(screen.getByLabelText('기록 메모')).toBeDisabled()
  setRemote({ version: 5, note: '보존할 수정' })
  fireEvent.click(screen.getByRole('button', { name: '결과 다시 확인' }))
  await waitFor(() => expect(screen.getByLabelText('기록 메모')).toBeEnabled())
  expect(screen.getByLabelText('기록 메모')).toHaveValue('보존할 수정')
  expect(screen.queryByRole('button', { name: '같은 요청 다시 보내기' })).not.toBeInTheDocument()
})

it.each(['menu', 'logout'])('ignores a delayed automatic correction status GET after %s', async (action) => {
  let resolve!: (response: Response) => void, reads = 0
  const pending = new Promise<Response>((done) => { resolve = done })
  const { queries } = await open({ patch: async () => Response.json({ ...initial(), version: 5, syncState: 'PENDING' }), read: async (value) => ++reads === 1 ? Response.json(value) : pending })
  submit()
  await screen.findByText('기록됨 · 일정 반영 중')
  await waitFor(() => expect(reads).toBe(2), { timeout: 4000 })
  if (action === 'logout') fireEvent.click(screen.getByRole('button', { name: '로그아웃' }))
  else fireEvent.click(screen.getByRole('link', { name: '오늘' }))
  await waitFor(() => expect(location.pathname).toBe(action === 'logout' ? '/login' : '/today'))
  const invalidations = vi.spyOn(queries, 'invalidateQueries')
  await act(async () => { resolve(Response.json({ ...initial(), version: 5 })); await pending })
  expect(invalidations).not.toHaveBeenCalled()
  expect(screen.queryByText('일정 반영 완료')).not.toBeInTheDocument()
})

it('does not let a pre-correction GET replace the saved pending revision', async () => {
  let resolve!: (response: Response) => void, hold = false
  const late = new Promise<Response>((done) => { resolve = done })
  const pending = { ...initial(), version: 5, syncState: 'PENDING' }
  const { queries, setRemote } = await open({ patch: async () => Response.json(pending), read: async (value) => { if (hold) { hold = false; return late }; return Response.json(value) } })
  hold = true
  let refreshing!: Promise<void>
  await act(async () => { refreshing = queries.refetchQueries({ queryKey: ['activity', 'owner', 'edit-1'] }); await Promise.resolve() })
  submit()
  await screen.findByText('기록됨 · 일정 반영 중')
  setRemote({ ...pending, syncState: 'APPLIED' })
  await act(async () => { resolve(Response.json(initial())); await refreshing })
  expect(queries.getQueryData<activity.ActivityResponse>(['activity', 'owner', 'edit-1'])?.version).toBe(5)
  expect(screen.getByText('기록됨 · 일정 반영 중')).toBeVisible()
  await screen.findByText('일정 반영 완료', {}, { timeout: 4000 })
})

it.each(['menu', 'logout'])('ignores an uncertain PATCH response after %s', async (action) => {
  let resolve!: (value: Response) => void
  const deferred = new Promise<Response>((done) => { resolve = done })
  const { queries } = await open({ patch: async () => deferred })
  submit()
  if (action === 'logout') fireEvent.click(screen.getByRole('button', { name: '로그아웃' }))
  else fireEvent.click(screen.getByRole('link', { name: '오늘' }))
  await waitFor(() => expect(location.pathname).toBe(action === 'logout' ? '/login' : '/today'))
  const invalidations = vi.spyOn(queries, 'invalidateQueries')
  await act(async () => { resolve(Response.json({ ...initial(), version: 5 })); await deferred })
  expect(invalidations).not.toHaveBeenCalled()
})
