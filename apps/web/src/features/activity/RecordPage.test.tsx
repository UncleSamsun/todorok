import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { SessionClient } from '@todorok/api-client'
import { App } from '../../App'

afterEach(() => { cleanup(); window.history.replaceState({}, '', '/') })

function sessionFor(writes: unknown[], failFirst: false | 'network' | 'validation' = false) {
  let attempts = 0
  return new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-09-06', to: '2026-09-12', days: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-09-07', tasks: [{ taskId: 'task-1', userId: 'owner', title: '볼더링', taskType: 'CLIMBING', scheduledDate: '2026-09-07', status: 'PLANNED', version: 0 }] })
    if (path.includes('/notes/')) return Response.json({ date: '2026-09-07', content: '', version: null })
    if (path.endsWith('/tasks/task-1')) return Response.json({ taskId: 'task-1', userId: 'owner', title: '볼더링', taskType: 'CLIMBING', scheduledDate: '2026-09-07', status: 'PLANNED', version: 0 })
    if (path.endsWith('/tasks/task-1/record-template')) return Response.json({ linked: false })
    if (path.endsWith('/activities') && init?.method === 'POST') {
      writes.push(JSON.parse(String(init.body)))
      attempts++
      if (failFirst === 'network' && attempts === 1) throw new TypeError('response lost')
      if (failFirst === 'validation' && attempts === 1) return Response.json({ code: 'VALIDATION_FAILED' }, { status: 422 })
      return Response.json({ activityId: 'activity-1', commandId: (writes.at(-1) as {commandId:string}).commandId, taskId: 'task-1', userId: 'owner', activityType: 'CLIMBING', performedAt: '2026-09-07T00:00:00+09:00', detail: { climbing: { rounds: [{ attempts: 0, completed: false }] } }, status: 'COMPLETED', version: 0, syncState: 'APPLIED' }, { status: 201 })
    }
    return Response.json({ code: 'NOT_FOUND' }, { status: 404 })
  } })
}

