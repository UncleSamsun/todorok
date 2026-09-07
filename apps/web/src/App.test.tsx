import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { App } from './App'
import { SessionClient } from '@todorok/api-client'
import { createSessionQueryClient } from './app/query-client'
import { seoulToday } from './app/router'

afterEach(() => {
  cleanup()
  window.history.replaceState({}, '', '/')
  localStorage.clear()
  vi.restoreAllMocks()
})
const session = {
  accessToken: 'never-store-this-token',
  userId: 'owner',
  expiresAt: '2099-01-01T00:00:00Z',
}
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  })
const calendarResponse = (url: unknown) =>
  String(url).includes('/calendar?')
    ? json({ from: seoulToday(), to: seoulToday(), days: [] })
    : String(url).includes('/calendar/')
      ? json({ date: seoulToday(), tasks: [] })
      : null

describe('App', () => {
  it('starts on the Seoul date across the UTC midnight boundary', () => {
    vi.useFakeTimers()
    try {
      vi.setSystemTime(new Date('2026-09-06T15:01:00Z'))
      expect(seoulToday()).toBe('2026-09-07')
    } finally {
      vi.useRealTimers()
    }
  })
  it('retains credentials after rejected login then opens today without persisting tokens', async () => {
    let reject = true
    const storage = vi.spyOn(Storage.prototype, 'setItem')
    const client = new SessionClient({
      fetcher: async (url) =>
        calendarResponse(url) ??
        (String(url).endsWith('/login') && !reject
          ? json(session)
          : json({}, 401)),
    })
    render(<App session={client} />)
    expect(await screen.findByRole('heading', { name: '토도록' })).toBeVisible()
    fireEvent.change(screen.getByLabelText('이메일'), {
      target: { value: 'owner@example.com' },
    })
    fireEvent.change(screen.getByLabelText('비밀번호'), {
      target: { value: 'my-password' },
    })
    fireEvent.click(screen.getByRole('button', { name: '로그인' }))
    expect(await screen.findByRole('alert')).toBeVisible()
    expect(screen.getByLabelText('이메일')).toHaveValue('owner@example.com')
    expect(screen.getByLabelText('비밀번호')).toHaveValue('my-password')
    reject = false
    fireEvent.click(screen.getByRole('button', { name: '로그인' }))
    expect(
      await screen.findByRole('region', { name: '선택 날짜' }),
    ).toBeVisible()
    expect(window.location.pathname).toBe('/today')
    expect(storage).not.toHaveBeenCalled()
  })
  it('restores to Seoul today even from a past domain URL and clears query cache after logout', async () => {
    window.history.replaceState({}, '', '/study?date=2020-01-01')
    const queries = createSessionQueryClient()
    const client = new SessionClient({
      fetcher: async (url) =>
        calendarResponse(url) ??
        (String(url).endsWith('/logout')
          ? new Response(null, { status: 204 })
          : json(session)),
    })
    render(<App session={client} queryClient={queries} />)
    expect(
      await screen.findByRole('region', { name: '선택 날짜' }),
    ).toBeVisible()
    const today = new Intl.DateTimeFormat('sv-SE', {
      timeZone: 'Asia/Seoul',
    }).format(new Date())
    expect(screen.getByText(today)).toBeVisible()
    queries.setQueryData(['private'], 'private data')
    fireEvent.click(screen.getByRole('button', { name: '로그아웃' }))
    expect(await screen.findByRole('button', { name: '로그인' })).toBeVisible()
    expect(queries.getQueryData(['private'])).toBeUndefined()
  })
  it('blocks a previous user query response after changing session', async () => {
    let resolve!: (value: string) => void
    const queries = createSessionQueryClient()
    const client = new SessionClient({
      fetcher: async (url) =>
        calendarResponse(url) ??
        (String(url).endsWith('/logout')
          ? new Response(null, { status: 204 })
          : json(session)),
    })
    render(<App session={client} queryClient={queries} />)
    await screen.findByRole('region', { name: '선택 날짜' })
    const pending = queries
      .fetchQuery({
        queryKey: ['private'],
        queryFn: () =>
          new Promise<string>((r) => {
            resolve = r
          }),
      })
      .catch(() => undefined)
    await client.logout()
    resolve('old user data')
    await pending
    await waitFor(() =>
      expect(queries.getQueryData(['private'])).toBeUndefined(),
    )
  })
})
