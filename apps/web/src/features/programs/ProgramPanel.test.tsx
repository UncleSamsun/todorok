import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { SessionClient } from '@todorok/api-client'
import { App } from '../../App'

afterEach(() => { cleanup(); history.replaceState({}, '', '/') })

it('등록한 프로그램의 현재 회차와 권장 세트를 운동 탭에서 보여준다', async () => {
  const writes: Record<string, unknown>[] = []
  let enrolled = false
  const enrollment = {
    enrollmentId: '50000000-0000-0000-0000-000000000001', catalogKey: 'synthetic-pushup', catalogVersion: 1,
    recommendedWeek: 1, startWeek: 2, currentWeek: 2, currentSession: 1, status: 'ACTIVE',
    target: { sessionId: '60000000-0000-0000-0000-000000000001', week: 2, session: 1, targetSets: [4, 3, 3] },
  }
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.endsWith('/programs')) return Response.json([{ catalogKey: 'synthetic-pushup', catalogVersion: 1, checksum: 'test', name: '합성 푸시업 프로그램', sessionsPerWeek: 3, totalWeeks: 2, source: { kind: 'SYNTHETIC', label: '테스트 전용 합성 자료', conditions: ['테스트용'], cautions: ['실제 운동 처방이 아님'] } }])
    if (path.endsWith('/program-enrollments') && init?.method === 'POST') { writes.push(JSON.parse(String(init.body))); enrolled = true; return Response.json(enrollment, { status: 201 }) }
    if (path.endsWith('/program-enrollments')) return Response.json(enrolled ? [enrollment] : [])
    if (path.includes('/activities/summary')) return Response.json({ month: '2026-09', activityType: 'WORKOUT', completedCount: 0, durationSeconds: 0 })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-09-01', to: '2026-09-30', days: [] })
    if (path.endsWith('/activities?limit=20')) return Response.json({ items: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-09-07', tasks: [] })
    return Response.json({}, { status: 404 })
  } })

  await act(async () => { render(<App session={session} />) })
  fireEvent.click(await screen.findByRole('link', { name: '운동' }))
  fireEvent.click(await screen.findByRole('button', { name: '합성 푸시업 프로그램 등록' }))
  fireEvent.change(screen.getByLabelText('초기 검사 횟수'), { target: { value: '12' } })
  fireEvent.change(screen.getByLabelText('시작 주차'), { target: { value: '2' } })
  fireEvent.click(screen.getByRole('button', { name: '프로그램 시작' }))

  expect(await screen.findByText('2주차 1회')).toBeVisible()
  expect(screen.getByText('4 · 3 · 3회')).toBeVisible()
  expect(screen.getByText('테스트 전용 합성 자료')).toBeVisible()
  expect(writes[0]).toMatchObject({ catalogKey: 'synthetic-pushup', catalogVersion: 1, initialTestValue: 12, startWeek: 2 })
  expect(writes[0]?.commandId).toEqual(expect.any(String))
})
