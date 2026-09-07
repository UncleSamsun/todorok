import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { activity, planner, SessionClient } from '@todorok/api-client'
import { QuickAdd } from '../today/QuickAdd'
import { StudyTemplateFields } from './StudyTemplateFields'
import { QueryClient } from '@tanstack/react-query'
import { App } from '../../App'

afterEach(() => {
  cleanup()
  localStorage.clear()
  history.replaceState({}, '', '/')
})

const template = (overrides: Record<string, unknown> = {}) => ({
  templateId: '10000000-0000-0000-0000-000000000001',
  domain: 'STUDY',
  kind: 'STUDY_CATEGORY',
  archived: false,
  revision: 0,
  currentVersion: {
    templateId: '10000000-0000-0000-0000-000000000001',
    templateVersion: 1,
    name: '알고리즘',
    fields: [
      { fieldId: '20000000-0000-0000-0000-000000000001', name: '문제 수', type: 'NUMBER', unit: '개', position: 0 },
      { fieldId: '20000000-0000-0000-0000-000000000002', name: '복습 시간', type: 'TIME', unit: '분', position: 1 },
      { fieldId: '20000000-0000-0000-0000-000000000003', name: '주제', type: 'SHORT_TEXT', position: 2 },
      { fieldId: '20000000-0000-0000-0000-000000000004', name: '복습 완료', type: 'CHECK', position: 3 },
      { fieldId: '20000000-0000-0000-0000-000000000005', name: '정리', type: 'MEMO', position: 4 },
    ],
  },
  ...overrides,
})

it('review: NUMBER accepts negative decimals and TIME displays minutes while sending seconds', () => {
  const change = vi.fn()
  render(<StudyTemplateFields definitions={template().currentVersion.fields as activity.FieldDefinition[]} value={{ fields: [{ fieldId: template().currentVersion.fields[0]!.fieldId, type: activity.TemplateFieldType.Number, numberValue: -1.5 }, { fieldId: template().currentVersion.fields[1]!.fieldId, type: activity.TemplateFieldType.Time, timeSeconds: 90 }] }} change={change} />)
  const number = screen.getByLabelText('문제 수 (개)') as HTMLInputElement
  fireEvent.change(number, { target: { value: '-1.5' } })
  expect(number.checkValidity()).toBe(true)
  const time = screen.getByLabelText('복습 시간 (분)')
  expect(time).toHaveValue(1.5)
  fireEvent.change(time, { target: { value: '2.5' } })
  expect(change.mock.lastCall?.[0].fields).toContainEqual({ fieldId: template().currentVersion.fields[1]!.fieldId, type: 'TIME', timeSeconds: 150 })
})

it('review: manager revision conflict compares latest without overwriting the draft', async () => {
  const writes: any[] = [], original = template(), latest = template({ revision: 2, currentVersion: { ...template().currentVersion, templateVersion: 3, name: '다른 사용 창의 정의' } })
  render(<App session={new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.includes('/templates?')) return Response.json({ items: [original] })
    if (path.endsWith(`/templates/${original.templateId}`)) return Response.json(latest)
    if (path.endsWith('/versions') && init?.method === 'POST') { writes.push(JSON.parse(String(init.body))); return Response.json({ code: 'TEMPLATE_VERSION_CONFLICT', retryable: false }, { status: 409 }) }
    return common(path) ?? Response.json({}, { status: 404 })
  } })} />)
  await waitFor(() => expect(location.pathname).toBe('/today'))
  fireEvent.click(await screen.findByRole('link', { name: '공부' }))
  fireEvent.click(await screen.findByRole('button', { name: '카테고리 관리' }))
  fireEvent.click(await screen.findByRole('button', { name: '알고리즘 수정' }))
  fireEvent.change(screen.getByLabelText('카테고리 이름'), { target: { value: '보존할 초안 이름' } })
  fireEvent.click(screen.getByRole('button', { name: '변경 저장' }))
  fireEvent.click(await screen.findByRole('button', { name: '최신 정의 확인 후 초안 유지' }))
  expect(screen.getByLabelText('카테고리 이름')).toHaveValue('보존할 초안 이름')
  expect(screen.getByLabelText('카테고리 이름')).toBeEnabled()
  fireEvent.click(screen.getByRole('button', { name: '변경 저장' }))
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[1].expectedRevision).toBe(2)
  expect(writes[1].name).toBe('보존할 초안 이름')
  expect(writes[1].commandId).not.toBe(writes[0].commandId)
})