it('keeps zero and false, omits optional time, and returns to performed date', async () => {
  const writes: unknown[] = []
  window.history.replaceState({}, '', '/today')
  render(<App session={sessionFor(writes)} />)
  fireEvent.click(await screen.findByRole('button', { name: '볼더링 기록' }))
  fireEvent.click(await screen.findByRole('button', { name: '라운드 추가' }))
  fireEvent.change(screen.getByLabelText('시도 횟수'), { target: { value: '0' } })
  fireEvent.change(screen.getByLabelText('완등 여부'), { target: { value: 'false' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await waitFor(() => expect(window.location.pathname + window.location.search).toBe('/today?date=2026-09-07&activityId=activity-1'))
  expect(writes).toHaveLength(1)
  expect(writes[0]).toMatchObject({ detail: { climbing: { rounds: [{ attempts: 0, completed: false }] } } })
  expect(writes[0]).not.toHaveProperty('startedAt')
  expect(writes[0]).not.toHaveProperty('endedAt')
})

it('retries an uncertain save with the same command id and preserved draft', async () => {
  const writes: any[] = []
  window.history.replaceState({}, '', '/today')
  render(<App session={sessionFor(writes, 'network')} />)
  fireEvent.click(await screen.findByRole('button', { name: '볼더링 기록' }))
  fireEvent.change(await screen.findByLabelText('기록 메모'), { target: { value: '손가락 상태 좋음' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  expect(await screen.findByText(/응답을 확인하지 못했습니다/)).toBeVisible()
  expect(screen.getByLabelText('기록 메모')).toHaveValue('손가락 상태 좋음')
  expect(screen.getByLabelText('기록 메모')).toBeDisabled()
  fireEvent.click(screen.getByRole('button', { name: '같은 요청 다시 보내기' }))
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[1].commandId).toBe(writes[0].commandId)
  expect(writes[1].note).toBe('손가락 상태 좋음')
})

it('does not POST local invalid time and unlocks fields after known validation rejection', async () => {
  const writes: any[] = []
  render(<App session={sessionFor(writes, 'validation')} />)
  fireEvent.click(await screen.findByRole('button', { name: '볼더링 기록' }))
  fireEvent.click(await screen.findByText('실제 시간 추가'))
  fireEvent.change(screen.getByLabelText('시작 시'), { target: { value: '7' } })
  fireEvent.change(screen.getByLabelText('시작 분'), { target: { value: '00' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  expect(await screen.findByText('시작과 종료 시간을 모두 선택해 주세요.')).toBeVisible()
  expect(writes).toHaveLength(0)
  fireEvent.change(screen.getByLabelText('시작 시'), { target: { value: '' } })
  fireEvent.change(screen.getByLabelText('시작 분'), { target: { value: '' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  expect(await screen.findByText(/기록을 저장하지 못했습니다/)).toBeVisible()
  expect(writes).toHaveLength(1)
  expect(screen.getByLabelText('기록 메모')).not.toBeDisabled()
  expect(screen.getByRole('button', { name: '기록 저장' })).toBeVisible()
})

it('records a linked free workout with sets and custom fields together', async () => {
  const writes: any[] = []
  const fieldId = '30000000-0000-0000-0000-000000000003'
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-09-06', to: '2026-09-12', days: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-09-07', tasks: [{ taskId: 'task-1', userId: 'owner', title: '하체 운동', taskType: 'WORKOUT', scheduledDate: '2026-09-07', status: 'PLANNED', version: 0 }] })
    if (path.includes('/notes/')) return Response.json({ date: '2026-09-07', content: '', version: null })
    if (path.endsWith('/tasks/task-1')) return Response.json({ taskId: 'task-1', userId: 'owner', title: '하체 운동', taskType: 'WORKOUT', scheduledDate: '2026-09-07', status: 'PLANNED', version: 0 })
    if (path.endsWith('/tasks/task-1/record-template')) return Response.json({ linked: true, template: { templateId: 'template-1', domain: 'WORKOUT', kind: 'FREE_WORKOUT', archived: false, revision: 0, currentVersion: { templateId: 'template-1', templateVersion: 1, name: '자유 운동', fields: [{ fieldId, name: 'RPE', type: 'NUMBER', unit: '점', position: 0 }] } } })
    if (path.endsWith('/activities') && init?.method === 'POST') {
      const body = JSON.parse(String(init.body)); writes.push(body)
      return Response.json({ activityId: 'activity-1', commandId: body.commandId, taskId: 'task-1', userId: 'owner', activityType: 'WORKOUT', performedAt: '2026-09-07T00:00:00+09:00', detail: body.detail, detailFormat: 'TEMPLATE', status: 'COMPLETED', version: 0, syncState: 'APPLIED' }, { status: 201 })
    }
    return Response.json({ code: 'NOT_FOUND' }, { status: 404 })
  } })
  window.history.replaceState({}, '', '/today')
  render(<App session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: '하체 운동 기록' }))
  fireEvent.click(await screen.findByRole('button', { name: '세트 추가' }))
  fireEvent.change(screen.getByLabelText('운동 1'), { target: { value: '스쿼트' } })
  fireEvent.change(screen.getByLabelText('횟수 1'), { target: { value: '8' } })
  fireEvent.change(await screen.findByLabelText('RPE (점)'), { target: { value: '7.5' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await waitFor(() => expect(writes).toHaveLength(1))
  expect(writes[0]).toMatchObject({ expectedTemplateVersion: 1, detail: { workout: { sets: [{ exercise: '스쿼트', reps: 8 }], fields: [{ fieldId, type: 'NUMBER', numberValue: 7.5 }] } } })
})

it('records a linked climbing session with rounds and custom fields together', async () => {
  const writes: any[] = []
  const fieldId = '60000000-0000-0000-0000-000000000006'
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
    if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
    if (path.includes('/calendar?')) return Response.json({ from: '2026-09-06', to: '2026-09-12', days: [] })
    if (path.includes('/calendar/')) return Response.json({ date: '2026-09-07', tasks: [{ taskId: 'task-2', userId: 'owner', title: '저녁 볼더링', taskType: 'CLIMBING', scheduledDate: '2026-09-07', status: 'PLANNED', version: 0 }] })
    if (path.includes('/notes/')) return Response.json({ date: '2026-09-07', content: '', version: null })
    if (path.endsWith('/tasks/task-2')) return Response.json({ taskId: 'task-2', userId: 'owner', title: '저녁 볼더링', taskType: 'CLIMBING', scheduledDate: '2026-09-07', status: 'PLANNED', version: 0 })
    if (path.endsWith('/tasks/task-2/record-template')) return Response.json({ linked: true, template: { templateId: 'template-2', domain: 'CLIMBING', kind: 'CLIMBING_SESSION', archived: false, revision: 0, currentVersion: { templateId: 'template-2', templateVersion: 1, name: '클라이밍 세션', fields: [{ fieldId, name: '느낌', type: 'MEMO', position: 0 }] } } })
    if (path.endsWith('/activities') && init?.method === 'POST') { const body = JSON.parse(String(init.body)); writes.push(body); return Response.json({ activityId: 'activity-2', commandId: body.commandId, taskId: 'task-2', userId: 'owner', activityType: 'CLIMBING', performedAt: body.performedAt, detail: body.detail, detailFormat: 'TEMPLATE', status: 'COMPLETED', version: 0, syncState: 'APPLIED' }, { status: 201 }) }
    return Response.json({ code: 'NOT_FOUND' }, { status: 404 })
  } })
  window.history.replaceState({}, '', '/today')
  render(<App session={session} />)
  fireEvent.click(await screen.findByRole('button', { name: '저녁 볼더링 기록' }))
  fireEvent.click(await screen.findByRole('button', { name: '라운드 추가' }))
  fireEvent.change(screen.getByLabelText('난이도'), { target: { value: 'V5' } })
  fireEvent.change(screen.getByLabelText('느낌'), { target: { value: '슬로퍼가 어려움' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await waitFor(() => expect(writes).toHaveLength(1))
  expect(writes[0]).toMatchObject({ expectedTemplateVersion: 1, detail: { climbing: { rounds: [{ grade: 'V5' }], fields: [{ fieldId, type: 'MEMO', memoValue: '슬로퍼가 어려움' }] } } })
})
