import { useMemo, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { activity } from '@todorok/api-client'
import { useAuth } from '../auth/AuthProvider'
import { studyFieldSummary } from '../activity/StudyTemplateFields'
import { requestFailure } from '../activity/requestFailure'
import { useRecordTemplates } from './useStudyTemplates'

type DraftField = activity.FieldDefinitionInput
const typeOptions: [activity.TemplateFieldType, string][] = [
  [activity.TemplateFieldType.Number, '숫자'],
  [activity.TemplateFieldType.Time, '시간'],
  [activity.TemplateFieldType.ShortText, '짧은 글'],
  [activity.TemplateFieldType.Check, '체크'],
  [activity.TemplateFieldType.Memo, '긴 메모'],
]

export function StudyTemplateManager({ close, domain = activity.TemplateDomain.Study, kind = activity.TemplateKind.StudyCategory, section = '공부', noun = '카테고리' }: { close: () => void; domain?: activity.TemplateDomain; kind?: activity.TemplateKind; section?: string; noun?: string }) {
  const { session, state } = useAuth(), queries = useQueryClient()
  const api = useMemo(() => new activity.TemplateApi(new activity.Configuration({ basePath: '/api/activity/v1', fetchApi: session.fetch })), [session])
  const list = useRecordTemplates(api, state.userId, domain, kind, true)
  const [editing, setEditing] = useState<activity.TemplateResponse | 'new' | null>(null)
  const [name, setName] = useState(''), [fields, setFields] = useState<DraftField[]>([])
  const [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false), [error, setError] = useState('')
  const command = useRef('')
  const request = useRef<(() => Promise<unknown>) | null>(null)
  const [conflict, setConflict] = useState(false), [latest, setLatest] = useState<activity.TemplateResponse | null>(null)
  const latestTarget = useRef<string | null>(null)
  const [archiveConflict, setArchiveConflict] = useState<activity.TemplateResponse | null>(null)
  const archiveCommands = useRef(new Map<string, activity.ArchiveTemplateRequest>())
  function begin(value: activity.TemplateResponse | 'new') {
    setEditing(value)
    setName(value === 'new' ? '' : value.currentVersion.name)
    setFields(value === 'new' ? [] : value.currentVersion.fields.map(({ fieldId, name, type, unit }) => ({ fieldId, name, type, ...(unit ? { unit } : {}) })))
    setError('')
    setUncertain(false)
    setConflict(false); setLatest(null); latestTarget.current = null; request.current = null
    command.current = crypto.randomUUID()
  }
  function update(index: number, patch: Partial<DraftField>) { setFields((current) => current.map((field, position) => position === index ? { ...field, ...patch, ...(patch.type && patch.type !== field.type ? { fieldId: editing && editing !== 'new' && editing.currentVersion.fields.some((saved) => saved.fieldId === field.fieldId) ? crypto.randomUUID() : field.fieldId, unit: patch.type === 'TIME' ? '초' : undefined } : {}) } : field)) }
  function move(index: number, offset: number) { setFields((current) => { const next = [...current], target = index + offset; if (target < 0 || target >= next.length) return current; [next[index], next[target]] = [next[target]!, next[index]!]; return next }) }
  async function save() {
    if (busy || conflict || !name.trim() || fields.some((field) => !field.name.trim())) return
    setBusy(true); setError('')
    try {
      if (!uncertain || !request.current) {
        const body = structuredClone({ commandId: command.current, name: name.trim(), fields: fields.map((field) => ({ ...field, name: field.name.trim(), unit: field.unit?.trim() || undefined })) })
        if (editing === 'new') { const createTemplateRequest = { ...body, domain, kind }; request.current = () => api.createTemplate({ createTemplateRequest }) }
        else if (editing) { const templateId = editing.templateId, createTemplateVersionRequest = { ...body, expectedRevision: editing.revision }; request.current = () => api.createTemplateVersion({ templateId, createTemplateVersionRequest }) }
      }
      await request.current?.()
      setEditing(null)
      await queries.invalidateQueries({ queryKey: ['templates', state.userId] })
    } catch (reason) {
      const failure = await requestFailure(reason)
      setUncertain(!failure.rejected)
      if (failure.rejected) { request.current = null; command.current = crypto.randomUUID() }
      if (failure.status === 409 && editing && editing !== 'new') { setConflict(true); await loadLatest(editing.templateId) }
      setError(failure.rejected ? `저장이 거절되었습니다. 작성 중인 내용을 확인해 주세요. (${failure.code ?? failure.status})` : '저장 결과를 확인하지 못했습니다. 작성 중인 내용과 원 요청은 보존됩니다.')
    }
    finally { setBusy(false) }
  }
  async function loadLatest(templateId: string) {
    latestTarget.current = templateId
    setLatest(null)
    try {
      const value = await api.getTemplate({ templateId })
      if (latestTarget.current === templateId && value.templateId === templateId) setLatest(value)
    } catch {
      if (latestTarget.current === templateId) setLatest(null)
      setError('최신 정의를 불러오지 못했습니다. 초안은 보존됩니다.')
    }
  }
  function rebase() {
    if (!latest || latest.archived || editing === 'new' || !editing || latestTarget.current !== editing.templateId || latest.templateId !== editing.templateId) return
    setFields((current) => current.map((field) => ({ ...field, fieldId: latest.currentVersion.fields.some((item) => item.fieldId === field.fieldId && item.type === field.type) ? field.fieldId : crypto.randomUUID() })))
    setEditing(latest); setConflict(false); setLatest(null); setError(''); command.current = crypto.randomUUID()
  }
  async function archive(value: activity.TemplateResponse) {
    if (busy) return
    setBusy(true); setError('')
    try {
      const archiveTemplateRequest = archiveCommands.current.get(value.templateId) ?? { commandId: crypto.randomUUID(), expectedRevision: value.revision }
      archiveCommands.current.set(value.templateId, archiveTemplateRequest)
      await api.archiveTemplate({ templateId: value.templateId, archiveTemplateRequest })
      archiveCommands.current.delete(value.templateId)
      setArchiveConflict(null); setLatest(null); latestTarget.current = null
      await queries.invalidateQueries({ queryKey: ['templates', state.userId] })
    } catch (reason) { const failure = await requestFailure(reason); if (failure.rejected) archiveCommands.current.delete(value.templateId); if (failure.status === 409) { setArchiveConflict(value); await loadLatest(value.templateId) }; setError(failure.rejected ? '보관이 거절되었습니다. 최신 정의를 확인해 주세요.' : '보관 결과를 확인하지 못했습니다. 같은 보관 요청으로 다시 확인해 주세요.') }
    finally { setBusy(false) }
  }
  if (editing) return <section className="template-manager"><header className="record-heading"><button aria-label={`${noun} 목록`} onClick={() => setEditing(null)}>‹</button><h1>{editing === 'new' ? `${section} ${noun} 만들기` : `${section} ${noun} 수정`}</h1></header>
    <form onSubmit={(event) => { event.preventDefault(); void save() }}><fieldset disabled={busy || uncertain}><label>{noun} 이름<input aria-label={`${noun} 이름`} value={name} maxLength={120} required onChange={(event) => setName(event.target.value)} /></label>
      <div className="template-fields">{fields.map((field, index) => <fieldset key={field.fieldId} className="template-field"><legend>{index + 1}번째 항목</legend><label>항목 이름<input value={field.name} maxLength={120} required onChange={(event) => update(index, { name: event.target.value })} /></label><label>형식<select value={field.type} onChange={(event) => { const type = event.target.value as activity.TemplateFieldType; update(index, { type, ...(type === activity.TemplateFieldType.Number || type === activity.TemplateFieldType.Time ? {} : { unit: undefined }) }) }}>{typeOptions.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>{(field.type === activity.TemplateFieldType.Number || field.type === activity.TemplateFieldType.Time) && <label>{field.name || '항목'} 단위<input aria-label={`${field.name || '항목'} 단위`} value={field.unit ?? ''} maxLength={40} onChange={(event) => update(index, { unit: event.target.value || undefined })} /></label>}<div className="field-actions"><button type="button" aria-label={`${field.name || '항목'} 위로 이동`} disabled={index === 0} onClick={() => move(index, -1)}>↑</button><button type="button" aria-label={`${field.name || '항목'} 아래로 이동`} disabled={index === fields.length - 1} onClick={() => move(index, 1)}>↓</button><button type="button" aria-label={`${field.name || '항목'} 삭제`} onClick={() => setFields((current) => current.filter((_, position) => position !== index))}>삭제</button></div></fieldset>)}</div>
      <button type="button" onClick={() => setFields((current) => [...current, { fieldId: crypto.randomUUID(), name: '', type: activity.TemplateFieldType.Number }])}>항목 추가</button></fieldset>{error && <p role="alert">{error}</p>}
      {conflict && <section><h2>최신 정의와 초안 비교</h2><p>작성 중인 초안은 위에 보존됩니다. 최신 정의를 확인한 뒤 현재 초안을 새 버전으로 저장할 수 있습니다.</p>{latest ? <><h3>{latest.currentVersion.name} · 버전 {latest.currentVersion.templateVersion}{latest.archived ? ' · 보관됨' : ''}</h3><ul>{latest.currentVersion.fields.map((field) => <li key={field.fieldId}>{studyFieldSummary(field)}</li>)}</ul><button type="button" disabled={busy || latest.archived} onClick={rebase}>최신 정의 확인 후 초안 유지</button></> : <button type="button" onClick={() => editing !== 'new' && void loadLatest(editing.templateId)}>최신 정의 다시 불러오기</button>}</section>}
      <div className="form-actions"><button type="button" disabled={busy} onClick={() => setEditing(null)}>취소</button><button disabled={busy || conflict || !name.trim()}>{busy ? '저장 중…' : uncertain ? '같은 요청 다시 보내기' : editing === 'new' ? `${noun} 저장` : '변경 저장'}</button></div></form>
  </section>
  const returnLabel = section === '운동' || section === '클라이밍' ? `${section}으로 돌아가기` : `${section}로 돌아가기`
  return <section className="template-manager"><header className="record-heading"><button aria-label={returnLabel} onClick={close}>‹</button><h1>{section} {noun} 관리</h1></header><button onClick={() => begin('new')}>새 {noun}</button>
    {list.isPending && <p role="status">{noun}를 불러오는 중…</p>}{list.isError && <p role="alert">{noun}를 불러오지 못했습니다. <button onClick={() => void list.refetch()}>다시 불러오기</button></p>}
    <div className="template-list">{list.data?.pages.flatMap((page) => page.items).map((value) => <article key={value.templateId}><div><strong>{value.currentVersion.name}</strong><small>버전 {value.currentVersion.templateVersion}{value.archived ? ' · 보관됨' : ''}</small><p>{value.currentVersion.fields.map(studyFieldSummary).join(', ') || '항목 없음'}</p></div>{!value.archived && <div className="form-actions"><button aria-label={`${value.currentVersion.name} 수정`} onClick={() => begin(value)}>수정</button><button aria-label={`${value.currentVersion.name} 보관`} disabled={busy || archiveConflict?.templateId === value.templateId} onClick={() => void archive(value)}>보관</button></div>}</article>)}</div>{list.hasNextPage && <button disabled={list.isFetchingNextPage} onClick={() => void list.fetchNextPage()}>{noun} 더 보기</button>}{error && <p role="alert">{error}</p>}
    {archiveConflict && <section><h2>보관 전 정의 비교</h2><p>이전: {archiveConflict.currentVersion.name} · 버전 {archiveConflict.currentVersion.templateVersion}</p><p>{archiveConflict.currentVersion.fields.map(studyFieldSummary).join(', ')}</p>{latest && latestTarget.current === archiveConflict.templateId && latest.templateId === archiveConflict.templateId ? <><p>최신: {latest.currentVersion.name} · 버전 {latest.currentVersion.templateVersion}{latest.archived ? ' · 보관됨' : ''}</p><p>{latest.currentVersion.fields.map(studyFieldSummary).join(', ')}</p>{!latest.archived && <button disabled={busy} onClick={() => void archive(latest)}>최신 정의 확인 후 보관</button>}</> : <button onClick={() => void loadLatest(archiveConflict.templateId)}>최신 정의 다시 불러오기</button>}</section>}
  </section>
}
