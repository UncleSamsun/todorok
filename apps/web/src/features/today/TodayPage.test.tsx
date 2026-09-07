import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { SessionClient } from '@todorok/api-client'
import { App } from '../../App'
afterEach(() => {
  cleanup()
  localStorage.clear()
  window.history.replaceState({}, '', '/')
})

it('keeps the draft and expected version together when the same task is clicked after a remote edit', async () => {
  let remote = {
    taskId: '00000000-0000-0000-0000-000000000004',
    userId: 'owner',
    title: '원래 제목',
    taskType: 'GENERAL',
    scheduledDate: '2026-09-07',
    status: 'PLANNED',
    version: 0,
  }
  const writes: { title: string; scheduledDate: string; version: number }[] = []
  const session = new SessionClient({
    fetcher: async (url, init) => {
      const path = String(url)
      let value: unknown
      let status = 200
      if (path.endsWith('/refresh')) {
        value = {
          accessToken: 'token',
          userId: 'owner',
          expiresAt: '2099-01-01T00:00:00Z',
        }
      } else if (path.endsWith('/rollover')) {
        value = { today: '2026-09-07', movedCount: 0 }
      } else if (path.includes('/calendar?')) {
        value = { from: '2026-09-06', to: '2026-09-12', days: [] }
      } else if (path.includes('/calendar/')) {
        value = { date: '2026-09-07', tasks: [remote] }
      } else if (init?.method === 'PATCH') {
        const update = JSON.parse(String(init.body)) as (typeof writes)[number]
        writes.push(update)
        if (update.version !== remote.version) {
          status = 409
          value = { code: 'VERSION_CONFLICT' }
        } else {
          remote = { ...remote, ...update, version: remote.version + 1 }
          value = remote
        }
      } else {
        value = remote
      }
      return new Response(JSON.stringify(value), {
        status,
        headers: { 'content-type': 'application/json' },
      })
    },
  })
  render(<App session={session} />)
  fireEvent.click(
    await screen.findByRole(
      'button',
      { name: '원래 제목 수정' },
      { timeout: 5000 },
    ),
  )
  expect(await screen.findByLabelText('제목 수정')).toHaveValue('원래 제목')
  fireEvent.change(screen.getByLabelText('제목 수정'), {
    target: { value: '내 초안' },
  })
  fireEvent.change(screen.getByLabelText('날짜 수정'), {
    target: { value: '2026-09-08' },
  })

  remote = {
    ...remote,
    title: '다른 탭 변경',
    scheduledDate: '2026-09-09',
    version: 1,
  }
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name: '원래 제목 수정' }))
  })
  fireEvent.click(screen.getByRole('button', { name: '수정 저장' }))
  await waitFor(() => expect(writes).toHaveLength(1))
  expect(writes[0]).toEqual({
    title: '내 초안',
    scheduledDate: '2026-09-08',
    version: 0,
  })
  expect(await screen.findByRole('alert')).toBeVisible()
  expect(screen.getByLabelText('제목 수정')).toHaveValue('내 초안')
  expect(screen.getByLabelText('날짜 수정')).toHaveValue('2026-09-08')
  expect(remote.title).toBe('다른 탭 변경')
  expect(remote.version).toBe(1)

  fireEvent.click(screen.getByRole('button', { name: '닫기' }))
  fireEvent.click(screen.getByRole('button', { name: '원래 제목 수정' }))
  expect(await screen.findByLabelText('제목 수정')).toHaveValue('다른 탭 변경')
  expect(screen.getByLabelText('날짜 수정')).toHaveValue('2026-09-09')
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
        : path.endsWith('/rollover')
          ? { today: '2026-09-07', movedCount: 0 }
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
