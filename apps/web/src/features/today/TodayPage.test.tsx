import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { SessionClient } from '@todorok/api-client'
import { App } from '../../App'
afterEach(() => {
  cleanup()
  localStorage.clear()
  window.history.replaceState({}, '', '/')
})
it('keeps four empty groups and preserves quick-add input after a failed save', async () => {
  const session = new SessionClient({
    fetcher: async (url, init) => {
      const path = String(url)
      const value = path.endsWith('/refresh')
        ? {
            accessToken: 'token',
            userId: 'owner',
            expiresAt: '2099-01-01T00:00:00Z',
          }
        : path.includes('/calendar?')
          ? { from: '2026-09-06', to: '2026-09-12', days: [] }
          : path.includes('/calendar/')
            ? { date: '2026-09-07', tasks: [] }
            : {}
      return new Response(JSON.stringify(value), {
        status: init?.method === 'POST' && path.endsWith('/tasks') ? 500 : 200,
        headers: { 'content-type': 'application/json' },
      })
    },
  })
  render(<App session={session} />)
  for (const name of ['할 일', '운동', '공부', '클라이밍'])
    expect(
      await screen.findByRole('button', { name: `${name} 추가` }),
    ).toBeVisible()
  fireEvent.click(screen.getByRole('button', { name: '할 일 추가' }))
  fireEvent.change(screen.getByLabelText('제목'), {
    target: { value: '입력 보존' },
  })
  fireEvent.click(screen.getByRole('button', { name: '저장' }))
  expect(await screen.findByRole('alert')).toBeVisible()
  expect(screen.getByLabelText('제목')).toHaveValue('입력 보존')
})
