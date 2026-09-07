import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { SessionClient } from '@todorok/api-client'
import { App } from '../../App'

afterEach(() => { cleanup(); history.replaceState({}, '', '/') })

it('refreshes a previously visited domain after saving a past record', async () => {
  let saved = false
  const task = { taskId: 'past-study', title: '지난 공부', taskType: 'STUDY', scheduledDate: '2026-09-06', status: 'PLANNED', version: 0 }
  const record = { activityId: 'new-record', taskId: task.taskId, userId: 'owner', activityType: 'STUDY', performedAt: '2026-09-06T00:00:00+09:00', detail: { study: { durationMinutes: 30 } }, note: '과거 공부 완료', status: 'COMPLETED', version: 0, syncState: 'APPLIED' }
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.includes('/activities/summary')) return Response.json({ month: '2026-09', activityType: 'STUDY', completedCount: saved ? 1 : 0, durationSeconds: saved ? 1800 : 0 })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-09-01', to: '2026-09-30', days: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-09-06', tasks: [task] })
    if (path.endsWith('/tasks/past-study')) return Response.json(task)
    if (path.endsWith('/activities') && init?.method === 'POST') { saved = true; return Response.json(record) }
    if (path.endsWith('/activities/new-record')) return Response.json(record)
    if (path.includes('/activities?')) return Response.json({ items: saved ? [record] : [] })
    return Response.json({}, { status: 404 })
  } })
  await act(async () => { render(<App session={session} />) })
  fireEvent.click(await screen.findByRole('link', { name: '공부' }))
  expect(await screen.findByText('0회')).toBeVisible()
  fireEvent.click(screen.getByRole('button', { name: '지난 기록' }))
  fireEvent.change(screen.getByLabelText('지난 수행일'), { target: { value: '2026-09-06' } })
  fireEvent.click(await screen.findByRole('button', { name: '지난 공부 기록' }))
  expect(await screen.findByLabelText('수행일')).toHaveValue('2026-09-06')
  fireEvent.change(screen.getByLabelText('집중 시간(분)'), { target: { value: '30' } })
  fireEvent.change(screen.getByLabelText('기록 메모'), { target: { value: '과거 공부 완료' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await screen.findByText('일정 반영 완료')
  fireEvent.click(screen.getByRole('link', { name: '공부' }))
  expect(await screen.findByText('1회')).toBeVisible()
  expect(screen.getByText('30분')).toBeVisible()
  expect(await screen.findByRole('button', { name: /과거 공부 완료/ })).toBeVisible()
})

it('shares the selected month between domain tabs and keeps partial failures explicit', async () => {
  const requested: string[] = []
  const session = new SessionClient({ fetcher: async (url) => {
    const path = String(url); requested.push(path)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.includes('/activities/summary')) return path.includes('activityType=STUDY')
      ? Response.json({ code: 'DOWN' }, { status: 503 })
      : Response.json({ month: '2026-08', activityType: 'WORKOUT', completedCount: 3, durationSeconds: 5400 })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-08-01', to: '2026-08-31', days: [{ date: '2026-08-03', totalCount: 2, completedCount: 0, categoryProgress: [{ taskType: 'WORKOUT', totalCount: 2, completedCount: 0 }, { taskType: 'STUDY', totalCount: 1, completedCount: 0 }] }] })
    if (path.endsWith('/activities?limit=20')) return Response.json({ items: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-09-07', tasks: [] })
    return Response.json({}, { status: 404 })
  } })
  render(<App session={session} />)
  await waitFor(() => expect(location.pathname).toBe('/today'))
  fireEvent.click(await screen.findByRole('link', { name: '운동' }))
  fireEvent.click(await screen.findByRole('button', { name: '이전 달' }))
  expect(await screen.findByText('3회')).toBeVisible()
  expect(screen.getByText('90분')).toBeVisible()
  expect(screen.getByText('2개')).toBeVisible()
  fireEvent.click(screen.getByRole('link', { name: '공부' }))
  expect(await screen.findByText('2026년 8월')).toBeVisible()
  expect(await screen.findByText('활동 요약을 불러오지 못했습니다.')).toBeVisible()
  expect(screen.getByText('1개')).toBeVisible()
  expect(requested.some((path) => path.includes('month=2026-08') && path.includes('activityType=STUDY'))).toBe(true)
})

it('opens a past date task in the existing record form', async () => {
  const session = new SessionClient({ fetcher: async (url) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.includes('/activities/summary')) return Response.json({ month: '2026-09', activityType: 'CLIMBING', completedCount: 0, durationSeconds: 0 })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-09-01', to: '2026-09-30', days: [] })
    if (path.endsWith('/activities?limit=20')) return Response.json({ items: [] })
    if (path.includes('/calendar/2026-08-30')) return Response.json({ date: '2026-08-30', tasks: [{ taskId: 'past-task', title: '지난 볼더링', taskType: 'CLIMBING', scheduledDate: '2026-08-30', status: 'PLANNED', version: 0 }] })
    if (path.endsWith('/tasks/past-task')) return Response.json({ taskId: 'past-task', title: '지난 볼더링', taskType: 'CLIMBING', scheduledDate: '2026-08-30', status: 'PLANNED', version: 0 })
    return Response.json({}, { status: 404 })
  } })
  render(<App session={session} />)
  await waitFor(() => expect(location.pathname).toBe('/today'))
  fireEvent.click(await screen.findByRole('link', { name: '클라이밍' }))
  fireEvent.click(await screen.findByRole('button', { name: '지난 기록' }))
  fireEvent.change(screen.getByLabelText('지난 수행일'), { target: { value: '2026-08-30' } })
  fireEvent.click(await screen.findByRole('button', { name: '지난 볼더링 기록' }))
  await waitFor(() => expect(location.search).toBe('?taskId=past-task'))
  expect(await screen.findByLabelText('수행일')).toHaveValue('2026-08-30')
})

