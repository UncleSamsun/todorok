import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { activity, activityTimestamps, correctEditableActivity, getEditableActivity, matchesActivityCorrection, positiveActivityInterval, voidEditableActivity, type ActivityCorrection, type EditableActivity } from '@todorok/api-client'
import { useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthProvider'
import { WorkoutFields } from './WorkoutFields'
import { StudyFields } from './StudyFields'
import { ClimbingFields } from './ClimbingFields'
import { RecordTimeFields, emptyTime, toDate, type TimeValue } from './RecordTimeFields'
import { SyncStatus } from './SyncStatus'
import { useCorrectionDraft, type CorrectionOperation } from './CorrectionDrafts'
import { useAppliedActivity } from './useAppliedActivity'
import { refreshActivity } from './refreshActivity'

type RecordType = 'WORKOUT' | 'STUDY' | 'CLIMBING'
const dateInSeoul = (value: Date) => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(value)
function timeValue(value?: Date): TimeValue {
  if (!value) return emptyTime()
  const parts = new Intl.DateTimeFormat('en-GB', { timeZone: 'Asia/Seoul', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(value)
  const hour = Number(parts.find((part) => part.type === 'hour')?.value ?? 0)
  return { period: hour >= 12 ? 'PM' : 'AM', hour: String(hour % 12 || 12), minute: parts.find((part) => part.type === 'minute')?.value ?? '00' }
}
function shiftDate(value: string | undefined, from: string, to: string) {
  if (!value || from === to) return value
  const shifted = new Date(Date.parse(value) + Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)).toISOString()
  return shifted.replace(/\.\d+Z$/, `${/\.\d+/.exec(value)?.[0] ?? ''}Z`)
}
function retainRevision(previous: unknown, incoming: unknown) {
  const before = previous as EditableActivity | undefined, next = incoming as EditableActivity
  // An earlier GET can complete after PATCH has primed the next revision.
  if (before?.activityId === next.activityId && (before.version > next.version ||
    (before.version === next.version && before.syncState !== 'PENDING' && next.syncState === 'PENDING'))) return previous
  return incoming
}
export function ActivityRecordPage({ type, activityId }: { type: RecordType; activityId: string }) {
  const { session, state } = useAuth(), navigate = useNavigate(), queries = useQueryClient()
  const api = useMemo(() => new activity.ActivityApi(new activity.Configuration({ basePath: '/api/activity/v1', fetchApi: session.fetch })), [session])
  const key = ['activity', state.userId, activityId]
  const record = useQuery({ queryKey: key, queryFn: ({ signal }) => getEditableActivity(api, activityId, signal), structuralSharing: retainRevision, refetchInterval: (query) => query.state.data?.syncState === 'PENDING' ? 1500 : false })
  useAppliedActivity(record.data, activityId)
  const { draft, update } = useCorrectionDraft(activityId)
  const { baseline, date, note, workout, study, climbing, start, end, operation, mode, error } = draft
  const [busy, setBusy] = useState(false), mounted = useRef(true), attempt = useRef(0)
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; attempt.current++ } }, [])
  const current = (id: number) => mounted.current && attempt.current === id && session.getSnapshot().generation === state.generation
  function initialize(value: EditableActivity) {
    update({ baseline: structuredClone(value), date: dateInSeoul(value.performedAt), note: value.note ?? '', workout: value.detail.workout?.sets ?? [], study: value.detail.study ?? {}, climbing: value.detail.climbing ?? {}, start: timeValue(value.startedAt), end: timeValue(value.endedAt), dateDirty: false, startDirty: false, endDirty: false, operation: null, mode: 'ready', error: '' })
  }
  useEffect(() => { if (!baseline && record.data) initialize(record.data) }, [record.data, baseline])
  function build(): ActivityCorrection {
    if (!baseline) throw new Error('원본 기록을 불러오는 중입니다.')
    const original = activityTimestamps(baseline), originalDate = dateInSeoul(baseline.performedAt)
    const startedAt = draft.startDirty ? toDate(date, start)?.toISOString() : shiftDate(original.startedAt, originalDate, date)
    const endedAt = draft.endDirty ? toDate(date, end)?.toISOString() : shiftDate(original.endedAt, originalDate, date)
    if (Boolean(startedAt) !== Boolean(endedAt)) throw new Error('시작과 종료 시간을 모두 선택해 주세요.')
    if (startedAt && endedAt && !positiveActivityInterval(startedAt, endedAt)) throw new Error('종료 시간은 시작 시간보다 늦어야 합니다.')
    return { expectedVersion: baseline.version, performedAt: draft.dateDirty ? new Date(`${date}T00:00:00+09:00`).toISOString() : original.performedAt, ...(startedAt && endedAt ? { startedAt, endedAt } : {}), ...(note.trim() ? { note: note.trim() } : {}), detail: type === 'WORKOUT' ? { workout: workout.length ? { sets: workout } : {} } : type === 'STUDY' ? { study } : { climbing } }
  }
  async function accept(value: EditableActivity) {
    queries.setQueryData(key, value)
    initialize(value)
    // The shared observer refreshes planner data once this revision is APPLIED.
    await refreshActivity(queries, state.userId, false)
  }
  function matches(value: EditableActivity, command: CorrectionOperation) {
    if (!baseline) return false
    if (command.kind === 'patch') return matchesActivityCorrection(value, baseline, command.body)
    return matchesActivityCorrection(value, { ...baseline, status: activity.ActivityStatus.Voided }, { ...activityTimestamps(baseline), expectedVersion: command.body.version, note: baseline.note, detail: baseline.detail })
  }
  async function confirm(command: CorrectionOperation, id: number) {
    const value = await getEditableActivity(api, activityId)
    if (!current(id)) return
    if (matches(value, command)) await accept(value)
    else { queries.setQueryData(key, value); update({ error: '수정 결과를 확인하지 못했습니다. 현재 기록이 초안과 달라 자동으로 덮어쓰지 않았습니다. 작성 중인 내용은 보존됩니다.' }) }
  }
  async function perform(command: CorrectionOperation) {
    if (busy || !baseline) return
    const id = ++attempt.current
    setBusy(true)
    update({ operation: command, mode: 'uncertain', error: '' })
    try {
      const value = command.kind === 'patch' ? await correctEditableActivity(api, activityId, command.body) : await voidEditableActivity(api, activityId, command.body)
      if (current(id)) await accept(value)
    } catch (reason) {
      if (!current(id)) return
      const response = reason && typeof reason === 'object' && 'response' in reason ? (reason as { response: Response }).response : null
      let problem: { code?: string; retryable?: boolean } | null = null
      try { problem = response ? await response.clone().json() : null } catch { /* proxy responses can be non-JSON */ }
      if (!current(id)) return
      if (response && [400, 422].includes(response.status) && problem?.code && problem.retryable !== true && ['VALIDATION_FAILED', 'MALFORMED_JSON', 'INVALID_INTERVAL', 'INVALID_DETAIL_ITEM', 'DETAIL_TYPE_MISMATCH'].includes(problem.code)) update({ operation: null, mode: 'ready', error: `입력을 수정해 주세요. (${problem.code})` })
      else if (response?.status === 409) update({ mode: 'conflict', error: '다른 변경과 충돌했습니다. 작성 중인 내용은 보존했습니다. 현재 기록을 확인한 뒤 다시 열어 주세요.' })
      else {
        update({ error: '수정 결과를 확인하지 못했습니다. 원 요청과 초안을 보존했습니다.' })
        try { await confirm(command, id) } catch { /* retain the uncertain operation */ }
      }
    } finally { if (current(id)) setBusy(false) }
  }
  function save() {
    if (busy || mode !== 'ready') return
    try { void perform({ kind: 'patch', body: structuredClone(build()) }) } catch (reason) { update({ error: reason instanceof Error ? reason.message : '입력을 확인해 주세요.' }) }
  }
  async function check() {
    if (busy) return
    const id = ++attempt.current
    setBusy(true)
    try {
      if (operation) await confirm(operation, id)
      else { const value = await getEditableActivity(api, activityId); if (current(id)) queries.setQueryData(key, value) }
    } catch { if (current(id)) update({ error: '현재 기록을 확인하지 못했습니다. 초안은 보존됩니다.' }) }
    finally { if (current(id)) setBusy(false) }
  }
  async function reopen() {
    if (busy) return
    const id = ++attempt.current
    setBusy(true)
    try { const value = await getEditableActivity(api, activityId); if (current(id)) { queries.setQueryData(key, value); initialize(value) } }
    catch { if (current(id)) update({ error: '현재 기록을 불러오지 못했습니다. 초안은 보존됩니다.' }) }
    finally { if (current(id)) setBusy(false) }
  }
  if (record.isError && !baseline) return <p role="alert">기록 상세를 불러오지 못했습니다. <button onClick={() => void record.refetch()}>다시 불러오기</button></p>
  if (!baseline) return <p role="status">기록 상세를 불러오는 중…</p>
  if (baseline.activityType !== type) return <p role="alert">이 유형의 기록이 아닙니다.</p>
  const voided = baseline.status === 'VOIDED', locked = busy || mode !== 'ready'
  const template = baseline.templateSnapshot
  return <section className="record-page"><header className="record-heading"><button aria-label="기록 목록" onClick={() => navigate(`/${type.toLowerCase()}`)}>‹</button><h1>{template?.name ?? (voided ? '취소된 기록' : '기록 수정')}</h1></header>
    <SyncStatus value={record.data ?? baseline} checking={busy || record.isFetching} check={() => void check()} />
    {record.isError && <p role="alert">상태를 불러오지 못했습니다. <button onClick={() => void check()}>상태 다시 확인</button></p>}
    {baseline.detailFormat === activity.DetailFormat.Legacy && baseline.legacyStudyPayloadRaw && <section className="legacy-study"><h2>기존 공부 자료</h2><p>검증되지 않은 이전 형식의 원문이며 읽기 전용입니다.</p><pre>{baseline.legacyStudyPayloadRaw}</pre></section>}
    {voided ? <p>이 기록은 취소되었으며 이력은 보존됩니다.</p> : <form onSubmit={(event) => { event.preventDefault(); save() }}><fieldset className="record-inputs" disabled={locked}>
      <label>수행일<input type="date" value={date} onChange={(event) => update({ date: event.target.value, dateDirty: true })} required /></label>
      {type === 'WORKOUT' ? <WorkoutFields sets={workout} change={(value) => update({ workout: value })} /> : type === 'STUDY' ? <StudyFields definitions={template?.fields} value={study} change={(value) => update({ study: value })} /> : <ClimbingFields detail={climbing} change={(value) => update({ climbing: value })} />}
      <RecordTimeFields start={start} end={end} setStart={(value) => update({ start: value, startDirty: true })} setEnd={(value) => update({ end: value, endDirty: true })} />
      <label>기록 메모<textarea value={note} onChange={(event) => update({ note: event.target.value })} /></label>
    </fieldset><div className="form-actions"><button type="button" disabled={locked} onClick={() => void perform({ kind: 'void', body: { reason: '사용자 취소', version: baseline.version } })}>기록 취소</button><button disabled={locked}>{busy ? '저장 중…' : '수정 저장'}</button></div></form>}
    {error && <p role="alert">{error}</p>}
    {operation && <div className="form-actions"><button disabled={busy} onClick={() => void check()}>결과 다시 확인</button>{mode === 'uncertain' && <button disabled={busy} onClick={() => void perform(operation)}>같은 요청 다시 보내기</button>}<button disabled={busy} onClick={() => void reopen()}>초안을 버리고 현재 기록으로 다시 열기</button></div>}
  </section>
}
export default ActivityRecordPage
