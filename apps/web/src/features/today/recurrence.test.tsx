import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { QuickAdd } from './QuickAdd'
import { App } from '../../App'
import { SessionClient, planner } from '@todorok/api-client'
afterEach(() => {
  cleanup()
  localStorage.clear()
  window.history.replaceState({}, '', '/')
})

it('submits a weekly recurrence with explicit days and end date from quick add', () => {
  const save = vi.fn()
  render(
    <QuickAdd
      date="2026-09-07"
      type={planner.TaskType.General}
      busy={false}
      error=""
      save={save}
      cancel={() => {}}
    />,
  )
  fireEvent.change(screen.getByLabelText('제목'), {
    target: { value: '매주 읽기' },
  })
  fireEvent.click(screen.getByText('옵션'))
  fireEvent.change(screen.getByLabelText('반복'), {
    target: { value: 'WEEKLY' },
  })
  fireEvent.click(screen.getByLabelText('수요일'))
  fireEvent.change(screen.getByLabelText('반복 간격'), {
    target: { value: '2' },
  })
  fireEvent.change(screen.getByLabelText('반복 종료일'), {
    target: { value: '2026-12-31' },
  })
  fireEvent.click(screen.getByRole('button', { name: '저장' }))
  expect(save).toHaveBeenCalledWith('매주 읽기', '2026-09-07', {
    rule: {
      frequency: 'WEEKLY',
      interval: 2,
      weekdays: new Set([1, 3]),
      monthDay: 7,
    },
    endDate: '2026-12-31',
  })
})

it('waits for successful rollover before reading calendars and exposes failure with retry', async () => {
  const calls: string[] = []
  let reject = true
  const session = new SessionClient({
    fetcher: async (url) => {
      const path = String(url)
      calls.push(path)
      let data: unknown
      if (path.endsWith('/refresh'))
        data = {
          accessToken: 'test',
          userId: 'owner',
          expiresAt: '2099-01-01T00:00:00Z',
        }
      else if (path.endsWith('/rollover')) {
        if (reject)
          return new Response('{}', {
            status: 503,
            headers: { 'content-type': 'application/json' },
          })
        data = { today: '2026-09-07', movedCount: 1 }
      } else if (path.includes('/calendar?'))
        data = { from: '2026-09-06', to: '2026-09-12', days: [] }
      else data = { date: '2026-09-07', tasks: [] }
      return new Response(JSON.stringify(data), {
        headers: { 'content-type': 'application/json' },
      })
    },
  })
  render(<App session={session} />)
  expect(
    await screen.findByText('지난 할 일을 이월하지 못했습니다.'),
  ).toBeVisible()
  expect(calls.filter((p) => p.includes('/calendar'))).toHaveLength(0)
  reject = false
  fireEvent.click(screen.getByRole('button', { name: '이월 다시 시도' }))
  await waitFor(() =>
    expect(calls.filter((p) => p.includes('/calendar'))).toHaveLength(2),
  )
  expect(
    screen.queryByText('지난 할 일을 이월하지 못했습니다.'),
  ).not.toBeInTheDocument()
})
