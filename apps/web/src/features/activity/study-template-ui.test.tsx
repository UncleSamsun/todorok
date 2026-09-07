import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { SessionClient } from '@todorok/api-client'
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