it('review: uncertain record retries its original version and all values after a template refresh', async () => {
  const writes: any[] = [], queries = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const session = new SessionClient({ fetcher: async (url, init) => {
    if (String(url).endsWith('/activities') && init?.method === 'POST') { writes.push(JSON.parse(String(init.body))); return Response.json({ code: 'UPSTREAM_UNAVAILABLE', retryable: true }, { status: 503 }) }
    return recordRead(String(url))
  } })
  render(<App session={session} queryClient={queries} />)
  await waitFor(() => expect(location.pathname).toBe('/today'))
  await waitFor(() => expect(screen.getByRole('button', { name: '공부 추가' })).toBeEnabled())
  await act(async () => { history.pushState({}, '', '/study?taskId=task-1'); dispatchEvent(new PopStateEvent('popstate')) })
  fireEvent.change(await screen.findByLabelText('정리'), { target: { value: '원래 메모' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await screen.findByRole('button', { name: '같은 요청 다시 보내기' })
  await act(async () => { queries.setQueryData(['record-template', 'owner', 'task-1'], { linked: true, template: template({ currentVersion: { ...template().currentVersion, templateVersion: 2, fields: [] } }) }) })
  expect(screen.getByLabelText('정리')).toHaveValue('원래 메모')
  expect(await screen.findByRole('button', { name: '변경 확인 후 최신 항목 적용' })).toBeDisabled()
  fireEvent.click(screen.getByRole('button', { name: '같은 요청 다시 보내기' }))
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[1]).toEqual(writes[0])
  expect(writes[1].expectedTemplateVersion).toBe(1)
})

it('review: uncertain management response keeps the exact version request frozen', async () => {
  const writes: any[] = []
  render(<App session={new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.includes('/templates?')) return Response.json({ items: [template()] })
    if (path.endsWith('/versions') && init?.method === 'POST') { writes.push(JSON.parse(String(init.body))); return Response.json({ code: 'UPSTREAM_UNAVAILABLE', retryable: true }, { status: 503 }) }
    return common(path) ?? Response.json({}, { status: 404 })
  } })} />)
  await waitFor(() => expect(location.pathname).toBe('/today'))
  fireEvent.click(await screen.findByRole('link', { name: '공부' }))
  fireEvent.click(await screen.findByRole('button', { name: '카테고리 관리' }))
  fireEvent.click(await screen.findByRole('button', { name: '알고리즘 수정' }))
  fireEvent.change(screen.getByLabelText('카테고리 이름'), { target: { value: '결과 미확인 이름' } })
  fireEvent.click(screen.getByRole('button', { name: '변경 저장' }))
  await screen.findByRole('button', { name: '같은 요청 다시 보내기' })
  expect(screen.getByLabelText('카테고리 이름')).toBeDisabled()
  fireEvent.click(screen.getByRole('button', { name: '같은 요청 다시 보내기' }))
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[1]).toEqual(writes[0])
  expect(writes[0].expectedRevision).toBe(0)
})

