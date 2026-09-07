import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
} from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StickyNote } from './StickyNote'
import type { planner } from '@todorok/api-client'
beforeEach(() => vi.useFakeTimers())
afterEach(() => {
  cleanup()
  vi.useRealTimers()
})
function deferred<T>() {
  let resolve!: (v: T) => void, reject!: (e: unknown) => void
  const promise = new Promise<T>((a, b) => {
    resolve = a
    reject = b
  })
  return { promise, resolve, reject }
}
function setup(isCurrent = () => true) {
  const pending: ReturnType<typeof deferred<planner.DailyNoteResponse>>[] = []
  const getDailyNote = vi.fn(async ({ date }: { date: string }) => ({
    date,
    content: 'server',
    version: 0,
  }))
  const updateDailyNote = vi.fn(() => {
    const d = deferred<planner.DailyNoteResponse>()
    pending.push(d)
    return d.promise
  })
  const api = { getDailyNote, updateDailyNote }
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const view = (date: string) => (
    <QueryClientProvider client={client}>
      <StickyNote date={date} api={api} isCurrent={isCurrent} />
    </QueryClientProvider>
  )
  const rendered = render(view('2026-09-09'))
  return { api, pending, client, ...rendered, view }
}
async function tick(ms = 0) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms)
  })
}
function edit(value: string) {
  fireEvent.change(screen.getByRole('textbox', { name: '날짜 메모' }), {
    target: { value },
  })
}
async function open() {
  await tick()
  fireEvent.click(screen.getByRole('button', { name: /스티키노트/ }))
}
it('debounces for 500ms and sends only the latest continuous input', async () => {
  const s = setup()
  await open()
  edit('first')
  await tick(499)
  expect(s.api.updateDailyNote).not.toHaveBeenCalled()
  edit('last')
  await tick(499)
  expect(s.api.updateDailyNote).not.toHaveBeenCalled()
  await tick(1)
  expect(s.api.updateDailyNote).toHaveBeenCalledWith({
    date: '2026-09-09',
    updateDailyNoteRequest: { content: 'last', expectedVersion: 0 },
  })
})
it('does not let a stale success mark a newer draft saved or regress cache on reversed responses', async () => {
  const s = setup()
  await open()
  edit('first')
  await tick(500)
  edit('last')
  await tick(500)
  await act(async () =>
    s.pending[1]!.resolve({
      date: '2026-09-09',
      content: 'last',
      version: 2,
    }),
  )
  expect(screen.getByRole('status').textContent).toContain('저장됨')
  await act(async () =>
    s.pending[0]!.resolve({
      date: '2026-09-09',
      content: 'first',
      version: 1,
    }),
  )
  expect(screen.getByRole('textbox')).toHaveValue('last')
  expect(s.client.getQueryData(['note', '2026-09-09'])).toMatchObject({
    content: 'last',
    version: 2,
  })
})
it('keeps newer input dirty when an earlier request succeeds', async () => {
  const s = setup()
  await open()
  edit('first')
  await tick(500)
  edit('last')
  await act(async () =>
    s.pending[0]!.resolve({
      date: '2026-09-09',
      content: 'first',
      version: 1,
    }),
  )
  expect(screen.getByRole('status').textContent).not.toContain('저장됨')
  await tick(500)
  expect(s.api.updateDailyNote.mock.calls.at(-1)).toMatchObject([
    { updateDailyNoteRequest: { content: 'last', expectedVersion: 1 } },
  ])
})
it('updates only the old date cache after switching and preserves each draft', async () => {
  const s = setup()
  await open()
  edit('old draft')
  await tick(500)
  s.rerender(s.view('2026-09-10'))
  await tick()
  edit('new draft')
  await act(async () =>
    s.pending[0]!.resolve({
      date: '2026-09-09',
      content: 'old draft',
      version: 1,
    }),
  )
  expect(screen.getByRole('textbox')).toHaveValue('new draft')
  expect(s.client.getQueryData(['note', '2026-09-09'])).toMatchObject({
    content: 'old draft',
  })
  s.rerender(s.view('2026-09-09'))
  await tick()
  expect(screen.getByRole('textbox')).toHaveValue('old draft')
})
it.each([409, 'network'])(
  'preserves input after %s and retries with refreshed version',
  async (reason) => {
    const s = setup()
    await open()
    edit('my draft')
    await tick(500)
    await act(async () => s.pending[0]!.reject(reason))
    expect(screen.getByRole('textbox')).toHaveValue('my draft')
    expect(screen.getByRole('alert').textContent).toContain('저장 실패')
    s.api.getDailyNote.mockResolvedValue({
      date: '2026-09-09',
      content: 'remote text',
      version: 4,
    })
    fireEvent.click(screen.getByRole('button', { name: '저장 다시 시도' }))
    await tick()
    expect(screen.getByRole('textbox')).toHaveValue('my draft')
    expect(s.api.updateDailyNote.mock.calls.at(-1)).toMatchObject([
      { updateDailyNoteRequest: { content: 'my draft', expectedVersion: 4 } },
    ])
    await act(async () =>
      s.pending[1]!.resolve({
        date: '2026-09-09',
        content: 'my draft',
        version: 5,
      }),
    )
    expect(screen.getByRole('status').textContent).toContain('저장됨')
  },
)
it('does not repopulate cleared account cache after unmount', async () => {
  const s = setup()
  await open()
  edit('private')
  await tick(500)
  s.unmount()
  s.client.clear()
  await act(async () =>
    s.pending[0]!.resolve({
      date: '2026-09-09',
      content: 'private',
      version: 1,
    }),
  )
  expect(s.client.getQueryData(['note', '2026-09-09'])).toBeUndefined()
})
it('ignores late completion immediately when account generation changes before unmount', async () => {
  let current = true
  const s = setup(() => current)
  await open()
  edit('private')
  await tick(500)
  current = false
  s.client.clear()
  await act(async () =>
    s.pending[0]!.resolve({
      date: '2026-09-09',
      content: 'private',
      version: 1,
    }),
  )
  expect(s.client.getQueryData(['note', '2026-09-09'])).toBeUndefined()
})
it('does not send a debounce draft after the account changes before unmount', async () => {
  let current = true
  const s = setup(() => current)
  await open()
  edit('old account draft')
  current = false
  await tick(500)
  expect(s.api.updateDailyNote).not.toHaveBeenCalled()
})
it('ignores an earlier failure after a newer success', async () => {
  const s = setup()
  await open()
  edit('first')
  await tick(500)
  edit('last')
  await tick(500)
  await act(async () =>
    s.pending[1]!.resolve({
      date: '2026-09-09',
      content: 'last',
      version: 1,
    }),
  )
  await act(async () => s.pending[0]!.reject(409))
  expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  expect(screen.getByRole('status').textContent).toContain('저장됨')
})
it('preserves the draft when retry version refresh also fails', async () => {
  const s = setup()
  await open()
  edit('draft')
  await tick(500)
  await act(async () => s.pending[0]!.reject(409))
  s.api.getDailyNote.mockRejectedValue(new Error('offline'))
  fireEvent.click(screen.getByRole('button', { name: '저장 다시 시도' }))
  await tick()
  expect(screen.getByRole('textbox')).toHaveValue('draft')
  expect(screen.getByRole('alert').textContent).toContain('저장 실패')
  expect(s.api.updateDailyNote).toHaveBeenCalledTimes(1)
})
it('refreshes clean drafts from newer server data but preserves dirty draft and expected version', async () => {
  const s = setup()
  await open()
  s.api.getDailyNote.mockResolvedValue({
    date: '2026-09-09',
    content: 'remote clean',
    version: 2,
  })
  await act(async () => {
    await s.client.refetchQueries({ queryKey: ['note', '2026-09-09'] })
  })
  await tick()
  expect(screen.getByRole('textbox')).toHaveValue('remote clean')
  edit('my dirty draft')
  s.api.getDailyNote.mockResolvedValue({
    date: '2026-09-09',
    content: 'remote dirty',
    version: 3,
  })
  await act(async () => {
    await s.client.refetchQueries({ queryKey: ['note', '2026-09-09'] })
  })
  await tick()
  expect(screen.getByRole('textbox')).toHaveValue('my dirty draft')
  await tick(500)
  expect(s.api.updateDailyNote.mock.calls.at(-1)).toMatchObject([
    {
      updateDailyNoteRequest: {
        content: 'my dirty draft',
        expectedVersion: 2,
      },
    },
  ])
})
