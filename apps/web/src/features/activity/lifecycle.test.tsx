import { StrictMode } from 'react'
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { QueryClient } from '@tanstack/react-query'
import { SessionClient } from '@todorok/api-client'
import { App } from '../../App'

afterEach(() => { cleanup(); window.history.replaceState({}, '', '/'); vi.restoreAllMocks() })
function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => { resolve = done })
  return { promise, resolve }
}
const task = (taskId = 'task-1', completed = false) => ({ taskId, userId: 'owner', title: taskId, taskType: 'CLIMBING', scheduledDate: '2026-09-07', status: completed ? 'COMPLETED' : 'PLANNED', version: 0, ...(completed ? { completionSummary: '완등 기록' } : {}) })
const activityResponse = (activityId = 'activity-1', syncState = 'APPLIED') => Response.json({ activityId, taskId: 'task-1', userId: 'owner', activityType: 'CLIMBING', performedAt: '2026-09-07T00:00:00+09:00', detail: { climbing: {} }, status: 'COMPLETED', version: 0, syncState })
async function setup(handlers: { post?: (body: unknown) => Promise<Response>; get?: (id: string) => Promise<Response>; completed?: () => boolean; scheduledDate?: () => string } = {}, strict = false) {
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/refresh') || path.endsWith('/login')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/logout')) return new Response(null, { status: 204 })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-09-06', to: '2026-09-12', days: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-09-07', tasks: [task('task-1', handlers.completed?.())] })
    if (path.includes('/notes/')) return Response.json({ date: '2026-09-07', content: '', version: null })
    if (path.includes('/tasks/task-')) return Response.json({ ...task(path.split('/').at(-1)), ...(handlers.scheduledDate ? { scheduledDate: handlers.scheduledDate() } : {}) })
    if (path.endsWith('/activities') && init?.method === 'POST') return handlers.post?.(JSON.parse(String(init.body))) ?? activityResponse()
    if (path.includes('/activities/')) return handlers.get?.(path.split('/').at(-1)!) ?? activityResponse()
    return Response.json({}, { status: 404 })
  } })
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const invalidations = vi.spyOn(queries, 'invalidateQueries')
  const app = <App session={session} queryClient={queries} />
  render(strict ? <StrictMode>{app}</StrictMode> : app)
  await screen.findByRole('button', { name: 'task-1 기록' })
  invalidations.mockClear()
  return { session, queries, invalidations }
}
async function navigate(path: string) {
  await act(async () => { window.history.pushState({}, '', path); window.dispatchEvent(new PopStateEvent('popstate')) })
}
async function settle(pending: ReturnType<typeof deferred<Response>>, value = activityResponse()) {
  await act(async () => { pending.resolve(value); await pending.promise; await new Promise((resolve) => setTimeout(resolve, 30)) })
}

it('submits successfully after StrictMode mounts, cleans up, and replays effects', async () => {
  const post = vi.fn(async () => activityResponse())
  await setup({ post }, true)
  fireEvent.click(screen.getByRole('button', { name: 'task-1 기록' }))
  fireEvent.click(await screen.findByRole('button', { name: '기록 저장' }))
  await waitFor(() => expect(location.search).toContain('activityId=activity-1'))
  expect(post).toHaveBeenCalledTimes(1)
})