it.each([false, true])('review: picker cursor and confirmed template conflict use a new Task/series command (series=%s)', async (series) => {
  let version = 1
  const writes: any[] = []
  const later = () => template({ templateId: 'later', currentVersion: { ...template().currentVersion, templateVersion: version, name: '21번째' } })
  render(<App session={new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.includes('/templates?')) return Response.json(path.includes('cursor=page2') ? { items: [later()] } : { items: [template()], nextCursor: 'page2' })
    if (/\/(tasks|series)$/.test(path) && init?.method === 'POST') { writes.push(JSON.parse(String(init.body))); version = 2; return Response.json({ code: 'TEMPLATE_VERSION_CONFLICT', retryable: false }, { status: 409 }) }
    return common(path) ?? Response.json({}, { status: 404 })
  } })} />)
  const add = await screen.findByRole('button', { name: '공부 추가' }); await waitFor(() => expect(add).toBeEnabled()); fireEvent.click(add)
  fireEvent.click(await screen.findByRole('button', { name: '카테고리 더 보기' }))
  await screen.findByRole('option', { name: '21번째' })
  fireEvent.change(screen.getByLabelText('공부 카테고리'), { target: { value: 'later' } })
  fireEvent.change(screen.getByLabelText('제목'), { target: { value: '그대로인 일정 초안' } })
  if (series) fireEvent.change(screen.getByLabelText('반복'), { target: { value: 'DAILY' } })
  fireEvent.click(screen.getByRole('button', { name: '저장' }))
  const confirm = await screen.findByRole('button', { name: '최신 카테고리 확인' }); await waitFor(() => expect(confirm).toBeEnabled())
  expect(screen.getByLabelText('제목')).toBeEnabled()
  fireEvent.click(confirm)
  fireEvent.click(screen.getByRole('button', { name: '저장' }))
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[1].templateSelection.expectedTemplateVersion).toBe(2)
  expect(writes[1].commandId).not.toBe(writes[0].commandId)
  expect(writes[1].title).toBe(writes[0].title)
  if (series) expect(writes[1].rule).toEqual(writes[0].rule)
})

it.each([400, 413])('review: correction rejection %s unlocks fields and uses the edited content', async (status) => {
  const writes: any[] = []
  const base = { activityId: 'activity-1', commandId: 'command-1', taskId: 'task-1', userId: 'owner', activityType: 'STUDY', performedAt: '2026-09-06T01:00:00.123456Z', detail: { study: { fields: [] } }, detailFormat: 'TEMPLATE', templateSnapshot: { ...template().currentVersion, domain: 'STUDY', kind: 'STUDY_CATEGORY', schemaVersion: 1 }, status: 'COMPLETED', version: 4, syncState: 'APPLIED' }
  render(<App session={new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.endsWith('/activities/activity-1')) {
      if (init?.method === 'PATCH') { writes.push(JSON.parse(String(init.body))); return Response.json({ code: status === 400 ? 'FIELD_VALUE_INVALID' : 'PAYLOAD_TOO_LARGE', retryable: false }, { status }) }
      return Response.json(base)
    }
    return common(path) ?? Response.json({}, { status: 404 })
  } })} />)
  await waitFor(() => expect(location.pathname).toBe('/today'))
  await waitFor(() => expect(screen.getByRole('button', { name: '공부 추가' })).toBeEnabled())
  await act(async () => { history.pushState({}, '', '/study?activityId=activity-1'); dispatchEvent(new PopStateEvent('popstate')) })
  fireEvent.change(await screen.findByLabelText('문제 수 (개)'), { target: { value: '-1.5' } })
  fireEvent.click(screen.getByRole('button', { name: '수정 저장' }))
  await screen.findByRole('alert')
  expect(screen.getByLabelText('문제 수 (개)')).toBeEnabled()
  fireEvent.change(screen.getByLabelText('문제 수 (개)'), { target: { value: '2.5' } })
  fireEvent.click(screen.getByRole('button', { name: '수정 저장' }))
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[1].detail.study.fields[0].numberValue).toBe(2.5)
  expect(writes[1].performedAt).toBe(base.performedAt)
})

