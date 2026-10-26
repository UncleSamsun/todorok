import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { SessionClient } from '@todorok/api-client'
import { App } from '../../App'

afterEach(() => { cleanup(); localStorage.clear(); history.replaceState({}, '', '/') })

async function open(reject = false) {
  const writes: Record<string, unknown>[] = []
  const enrollment = { enrollmentId: 'enrollment-1', catalogKey: 'five-session-program', catalogVersion: 1,
    recommendedWeek: 1, startWeek: 1, currentWeek: 3, currentSession: 2, revision: 4, status: 'ACTIVE',
    target: { sessionId: 'session-1', week: 3, session: 2, targetSets: [4, 3, 3] } }
  const catalog = { catalogKey: enrollment.catalogKey, catalogVersion: 1, name: '주 5회 프로그램',
    checksum: 'test', sessionsPerWeek: 5, totalWeeks: 6, source: { kind: 'SYNTHETIC', label: '테스트' } }
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-10-23', movedCount: 0 })
    if (path.endsWith('/programs')) return Response.json([catalog])
    if (path.endsWith('/program-enrollments')) return Response.json([enrollment])
    if (path.endsWith('/program-enrollments/enrollment-1/sessions')) {
      const body = JSON.parse(String(init?.body)); writes.push(body)
      if (reject) return Response.json({ code: 'VERSION_CONFLICT' }, { status: 409 })
      Object.assign(enrollment, { currentWeek: body.week, currentSession: body.session, revision: 5,
        target: { sessionId: 'session-2', week: body.week, session: body.session, targetSets: [5, 4, 3] } })
      return Response.json(enrollment)
    }
    if (path.includes('/activities/summary')) return Response.json({ month: '2026-10', activityType: 'WORKOUT', completedCount: 0, durationSeconds: 0 })
    if (path.includes('/activities?') || path.includes('/templates')) return Response.json({ items: [] })
    if (path.includes('/calendar?')) return Response.json({ days: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-10-23', tasks: [] })
    if (path.includes('/notes/')) return Response.json({ date: '2026-10-23', content: '', version: null })
    return Response.json({}, { status: 404 })
  } })
  history.replaceState({}, '', '/workout')
  render(<App session={session} />)
  await waitFor(() => expect(screen.getByRole('button', { name: '할 일 추가' })).toBeEnabled())
  fireEvent.click(screen.getByRole('link', { name: '운동' }))
  await waitFor(() => expect(screen.getByRole('button', { name: '주 5회 프로그램 상태 수정' })).toBeEnabled())
  return writes
}

it('edits and restores the enrolled week and session using catalog bounds', async () => {
  const writes = await open()
  fireEvent.click(await screen.findByRole('button', { name: '주 5회 프로그램 상태 수정' }))
  let dialog = screen.getByRole('dialog', { name: '주 5회 프로그램 상태 수정' })
  expect(within(dialog).getByLabelText('주차')).toHaveValue('3')
  expect(within(dialog).getByLabelText('회차')).toHaveValue('2')
  expect(within(dialog).getAllByRole('option').map(option => option.textContent)).toContain('5회차')
  fireEvent.change(within(dialog).getByLabelText('주차'), { target: { value: '4' } })
  fireEvent.change(within(dialog).getByLabelText('회차'), { target: { value: '5' } })
  fireEvent.click(within(dialog).getByRole('button', { name: '상태 저장' }))
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  expect(writes).toEqual([expect.objectContaining({ week: 4, session: 5, expectedRevision: 4, commandId: expect.any(String) })])
  expect(screen.getByText('4주차 5회')).toBeVisible()
  fireEvent.click(screen.getByRole('button', { name: '주 5회 프로그램 상태 수정' }))
  dialog = screen.getByRole('dialog', { name: '주 5회 프로그램 상태 수정' })
  expect(within(dialog).getByLabelText('주차')).toHaveValue('4')
  expect(within(dialog).getByLabelText('회차')).toHaveValue('5')
})

it('preserves the selected position when a state change is rejected', async () => {
  const writes = await open(true)
  fireEvent.click(await screen.findByRole('button', { name: '주 5회 프로그램 상태 수정' }))
  const dialog = screen.getByRole('dialog', { name: '주 5회 프로그램 상태 수정' })
  fireEvent.change(within(dialog).getByLabelText('주차'), { target: { value: '2' } })
  fireEvent.click(within(dialog).getByRole('button', { name: '상태 저장' }))
  expect(await within(dialog).findByRole('alert')).toBeVisible()
  expect(within(dialog).getByLabelText('주차')).toHaveValue('2')
  expect(screen.getByText('3주차 2회')).toBeVisible()
  expect(writes).toHaveLength(1)
})
