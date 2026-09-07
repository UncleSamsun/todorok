import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { SessionClient } from '@todorok/api-client'
import { App } from '../../App'
afterEach(() => { cleanup(); history.replaceState({}, '', '/') })

it.each(['today', 'past'])('opens the exact completed activity beyond the first page from %s without creating', async (entry) => {
  let posts = 0, secondPage = false
  const task = { taskId: 'done-task', title: '완료한 공부', taskType: 'STUDY', scheduledDate: '2026-09-07', status: 'COMPLETED', version: 2 }
  const record = { activityId: 'correct-record', taskId: 'done-task', userId: 'owner', activityType: 'STUDY', performedAt: '2026-08-30T00:00:00Z', detail: { study: {} }, status: 'COMPLETED', version: 4, syncState: 'APPLIED', note: '실제 기존 기록' }
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-09-01', to: '2026-09-30', days: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-09-07', tasks: [task] })
    if (path.endsWith('/tasks/done-task')) return Response.json(task)
    if (path.includes('/activities/summary')) return Response.json({ month: '2026-09', activityType: 'STUDY', completedCount: 0, durationSeconds: 0 })
    if (path.endsWith('/activities') && init?.method === 'POST') { posts++; return Response.json(record) }
    if (path.includes('/activities?')) {
      if (path.includes('cursor=page-2')) { secondPage = true; return Response.json({ items: [record] }) }
      return Response.json({ items: [{ ...record, activityId: 'voided-old', status: 'VOIDED' }, { ...record, activityId: 'other-task', taskId: 'unrelated' }], nextCursor: 'page-2' })
    }
    if (path.endsWith('/activities/correct-record')) return Response.json(record)
    return Response.json({}, { status: 404 })
  } })
  await act(async () => { render(<App session={session} />) })
  if (entry === 'past') {
    fireEvent.click(await screen.findByRole('link', { name: '공부' }))
    fireEvent.click(await screen.findByRole('button', { name: '지난 기록' }))
  }
  fireEvent.click(await screen.findByRole('button', { name: '완료한 공부 기록' }))
  await screen.findByRole('heading', { name: '기록 수정' })
  expect(screen.getByLabelText('기록 메모')).toHaveValue('실제 기존 기록')
  expect(location.search).toBe('?activityId=correct-record')
  expect(secondPage).toBe(true)
  expect(posts).toBe(0)
})

it('refreshes visited registration totals after planner creation and deletion', async () => {
  let exists = false
  const task = { taskId: 'new-task', title: '등록 확인', userId: 'owner', taskType: 'STUDY', scheduledDate: '2026-09-07', status: 'PLANNED', version: 0 }
  const category = { templateId: '10000000-0000-0000-0000-000000000001', domain: 'STUDY', kind: 'STUDY_CATEGORY', archived: false, revision: 0, currentVersion: { templateId: '10000000-0000-0000-0000-000000000001', templateVersion: 1, name: '기본 공부', fields: [] } }
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-09-01', to: '2026-09-30', days: [{ date: '2026-09-07', totalCount: exists ? 1 : 0, completedCount: 0, categoryProgress: [{ taskType: 'STUDY', totalCount: exists ? 1 : 0, completedCount: 0 }] }] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-09-07', tasks: exists ? [task] : [] })
    if (path.endsWith('/tasks') && init?.method === 'POST') { exists = true; return Response.json(task) }
    if (path.includes('/tasks/new-task')) { if (init?.method === 'DELETE') exists = false; return Response.json(task) }
    if (path.includes('/activities/summary')) return Response.json({ month: '2026-09', activityType: 'STUDY', completedCount: 0, durationSeconds: 0 })
    if (path.includes('/activities?')) return Response.json({ items: [] })
    if (path.includes('/templates?')) return Response.json({ items: [category] })
    return Response.json({}, { status: 404 })
  } })
  await act(async () => { render(<App session={session} />) })
  fireEvent.click(await screen.findByRole('link', { name: '공부' }))
  expect(await screen.findByText('0개')).toBeVisible()
  fireEvent.click(screen.getByRole('link', { name: '오늘' }))
  fireEvent.click(await screen.findByRole('button', { name: '공부 추가' }))
  await screen.findByRole('option', { name: '기본 공부' })
  fireEvent.change(screen.getByLabelText('공부 카테고리'), { target: { value: category.templateId } })
  fireEvent.change(screen.getByLabelText('제목'), { target: { value: '등록 확인' } })
  fireEvent.click(screen.getByRole('button', { name: '저장' }))
  await screen.findByRole('button', { name: '등록 확인 기록' })
  fireEvent.click(screen.getByRole('link', { name: '공부' }))
  expect(await screen.findByText('1개')).toBeVisible()
  fireEvent.click(screen.getByRole('link', { name: '오늘' }))
  fireEvent.click(await screen.findByRole('button', { name: '등록 확인 수정' }))
  fireEvent.click(await screen.findByRole('button', { name: '삭제' }))
  await waitFor(() => expect(screen.queryByRole('button', { name: '등록 확인 기록' })).not.toBeInTheDocument())
  fireEvent.click(screen.getByRole('link', { name: '공부' }))
  expect(await screen.findByText('0개')).toBeVisible()
})