it.each([false, true])('review: QuickAdd freezes the request across refresh and archive (series=%s)', (series) => {
  const save = vi.fn(), original = template() as activity.TemplateResponse
  const props = { date: '2026-09-07', type: planner.TaskType.Study, busy: false, error: '', save, cancel: vi.fn() }
  const view = render(<QuickAdd {...props} templates={[original]} />)
  fireEvent.change(screen.getByLabelText('제목'), { target: { value: '고정 요청' } })
  fireEvent.change(screen.getByLabelText('공부 카테고리'), { target: { value: original.templateId } })
  if (series) fireEvent.change(screen.getByLabelText('반복'), { target: { value: 'DAILY' } })
  fireEvent.click(screen.getByRole('button', { name: '저장' }))
  const first = structuredClone(save.mock.calls[0])
  fireEvent.click(screen.getByRole('button', { name: '저장' }))
  expect(save.mock.calls[1]).toEqual(first)
  view.rerender(<QuickAdd {...props} uncertain templates={[{ ...original, currentVersion: { ...original.currentVersion, templateVersion: 2 } }]} />)
  fireEvent.click(screen.getByRole('button', { name: '같은 요청 다시 보내기' }))
  expect(save.mock.calls[2]).toEqual(first)
  view.rerender(<QuickAdd {...props} uncertain templates={[]} />)
  expect(screen.getByRole('button', { name: '같은 요청 다시 보내기' })).toBeEnabled()
  fireEvent.click(screen.getByRole('button', { name: '같은 요청 다시 보내기' }))
  expect(save.mock.calls[3]).toEqual(first)
})

async function openStudyRecord(fetcher: typeof fetch) {
  render(<App session={new SessionClient({ fetcher })} />)
  await waitFor(() => expect(location.pathname).toBe('/today'))
  await waitFor(() => expect(screen.getByRole('button', { name: '공부 추가' })).toBeEnabled())
  await act(async () => { history.pushState({}, '', '/study?taskId=task-1'); dispatchEvent(new PopStateEvent('popstate')) })
  await screen.findByLabelText('문제 수 (개)')
}
const studyTask = { taskId: 'task-1', userId: 'owner', title: 'DP 연습', taskType: 'STUDY', scheduledDate: '2026-09-07', status: 'PLANNED', version: 0 }
function recordRead(path: string, current = template()) {
  if (path.endsWith('/tasks/task-1')) return Response.json(studyTask)
  if (path.endsWith('/record-template')) return Response.json({ linked: true, template: current })
  return common(path) ?? Response.json({}, { status: 404 })
}

