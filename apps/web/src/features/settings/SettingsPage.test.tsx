import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { SessionClient } from '@todorok/api-client'
import { App } from '../../App'

afterEach(() => { cleanup(); history.replaceState({}, '', '/'); delete document.documentElement.dataset.theme })

it('계정 테마를 저장하고 현재 화면에 즉시 적용한다', async () => {
  const writes: Record<string, unknown>[] = []
  let theme = 'SYSTEM', revision = 0
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.endsWith('/preferences') && init?.method === 'PUT') { const body = JSON.parse(String(init.body)); writes.push(body); theme = body.theme; return Response.json({ theme, revision: revision++ }) }
    if (path.endsWith('/preferences')) return Response.json({ theme, revision })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-09-01', to: '2026-09-30', days: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-09-07', tasks: [] })
    if (path.includes('/notes/')) return Response.json({ date: '2026-09-07', content: '', version: null })
    return Response.json({}, { status: 404 })
  } })
  await act(async () => { render(<App session={session} />) })
  fireEvent.click(await screen.findByRole('link', { name: '설정' }))
  const select = await screen.findByLabelText('테마')
  expect(select).toHaveValue('SYSTEM')
  fireEvent.change(select, { target: { value: 'DARK' } })
  await waitFor(() => expect(writes).toHaveLength(1))
  expect(writes[0]).toMatchObject({ theme: 'DARK', expectedRevision: 0 })
  expect(document.documentElement.dataset.theme).toBe('dark')
})

it('설정에서 홈 화면 설치 안내를 보여 준다', async () => {
  const session = new SessionClient({ fetcher: async (url) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.endsWith('/preferences')) return Response.json({ theme: 'SYSTEM', revision: 0 })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-09-01', to: '2026-09-30', days: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-09-07', tasks: [] })
    if (path.includes('/notes/')) return Response.json({ date: '2026-09-07', content: '', version: null })
    return Response.json({}, { status: 404 })
  } })
  await act(async () => { render(<App session={session} />) })
  fireEvent.click(await screen.findByRole('link', { name: '설정' }))
  expect(await screen.findByText('홈 화면에 추가')).toBeVisible()
})