it('preserves the edit snapshot when the server rejects a stale version', async () => {
  const writes: any[] = []
  const detail = { activityId: 'activity-1', taskId: 'task-1', userId: 'owner', activityType: 'STUDY', performedAt: '2026-09-01T00:00:00+09:00', detail: { study: { subject: '수학', durationMinutes: 20 } }, note: '원본', status: 'COMPLETED', version: 4, syncState: 'APPLIED' }
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.includes('/activities/summary')) return Response.json({ month: '2026-09', activityType: 'STUDY', completedCount: 1, durationSeconds: 1200 })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-09-01', to: '2026-09-30', days: [] })
    if (path.endsWith('/activities?limit=20')) return Response.json({ items: [detail] })
    if (path.endsWith('/activities/activity-1') && init?.method === 'PATCH') { writes.push(JSON.parse(String(init.body))); return Response.json({ code: 'VERSION_CONFLICT' }, { status: 409 }) }
    if (path.endsWith('/activities/activity-1')) return Response.json(detail)
    if (path.includes('/calendar/')) return Response.json({ date: '2026-09-07', tasks: [] })
    return Response.json({}, { status: 404 })
  } })
  render(<App session={session} />)
  await waitFor(() => expect(location.pathname).toBe('/today'))
  await new Promise((resolve) => setTimeout(resolve, 30))
  fireEvent.click(screen.getByRole('link', { name: '공부' }))
  await screen.findByRole('heading', { name: '기록' })
  fireEvent.click(await screen.findByRole('button', { name: /원본/ }))
  fireEvent.change(await screen.findByLabelText('기록 메모'), { target: { value: '내 수정 초안' } })
  fireEvent.click(screen.getByRole('button', { name: '수정 저장' }))
  expect(await screen.findByText(/다른 변경과 충돌/)).toBeVisible()
  expect(screen.getByLabelText('기록 메모')).toHaveValue('내 수정 초안')
  expect(writes[0]).toMatchObject({ expectedVersion: 4, note: '내 수정 초안' })
})