it('review: definition conflict preserves removed and unit-changed inputs until explicit comparison', async () => {
  const writes: any[] = []; let current = template()
  let saved: Record<string, unknown> | null = null
  await openStudyRecord(async (url, init) => {
    const path = String(url)
    if (path.endsWith('/activities/retained-record')) return Response.json(saved)
    if (path.endsWith('/activities') && init?.method === 'POST') {
      writes.push(JSON.parse(String(init.body)))
      if (writes.length === 2) {
        saved = { activityId: 'retained-record', commandId: writes[1].commandId, taskId: 'task-1', userId: 'owner', activityType: 'STUDY', performedAt: writes[1].performedAt, detail: writes[1].detail, detailFormat: 'TEMPLATE', templateSnapshot: { ...current.currentVersion, schemaVersion: 1, domain: 'STUDY', kind: 'STUDY_CATEGORY' }, status: 'COMPLETED', version: 0, syncState: 'APPLIED' }
        return Response.json(saved, { status: 201 })
      }
      current = template({ currentVersion: { ...current.currentVersion, templateVersion: 2, fields: current.currentVersion.fields.filter((field) => field.type !== 'MEMO').map((field) => field.type === 'NUMBER' ? { ...field, unit: '문항' } : field) } })
      return Response.json({ code: 'TEMPLATE_VERSION_CONFLICT', retryable: false }, { status: 409 })
    }
    return recordRead(path, current)
  })
  fireEvent.change(screen.getByLabelText('문제 수 (개)'), { target: { value: '7' } })
  fireEvent.change(screen.getByLabelText('정리'), { target: { value: '없애면 안 되는 메모' } })
  fireEvent.change(screen.getByLabelText('복습 완료'), { target: { value: 'false' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await screen.findByRole('button', { name: '변경 확인 후 최신 항목 적용' })
  expect(screen.getByLabelText('정리')).toHaveValue('없애면 안 되는 메모')
  expect(screen.getByLabelText('문제 수 (개)')).toHaveValue(7)
  expect(screen.getByRole('button', { name: '기록 저장' })).toBeDisabled()
  fireEvent.click(await screen.findByRole('button', { name: '변경 확인 후 최신 항목 적용' }))
  expect(screen.getByLabelText('문제 수 (문항)')).toHaveValue(null)
  expect(screen.getByLabelText('복습 완료')).toHaveValue('false')
  expect(screen.getAllByText(/없애면 안 되는 메모/).length).toBeGreaterThan(0)
  fireEvent.change(screen.getByLabelText('주제'), { target: { value: 'BFS' } })
  expect(screen.getByText('없애면 안 되는 메모')).toBeVisible()
  fireEvent.change(screen.getByLabelText('주제'), { target: { value: '' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[1].commandId).not.toBe(writes[0].commandId)
  expect(writes[1].expectedTemplateVersion).toBe(2)
  expect(writes[1].detail.study.fields).toEqual([{ fieldId: template().currentVersion.fields[3]!.fieldId, type: 'CHECK', checked: false }])
  await waitFor(() => expect(location.pathname).toBe('/today'))
  await act(async () => { history.pushState({}, '', '/study?activityId=retained-record'); dispatchEvent(new PopStateEvent('popstate')) })
  await screen.findByRole('button', { name: '수정 저장' })
  expect(screen.getByText('없애면 안 되는 메모')).toBeVisible()
  expect(screen.getByText('문제 수 · 숫자 · 개')).toBeVisible()
})

it.each([[400, 'FIELD_VALUE_INVALID'], [413, 'PAYLOAD_TOO_LARGE']])('review: confirmed record error %s preserves editable draft', async (status, code) => {
  const writes: any[] = []
  await openStudyRecord(async (url, init) => {
    if (String(url).endsWith('/activities') && init?.method === 'POST') { writes.push(JSON.parse(String(init.body))); return Response.json({ code, retryable: false }, { status: Number(status) }) }
    return recordRead(String(url))
  })
  fireEvent.change(screen.getByLabelText('문제 수 (개)'), { target: { value: '1' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await screen.findByRole('alert')
  expect(screen.getByLabelText('문제 수 (개)')).toBeEnabled()
  fireEvent.change(screen.getByLabelText('문제 수 (개)'), { target: { value: '2' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[1].commandId).not.toBe(writes[0].commandId)
  expect(writes[1].detail.study.fields[0].numberValue).toBe(2)
})

it('review: manager follows cursor and creates a new identity when changing saved field type', async () => {
  const writes: any[] = [], cursors: string[] = []
  const later = template({ templateId: 'later', currentVersion: { ...template().currentVersion, name: '21번째 카테고리' } })
  history.replaceState({}, '', '/study')
  render(<App session={new SessionClient({ fetcher: async (url, init) => {
    const path = String(url)
    if (path.includes('/templates?')) { cursors.push(path); return Response.json(path.includes('cursor=page2') ? { items: [later] } : { items: [template()], nextCursor: 'page2' }) }
    if (path.endsWith('/versions') && init?.method === 'POST') { writes.push(JSON.parse(String(init.body))); return Response.json({ code: 'VALIDATION_FAILED', retryable: false }, { status: 400 }) }
    return common(path) ?? Response.json({}, { status: 404 })
  } })} />)
  await waitFor(() => expect(location.pathname).toBe('/today'))
  fireEvent.click(await screen.findByRole('link', { name: '공부' }))
  fireEvent.click(await screen.findByRole('button', { name: '카테고리 관리' }))
  fireEvent.click(await screen.findByRole('button', { name: '카테고리 더 보기' }))
  fireEvent.click(await screen.findByRole('button', { name: '21번째 카테고리 수정' }))
  fireEvent.change(screen.getAllByLabelText('형식')[0]!, { target: { value: 'TIME' } })
  fireEvent.click(screen.getByRole('button', { name: '변경 저장' }))
  await screen.findByRole('alert')
  expect(writes[0].fields[0].fieldId).not.toBe(template().currentVersion.fields[0]!.fieldId)
  expect(writes[0].fields[0].unit).toBe('초')
  expect(screen.getByLabelText('카테고리 이름')).toBeEnabled()
  expect(cursors.some((url) => url.includes('cursor=page2'))).toBe(true)
})

function common(path: string) {
  if (path.endsWith('/refresh')) return Response.json({ accessToken: 'token', userId: 'owner', expiresAt: '2099-01-01T00:00:00Z' })
  if (path.endsWith('/rollover')) return Response.json({ today: '2026-09-07', movedCount: 0 })
  if (path.includes('/calendar?')) return Response.json({ from: '2026-09-01', to: '2026-09-30', days: [] })
  if (path.includes('/calendar/')) return Response.json({ date: '2026-09-07', tasks: [] })
  if (path.includes('/notes/')) return Response.json({ date: '2026-09-07', content: '', version: null })
  if (path.includes('/activities/summary')) return Response.json({ month: '2026-09', activityType: 'STUDY', completedCount: 0, durationSeconds: 0 })
  if (path.includes('/activities?')) return Response.json({ items: [] })
  return null
}

it('creates, versions, and archives study categories without creating a task', async () => {
  let current = template()
  const writes: { path: string; body: any }[] = []
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url), fallback = common(path)
    if (path.includes('/templates?')) return Response.json({ items: [current] })
    if (path.endsWith('/templates') && init?.method === 'POST') {
      const body = JSON.parse(String(init.body)); writes.push({ path, body }); return Response.json(current)
    }
    if (path.endsWith(`/templates/${current.templateId}/versions`) && init?.method === 'POST') {
      const body = JSON.parse(String(init.body)); writes.push({ path, body })
      current = template({ revision: 1, currentVersion: { ...current.currentVersion, templateVersion: 2, name: body.name, fields: body.fields.map((field: any, position: number) => ({ ...field, position })) } })
      return Response.json(current)
    }
    if (path.endsWith(`/templates/${current.templateId}/archive`) && init?.method === 'POST') {
      const body = JSON.parse(String(init.body)); writes.push({ path, body }); current = { ...current, archived: true, revision: 2 }; return Response.json(current)
    }
    return fallback ?? Response.json({}, { status: 404 })
  } })
  render(<App session={session} />)
  await waitFor(() => expect(location.pathname).toBe('/today'))
  fireEvent.click(await screen.findByRole('link', { name: '공부' }))
  fireEvent.click(await screen.findByRole('button', { name: '카테고리 관리' }))
  fireEvent.click(await screen.findByRole('button', { name: '알고리즘 수정' }))
  fireEvent.change(screen.getByLabelText('카테고리 이름'), { target: { value: '알고리즘 심화' } })
  fireEvent.change(screen.getByLabelText('문제 수 단위'), { target: { value: '문항' } })
  fireEvent.click(screen.getByRole('button', { name: '정리 위로 이동' }))
  fireEvent.click(screen.getByRole('button', { name: '변경 저장' }))
  await waitFor(() => expect(writes).toHaveLength(1))
  expect(writes[0].body).toMatchObject({ expectedRevision: 0, name: '알고리즘 심화' })
  expect(writes[0].body.fields.at(-2).name).toBe('정리')
  expect(writes[0].body.fields[0].unit).toBe('문항')
  fireEvent.click(await screen.findByRole('button', { name: '알고리즘 심화 보관' }))
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[1].body.expectedRevision).toBe(1)
  expect(writes.some((write) => /\/tasks$|\/series$/.test(write.path))).toBe(false)
})

it('keeps the study quick-add draft and command when a save result is uncertain', async () => {
  const writes: any[] = []
  let attempts = 0
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url), fallback = common(path)
    if (path.includes('/templates?')) return Response.json({ items: [template()] })
    if (path.endsWith('/tasks') && init?.method === 'POST') {
      const body = JSON.parse(String(init.body)); writes.push(body); attempts++
      if (attempts === 1) return Response.json({ code: 'UPSTREAM_UNAVAILABLE', retryable: true }, { status: 503 })
      return Response.json({ taskId: 'task-1', userId: 'owner', title: body.title, taskType: 'STUDY', scheduledDate: body.scheduledDate, status: 'PLANNED', version: 0 })
    }
    return fallback ?? Response.json({}, { status: 404 })
  } })
  render(<App session={session} />)
  await waitFor(() => expect(location.pathname).toBe('/today'))
  const add = await screen.findByRole('button', { name: '공부 추가' })
  await waitFor(() => expect(add).toBeEnabled())
  fireEvent.click(add)
  await screen.findByRole('option', { name: '알고리즘' })
  fireEvent.change(screen.getByLabelText('제목'), { target: { value: 'DP 연습' } })
  fireEvent.click(screen.getByRole('button', { name: '카테고리 관리' }))
  expect(await screen.findByRole('heading', { name: '공부 카테고리 관리' })).toBeVisible()
  fireEvent.click(screen.getByRole('button', { name: '공부로 돌아가기' }))
  expect(screen.getByLabelText('제목')).toHaveValue('DP 연습')
  fireEvent.change(screen.getByLabelText('공부 카테고리'), { target: { value: template().templateId } })
  expect(screen.getByText(/문제 수 · 숫자 · 개/)).toBeVisible()
  fireEvent.click(screen.getByRole('button', { name: '저장' }))
  expect(await screen.findByRole('alert')).toBeVisible()
  expect(screen.getByLabelText('제목')).toHaveValue('DP 연습')
  fireEvent.click(screen.getByRole('button', { name: '같은 요청 다시 보내기' }))
  await waitFor(() => expect(writes).toHaveLength(2))
  expect(writes[1]).toEqual(writes[0])
  expect(writes[0].templateSelection).toEqual({ templateId: template().templateId, expectedTemplateVersion: 1 })
  expect(writes[0].commandId).toMatch(/^[0-9a-f-]{36}$/)
})

it('loads the current task template and submits only entered values while preserving zero and false', async () => {
  const writes: any[] = []
  const task = { taskId: 'task-1', userId: 'owner', title: 'DP 연습', taskType: 'STUDY', scheduledDate: '2026-09-07', status: 'PLANNED', version: 0 }
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url), fallback = common(path)
    if (path.endsWith('/tasks/task-1')) return Response.json(task)
    if (path.endsWith('/tasks/task-1/record-template')) return Response.json({ linked: true, templateLink: { bindingId: 'binding-1', templateId: template().templateId, selectedTemplateVersion: 1, name: '알고리즘', fieldSummary: '5개 항목' }, template: template() })
    if (path.endsWith('/activities') && init?.method === 'POST') {
      const body = JSON.parse(String(init.body)); writes.push(body)
      return Response.json({ activityId: 'activity-1', commandId: body.commandId, taskId: 'task-1', userId: 'owner', activityType: 'STUDY', performedAt: body.performedAt, detail: body.detail, detailFormat: 'TEMPLATE', templateSnapshot: { schemaVersion: 1, templateId: template().templateId, templateVersion: 1, name: '알고리즘', domain: 'STUDY', kind: 'STUDY_CATEGORY', fields: template().currentVersion.fields }, status: 'COMPLETED', version: 0, syncState: 'APPLIED' })
    }
    return fallback ?? Response.json({}, { status: 404 })
  } })
  render(<App session={session} />)
  await waitFor(() => expect(location.pathname).toBe('/today'))
  await waitFor(() => expect(screen.getByRole('button', { name: '공부 추가' })).toBeEnabled())
  await act(async () => { history.pushState({}, '', '/study?taskId=task-1'); dispatchEvent(new PopStateEvent('popstate')) })
  expect(await screen.findByRole('heading', { name: '알고리즘 기록' })).toBeVisible()
  fireEvent.change(screen.getByLabelText('문제 수 (개)'), { target: { value: '0' } })
  fireEvent.change(screen.getByLabelText('복습 완료'), { target: { value: 'false' } })
  fireEvent.change(screen.getByLabelText('정리'), { target: { value: '  첫 줄\n둘째 줄  ' } })
  fireEvent.click(screen.getByRole('button', { name: '기록 저장' }))
  await waitFor(() => expect(writes).toHaveLength(1))
  expect(writes[0].expectedTemplateVersion).toBe(1)
  expect(writes[0].detail.study.fields).toEqual([
    { fieldId: '20000000-0000-0000-0000-000000000001', type: 'NUMBER', numberValue: 0 },
    { fieldId: '20000000-0000-0000-0000-000000000004', type: 'CHECK', checked: false },
    { fieldId: '20000000-0000-0000-0000-000000000005', type: 'MEMO', memoValue: '  첫 줄\n둘째 줄  ' },
  ])
})