it.each([400, 502, 504])('preserves an uncertain HTTP %s snapshot across leaving and reentering', async (status) => {
  const writes: unknown[] = []
  await setup({ post: async (body) => { writes.push(body); return writes.length === 1 ? new Response('proxy timeout', { status }) : activityResponse() } })
  fireEvent.click(screen.getByRole('button', { name: 'task-1 기록' }))
  fireEvent.change(await screen.findByLabelText('기록 메모'), { target: { value: '보존할 메모' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await screen.findByText(/서버 응답을 확인하지 못했습니다/)
  expect(screen.getByLabelText('기록 메모')).toBeDisabled()
  fireEvent.click(screen.getByRole('button', { name: '취소' }))
  fireEvent.click(await screen.findByRole('button', { name: 'task-1 기록' }))
  expect(await screen.findByLabelText('기록 메모')).toHaveValue('보존할 메모')
  expect(screen.getByLabelText('기록 메모')).toBeDisabled()
  fireEvent.click(screen.getByRole('button', { name: '같은 요청 다시 보내기' }))
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[1]).toEqual(writes[0])
})

it.each(['cancel', 'menu', 'back'] as const)('restores an editable unsent draft after %s', async (exit) => {
  const post = vi.fn(async () => activityResponse())
  await setup({ post })
  fireEvent.click(screen.getByRole('button', { name: 'task-1 기록' }))
  fireEvent.change(await screen.findByLabelText('기록 메모'), { target: { value: '작성 중' } })
  if (exit === 'cancel') fireEvent.click(screen.getByRole('button', { name: '취소' }))
  else if (exit === 'menu') fireEvent.click(screen.getByRole('link', { name: '오늘' }))
  else await act(async () => { history.back(); await new Promise((resolve) => setTimeout(resolve, 30)) })
  fireEvent.click(await screen.findByRole('button', { name: 'task-1 기록' }))
  expect(await screen.findByLabelText('기록 메모')).toHaveValue('작성 중')
  expect(screen.getByLabelText('기록 메모')).toBeEnabled()
  expect(post).not.toHaveBeenCalled()
})

it.each([400, 422])('unlocks a confirmed validation %s rejection for a corrected new command', async (status) => {
  const writes: any[] = []
  await setup({ post: async (body) => { writes.push(body); return writes.length === 1 ? Response.json({ code: 'VALIDATION_FAILED', retryable: false }, { status }) : activityResponse() } })
  fireEvent.click(screen.getByRole('button', { name: 'task-1 기록' }))
  fireEvent.click(await screen.findByRole('button', { name: '기록 저장' }))
  await screen.findByText(/입력을 수정해 주세요/)
  expect(screen.getByLabelText('기록 메모')).toBeEnabled()
  fireEvent.change(screen.getByLabelText('기록 메모'), { target: { value: '수정' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[1].commandId).not.toBe(writes[0].commandId)
  expect(writes[1].note).toBe('수정')
})

it.each([true, false])('distinguishes retryable reference lag from a permanent conflict (%s)', async (retryable) => {
  const post = vi.fn(async () => Response.json({ code: retryable ? 'TASK_REFERENCE_PENDING' : 'TASK_DELETED', retryable }, { status: 409 }))
  await setup({ post })
  fireEvent.click(screen.getByRole('button', { name: 'task-1 기록' }))
  fireEvent.click(await screen.findByRole('button', { name: '기록 저장' }))
  await screen.findByText(retryable ? /할 일 정보가 기록 서비스/ : /충돌로 저장이 거절/)
  expect(screen.getByLabelText('기록 메모')).toBeDisabled()
  if (retryable) expect(screen.getByRole('button', { name: '같은 요청 다시 보내기' })).toBeEnabled()
  else {
    expect(screen.getByRole('button', { name: '충돌 상태 확인 필요' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '오늘에서 할 일 확인' })).toBeEnabled()
  }
})

it('rejects partial hours on both sides before sending a POST', async () => {
  const post = vi.fn(async () => activityResponse())
  await setup({ post })
  fireEvent.click(screen.getByRole('button', { name: 'task-1 기록' }))
  fireEvent.click(await screen.findByText('실제 시간 추가'))
  fireEvent.change(screen.getByLabelText('시작 시'), { target: { value: '7' } })
  fireEvent.change(screen.getByLabelText('종료 시'), { target: { value: '8' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await screen.findByText('시간의 시와 분을 모두 선택해 주세요.')
  expect(post).not.toHaveBeenCalled()
})

it('clears drafts on logout and ignores an old success after the next login', async () => {
  const pending = deferred<Response>()
  const { session } = await setup({ post: () => pending.promise })
  fireEvent.click(screen.getByRole('button', { name: 'task-1 기록' }))
  fireEvent.change(await screen.findByLabelText('기록 메모'), { target: { value: '이전 세션 비공개 메모' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  fireEvent.click(screen.getByRole('button', { name: '로그아웃' }))
  await waitFor(() => expect(location.pathname).toBe('/login'))
  await act(async () => { await session.login('owner@example.com', 'password') })
  fireEvent.click(await screen.findByRole('button', { name: 'task-1 기록' }))
  expect(await screen.findByLabelText('기록 메모')).toHaveValue('')
  expect(screen.getByLabelText('기록 메모')).toBeEnabled()
  await settle(pending)
  expect(location.search).toBe('?taskId=task-1')
  fireEvent.click(screen.getByRole('button', { name: '취소' }))
  await waitFor(() => expect(screen.getByRole('button', { name: 'task-1 기록' })).toBeEnabled())
  fireEvent.click(await screen.findByRole('button', { name: 'task-1 기록' }))
  expect(await screen.findByLabelText('기록 메모')).toHaveValue('')
  expect(screen.getByRole('button', { name: '기록 저장' })).toBeEnabled()
})

it('retains the submitted date and command after leaving an in-flight save and task rescheduling', async () => {
  const pending = deferred<Response>(), writes: unknown[] = []
  let scheduledDate = '2026-09-07'
  await setup({ scheduledDate: () => scheduledDate, post: async (body) => { writes.push(body); return writes.length === 1 ? pending.promise : activityResponse() } })
  fireEvent.click(screen.getByRole('button', { name: 'task-1 기록' }))
  fireEvent.click(await screen.findByRole('button', { name: '기록 저장' }))
  await waitFor(() => expect(writes).toHaveLength(1))
  fireEvent.click(screen.getByRole('button', { name: '취소' }))
  scheduledDate = '2026-09-08'
  await waitFor(() => expect(screen.getByRole('button', { name: 'task-1 기록' })).toBeEnabled())
  fireEvent.click(screen.getByRole('button', { name: 'task-1 기록' }))
  expect(await screen.findByLabelText('수행일')).toHaveValue('2026-09-07')
  await settle(pending)
  expect(location.search).toBe('?taskId=task-1')
  expect(screen.getByLabelText('수행일')).toHaveValue('2026-09-07')
  expect(screen.getByLabelText('수행일')).toBeDisabled()
  fireEvent.click(screen.getByRole('button', { name: '같은 요청 다시 보내기' }))
  await waitFor(() => expect(location.search).toBe('?date=2026-09-07&activityId=activity-1'))
  expect(writes).toHaveLength(2)
  expect(writes[1]).toEqual(writes[0])
})

it.each(['cancel', 'task switch', 'logout'] as const)('ignores deferred POST success after %s', async (action) => {
  const pending = deferred<Response>(), post = vi.fn(() => pending.promise)
  const { queries, invalidations } = await setup({ post })
  fireEvent.click(screen.getByRole('button', { name: 'task-1 기록' }))
  fireEvent.click(await screen.findByRole('button', { name: '기록 저장' }))
  await waitFor(() => expect(post).toHaveBeenCalledTimes(1))
  if (action === 'cancel') {
    fireEvent.click(screen.getByRole('button', { name: '취소' }))
    await screen.findByRole('button', { name: 'task-1 기록' })
  } else if (action === 'task switch') {
    await navigate('/climbing?taskId=task-2')
    await screen.findByText('task-2')
    fireEvent.change(screen.getByLabelText('기록 메모'), { target: { value: '새 작업 메모' } })
  } else {
    fireEvent.click(screen.getByRole('button', { name: '로그아웃' }))
    await waitFor(() => expect(location.pathname).toBe('/login'))
  }
  const destination = location.pathname + location.search
  const cached = queries.getQueryData(['calendar', 'day', '2026-09-07'])
  invalidations.mockClear()
  await settle(pending)
  expect(location.pathname + location.search).toBe(destination)
  expect(invalidations).not.toHaveBeenCalled()
  expect(queries.getQueryData(['calendar', 'day', '2026-09-07'])).toBe(cached)
  if (action === 'task switch') {
    expect(screen.getByLabelText('기록 메모')).toHaveValue('새 작업 메모')
    expect(screen.getByRole('button', { name: '기록 저장' })).toBeEnabled()
  }
})

it('automatically polls PENDING to APPLIED and refreshes the existing calendar row once', async () => {
  let reads = 0, completed = false
  const get = vi.fn(async () => { completed = ++reads > 1; return activityResponse('activity-1', completed ? 'APPLIED' : 'PENDING') })
  const { invalidations } = await setup({ get, completed: () => completed })
  await navigate('/today?date=2026-09-07&activityId=activity-1')
  await screen.findByText('기록됨 · 일정 반영 중')
  expect(screen.getByRole('button', { name: 'task-1 기록' })).toHaveAttribute('aria-pressed', 'false')
  await screen.findByText('일정 반영 완료', {}, { timeout: 4000 })
  await screen.findByText('완등 기록')
  expect(screen.getAllByRole('button', { name: 'task-1 기록' })).toHaveLength(1)
  expect(screen.getByRole('button', { name: 'task-1 기록' })).toHaveAttribute('aria-pressed', 'true')
  expect(invalidations).toHaveBeenCalledTimes(1)
  expect(invalidations).toHaveBeenCalledWith({ queryKey: ['calendar'] })
  await act(async () => { await new Promise((resolve) => setTimeout(resolve, 1600)) })
  expect(get).toHaveBeenCalledTimes(2)
  expect(invalidations).toHaveBeenCalledTimes(1)
})

it('invalidates once for manual APPLIED confirmation and independently for the next activity', async () => {
  let reads = 0
  const { invalidations } = await setup({ get: async (id) => activityResponse(id, ++reads === 1 ? 'CONFLICT' : 'APPLIED') })
  await navigate('/today?activityId=activity-1')
  fireEvent.click(await screen.findByRole('button', { name: '상태 다시 확인' }))
  await screen.findByText('일정 반영 완료')
  expect(invalidations).toHaveBeenCalledTimes(1)
  invalidations.mockClear()
  await navigate('/today?activityId=activity-2')
  await screen.findByText('일정 반영 완료')
  await waitFor(() => expect(invalidations).toHaveBeenCalledTimes(1))
})

it.each([
  ['activity switch', 'manual'], ['logout', 'manual'],
  ['activity switch', 'automatic'], ['logout', 'automatic'],
] as const)('ignores a deferred GET after %s (%s)', async (action, mode) => {
  const pending = deferred<Response>()
  let reads = 0
  const { invalidations } = await setup({ get: async (id) => id === 'activity-1' && ++reads > 1 ? pending.promise : activityResponse(id, mode === 'automatic' ? 'PENDING' : 'CONFLICT') })
  await navigate('/today?activityId=activity-1')
  const check = await screen.findByRole('button', { name: '상태 다시 확인' })
  if (mode === 'manual') fireEvent.click(check)
  await waitFor(() => expect(reads).toBe(2), { timeout: 4000 })
  if (action === 'activity switch') await navigate('/today?activityId=activity-2')
  else {
    fireEvent.click(screen.getByRole('button', { name: '로그아웃' }))
    await waitFor(() => expect(location.pathname).toBe('/login'))
  }
  invalidations.mockClear()
  await settle(pending)
  expect(invalidations).not.toHaveBeenCalled()
  expect(screen.queryByText('일정 반영 완료')).not.toBeInTheDocument()
  if (action === 'activity switch') expect(screen.getByRole('button', { name: '상태 다시 확인' })).toBeEnabled()
})
