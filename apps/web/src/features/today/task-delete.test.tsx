import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { SessionClient } from '@todorok/api-client'
import { App } from '../../App'

afterEach(() => { cleanup(); localStorage.clear(); window.history.replaceState({}, '', '/') })

function open(reject = false, recurring = false) {
  let deleted = false
  const writes: { path: string; method: string }[] = []
  const task = { taskId: 'delete-task', userId: 'owner', title: '삭제할 일정', taskType: 'GENERAL',
    scheduledDate: '2026-10-23', status: 'PLANNED', version: 4, ...(recurring ? { seriesId: 'series-1' } : {}) }
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (init?.method === 'DELETE') {
      writes.push({ path, method: init.method })
      if (reject) return Response.json({ code: 'VERSION_CONFLICT' }, { status: 409 })
      deleted = true
      return new Response(null, { status: 204 })
    }
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-10-23', movedCount: 0 })
    if (path.includes('/calendar?')) return Response.json({ days: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-10-23', tasks: deleted ? [] : [task] })
    if (path.includes('/notes/')) return Response.json({ date: '2026-10-23', content: '', version: null })
    return Response.json(task)
  } })
  window.history.replaceState({}, '', '/today')
  render(<App session={session} />)
  return writes
}

it.each([false, true])('deletes only the selected task from its menu and refreshes the list (recurring=%s)', async recurring => {
  const writes = open(false, recurring)
  const menu = await screen.findByRole('button', { name: '삭제할 일정 메뉴' })
  await waitFor(() => expect(menu).toBeEnabled())
  fireEvent.click(menu)
  fireEvent.click(screen.getByRole('menuitem', { name: '삭제' }))
  await waitFor(() => expect(screen.queryByRole('button', { name: '삭제할 일정 메뉴' })).not.toBeInTheDocument())
  expect(writes).toEqual([{ method: 'DELETE', path: '/api/planner/v1/tasks/delete-task?version=4' }])
})

it('keeps the task visible and reports a rejected deletion', async () => {
  const writes = open(true)
  const menu = await screen.findByRole('button', { name: '삭제할 일정 메뉴' })
  await waitFor(() => expect(menu).toBeEnabled())
  fireEvent.click(menu)
  fireEvent.click(screen.getByRole('menuitem', { name: '삭제' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('저장하지 못했습니다')
  expect(screen.getByRole('button', { name: '삭제할 일정 메뉴' })).toBeEnabled()
  expect(writes).toHaveLength(1)
})