it('edits template records from their original snapshot and displays legacy JSON without rounding it', async () => {
  const writes: any[] = []
  const snapshot = { schemaVersion: 1, templateId: template().templateId, templateVersion: 1, name: '기록 당시 알고리즘', domain: 'STUDY', kind: 'STUDY_CATEGORY', fields: template().currentVersion.fields }
  const base = { activityId: 'activity-1', commandId: 'command-1', taskId: 'task-1', userId: 'owner', activityType: 'STUDY', performedAt: '2026-09-06T01:00:00.123456Z', startedAt: '2026-09-06T01:00:59.123456Z', endedAt: '2026-09-06T01:01:01.456789Z', detail: { study: { fields: [{ fieldId: '20000000-0000-0000-0000-000000000001', type: 'NUMBER', numberValue: 3 }] } }, detailFormat: 'TEMPLATE', templateSnapshot: snapshot, status: 'COMPLETED', version: 4, syncState: 'APPLIED' }
  let responseText = JSON.stringify(base)
  const session = new SessionClient({ fetcher: async (url, init) => {
    const path = String(url), fallback = common(path)
    if (path.endsWith('/activities/activity-1') && init?.method === 'PATCH') { const body = JSON.parse(String(init.body)); writes.push(body); return new Response(JSON.stringify({ ...base, ...body, version: 5 }), { headers: { 'content-type': 'application/json' } }) }
    if (path.endsWith('/activities/activity-1')) return new Response(responseText, { headers: { 'content-type': 'application/json' } })
    return fallback ?? Response.json({}, { status: 404 })
  } })
  render(<App session={session} />)
  await waitFor(() => expect(location.pathname).toBe('/today'))
  await waitFor(() => expect(screen.getByRole('button', { name: '공부 추가' })).toBeEnabled())
  await act(async () => { history.pushState({}, '', '/study?activityId=activity-1'); dispatchEvent(new PopStateEvent('popstate')) })
  expect(await screen.findByRole('heading', { name: '기록 당시 알고리즘' })).toBeVisible()
  expect(screen.getByLabelText('문제 수 (개)')).toHaveValue(3)
  fireEvent.change(screen.getByLabelText('문제 수 (개)'), { target: { value: '4' } })
  fireEvent.click(screen.getByRole('button', { name: '수정 저장' }))
  await waitFor(() => expect(writes).toHaveLength(1))
  expect(writes[0].detail.study.fields[0].numberValue).toBe(4)
  expect(writes[0]).not.toHaveProperty('expectedTemplateVersion')
  expect(writes[0]).toMatchObject({ performedAt: base.performedAt, startedAt: base.startedAt, endedAt: base.endedAt })

  cleanup()
  responseText = '{"activityId":"legacy-1","commandId":"legacy-command","taskId":"task-2","userId":"owner","activityType":"STUDY","performedAt":"2026-09-06T01:00:00.123456Z","detail":{"study":{"subject":"수학"}},"detailFormat":"LEGACY","legacyStudyPayload":{"provenance":"UNVERIFIED_LEGACY","values":{"precise":12345678901234567890.123456789},"snapshot":null},"status":"COMPLETED","version":2,"syncState":"APPLIED"}'
  history.replaceState({}, '', '/study?activityId=legacy-1')
  const legacySession = new SessionClient({ fetcher: async (url) => {
    const path = String(url), fallback = common(path)
    if (path.endsWith('/activities/legacy-1')) return new Response(responseText, { headers: { 'content-type': 'application/json' } })
    return fallback ?? Response.json({}, { status: 404 })
  } })
  render(<App session={legacySession} />)
  expect(await screen.findByText(/12345678901234567890\.123456789/)).toBeVisible()
  expect(screen.getByText(/읽기 전용/)).toBeVisible()
})
