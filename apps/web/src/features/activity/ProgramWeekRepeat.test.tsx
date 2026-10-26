import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { SessionClient } from '@todorok/api-client'
import { App } from '../../App'

afterEach(() => { cleanup(); localStorage.clear(); history.replaceState({}, '', '/') })

async function open(slot: number | null = 5, loseFirstResponse = false) {
  const writes: Record<string, any>[] = []
  const task = { taskId: 'program-task', userId: 'owner', title: '프로그램 운동', taskType: 'WORKOUT',
    scheduledDate: '2026-10-23', status: 'PLANNED', version: 0 }
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-10-23', movedCount: 0 })
    if (path.includes('/calendar?')) return Response.json({ days: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-10-23', tasks: [task] })
    if (path.includes('/notes/')) return Response.json({ date: '2026-10-23', content: '', version: null })
    if (path.endsWith('/tasks/program-task')) return Response.json(task)
    if (path.endsWith('/tasks/program-task/record-template')) return Response.json({ linked: false,
      ...(slot === null ? {} : { programSession: { week: 4, session: slot, sessionsPerWeek: 5, canRepeatWeek: slot === 5 } }) })
    if (path.endsWith('/activities') && init?.method === 'POST') {
      const body = JSON.parse(String(init.body)); writes.push(body)
      if (loseFirstResponse && writes.length === 1) throw new TypeError('response lost')
      return Response.json({ activityId: 'saved', commandId: body.commandId, taskId: task.taskId, userId: 'owner',
        activityType: 'WORKOUT', performedAt: body.performedAt, detail: body.detail, status: 'COMPLETED', version: 0, syncState: 'APPLIED' }, { status: 201 })
    }
    return Response.json({}, { status: 404 })
  } })
  render(<App session={session} />)
  const record = await screen.findByRole('button', { name: '프로그램 운동 기록' })
  await waitFor(() => expect(record).toBeEnabled())
  fireEvent.click(record)
  await screen.findByRole('button', { name: '기록 저장' })
  return writes
}

it('saves an explicit week repeat with the final session and preserves it on an uncertain retry', async () => {
  const writes = await open(5, true)
  const repeat = screen.getByRole('button', { name: '이번 주 다시하기' })
  fireEvent.click(repeat)
  expect(repeat).toHaveAttribute('aria-pressed', 'true')
  expect(screen.getByText('기록을 저장하고 4주차 1회차부터 다시 시작합니다.')).toBeVisible()
  fireEvent.click(screen.getByRole('button', { name: '기록 저장 · 이번 주 다시하기' }))
  const retry = await screen.findByRole('button', { name: '같은 요청 다시 보내기' })
  expect(repeat).toBeDisabled()
  fireEvent.click(retry)
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[0]).toMatchObject({ repeatProgramWeek: true, completionStatus: 'COMPLETED', detail: { workout: {} } })
  expect(writes[1]).toEqual(writes[0])
})

it('keeps normal progress as the default for the final session', async () => {
  const writes = await open()
  expect(screen.getByRole('button', { name: '이번 주 다시하기' })).toHaveAttribute('aria-pressed', 'false')
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await waitFor(() => expect(writes).toHaveLength(1))
  expect(writes[0]).not.toHaveProperty('repeatProgramWeek')
})

it.each([2, null])('does not offer week repeat for a nonfinal or ordinary workout (slot=%s)', async slot => {
  await open(slot)
  expect(screen.queryByRole('button', { name: '이번 주 다시하기' })).not.toBeInTheDocument()
})
