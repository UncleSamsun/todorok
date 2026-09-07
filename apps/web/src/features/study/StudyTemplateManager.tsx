import { useMemo, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { activity } from '@todorok/api-client'
import { useAuth } from '../auth/AuthProvider'
import { studyFieldSummary } from '../activity/StudyTemplateFields'

type DraftField = activity.FieldDefinitionInput
const typeOptions: [activity.TemplateFieldType, string][] = [
  [activity.TemplateFieldType.Number, '숫자'],
  [activity.TemplateFieldType.Time, '시간'],
  [activity.TemplateFieldType.ShortText, '짧은 글'],
  [activity.TemplateFieldType.Check, '체크'],
  [activity.TemplateFieldType.Memo, '긴 메모'],
]

export function StudyTemplateManager({ close }: { close: () => void }) {
  const { session, state } = useAuth(), queries = useQueryClient()
  const api = useMemo(() => new activity.TemplateApi(new activity.Configuration({ basePath: '/api/activity/v1', fetchApi: session.fetch })), [session])
  const key = ['templates', state.userId, 'STUDY', 'STUDY_CATEGORY', 'all']
  const list = useQuery({ queryKey: key, queryFn: ({ signal }) => api.listTemplates({ domain: activity.TemplateDomain.Study, kind: activity.TemplateKind.StudyCategory, includeArchived: true }, { signal }) })
  const [editing, setEditing] = useState<activity.TemplateResponse | 'new' | null>(null)
  const [name, setName] = useState(''), [fields, setFields] = useState<DraftField[]>([])
  const [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false), [error, setError] = useState('')
  const command = useRef('')
  const archiveCommands = useRef(new Map<string, string>())
  function begin(value: activity.TemplateResponse | 'new') {
    setEditing(value)
    setName(value === 'new' ? '' : value.currentVersion.name)
    setFields(value === 'new' ? [] : value.currentVersion.fields.map(({ fieldId, name, type, unit }) => ({ fieldId, name, type, ...(unit ? { unit } : {}) })))
    setError('')
    setUncertain(false)
    command.current = crypto.randomUUID()
  }
  function update(index: number, patch: Partial<DraftField>) { setFields((current) => current.map((field, position) => position === index ? { ...field, ...patch } : field)) }
  function move(index: number, offset: number) { setFields((current) => { const next = [...current], target = index + offset; if (target < 0 || target >= next.length) return current; [next[index], next[target]] = [next[target]!, next[index]!]; return next }) }
  async function save() {
    if (busy || !name.trim() || fields.some((field) => !field.name.trim())) return
    setBusy(true); setError('')
    try {
      if (editing === 'new') await api.createTemplate({ createTemplateRequest: { commandId: command.current, name: name.trim(), domain: activity.TemplateDomain.Study, kind: activity.TemplateKind.StudyCategory, fields: fields.map((field) => ({ ...field, name: field.name.trim(), ...(field.unit?.trim() ? { unit: field.unit.trim() } : { unit: undefined }) })) } })
      else if (editing) await api.createTemplateVersion({ templateId: editing.templateId, createTemplateVersionRequest: { commandId: command.current, expectedRevision: editing.revision, name: name.trim(), fields: fields.map((field) => ({ ...field, name: field.name.trim(), ...(field.unit?.trim() ? { unit: field.unit.trim() } : { unit: undefined }) })) } })
      setEditing(null)
      await queries.invalidateQueries({ queryKey: ['templates', state.userId] })
    } catch { setUncertain(true); setError('저장 결과를 확인하지 못했습니다. 작성 중인 내용과 원 요청은 보존됩니다.') }
    finally { setBusy(false) }
  }
  async function archive(value: activity.TemplateResponse) {
    if (busy) return
    setBusy(true); setError('')
    try {
      const commandId = archiveCommands.current.get(value.templateId) ?? crypto.randomUUID()
      archiveCommands.current.set(value.templateId, commandId)
      await api.archiveTemplate({ templateId: value.templateId, archiveTemplateRequest: { commandId, expectedRevision: value.revision } })
      archiveCommands.current.delete(value.templateId)
      await queries.invalidateQueries({ queryKey: ['templates', state.userId] })
    } catch { setError('카테고리를 보관하지 못했습니다.') }
    finally { setBusy(false) }
  }
  if (editing) return <section className="template-manager"><header className="record-heading"><button aria-label="카테고리 목록" onClick={() => setEditing(null)}>‹</button><h1>{editing === 'new' ? '공부 카테고리 만들기' : '공부 카테고리 수정'}</h1></header>
    <form onSubmit={(event) => { event.preventDefault(); void save() }}><fieldset disabled={busy || uncertain}><label>카테고리 이름<input value={name} maxLength={120} required onChange={(event) => setName(event.target.value)} /></label>
      <div className="template-fields">{fields.map((field, index) => <fieldset key={field.fieldId} className="template-field"><legend>{index + 1}번째 항목</legend><label>항목 이름<input value={field.name} maxLength={120} required onChange={(event) => update(index, { name: event.target.value })} /></label><label>형식<select value={field.type} onChange={(event) => { const type = event.target.value as activity.TemplateFieldType; update(index, { type, ...(type === activity.TemplateFieldType.Number || type === activity.TemplateFieldType.Time ? {} : { unit: undefined }) }) }}>{typeOptions.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>{(field.type === activity.TemplateFieldType.Number || field.type === activity.TemplateFieldType.Time) && <label>{field.name || '항목'} 단위<input aria-label={`${field.name || '항목'} 단위`} value={field.unit ?? ''} maxLength={40} onChange={(event) => update(index, { unit: event.target.value || undefined })} /></label>}<div className="field-actions"><button type="button" aria-label={`${field.name || '항목'} 위로 이동`} disabled={index === 0} onClick={() => move(index, -1)}>↑</button><button type="button" aria-label={`${field.name || '항목'} 아래로 이동`} disabled={index === fields.length - 1} onClick={() => move(index, 1)}>↓</button><button type="button" aria-label={`${field.name || '항목'} 삭제`} onClick={() => setFields((current) => current.filter((_, position) => position !== index))}>삭제</button></div></fieldset>)}</div>
      <button type="button" onClick={() => setFields((current) => [...current, { fieldId: crypto.randomUUID(), name: '', type: activity.TemplateFieldType.Number }])}>항목 추가</button></fieldset>{error && <p role="alert">{error}</p>}<div className="form-actions"><button type="button" disabled={busy} onClick={() => setEditing(null)}>취소</button><button disabled={busy || !name.trim()}>{busy ? '저장 중…' : uncertain ? '같은 요청 다시 보내기' : editing === 'new' ? '카테고리 저장' : '변경 저장'}</button></div></form>
  </section>
  return <section className="template-manager"><header className="record-heading"><button aria-label="공부로 돌아가기" onClick={close}>‹</button><h1>공부 카테고리 관리</h1></header><button onClick={() => begin('new')}>새 카테고리</button>
    {list.isPending && <p role="status">카테고리를 불러오는 중…</p>}{list.isError && <p role="alert">카테고리를 불러오지 못했습니다. <button onClick={() => void list.refetch()}>다시 불러오기</button></p>}
    <div className="template-list">{list.data?.items.map((value) => <article key={value.templateId}><div><strong>{value.currentVersion.name}</strong><small>버전 {value.currentVersion.templateVersion}{value.archived ? ' · 보관됨' : ''}</small><p>{value.currentVersion.fields.map(studyFieldSummary).join(', ') || '항목 없음'}</p></div>{!value.archived && <div className="form-actions"><button aria-label={`${value.currentVersion.name} 수정`} onClick={() => begin(value)}>수정</button><button aria-label={`${value.currentVersion.name} 보관`} onClick={() => void archive(value)}>보관</button></div>}</article>)}</div>{error && <p role="alert">{error}</p>}
  </section>
}
