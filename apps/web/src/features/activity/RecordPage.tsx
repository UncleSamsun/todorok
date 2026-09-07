import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { activity, planner } from '@todorok/api-client'
import { useNavigate, useSearchParams } from 'react-router'
import { useAuth } from '../auth/AuthProvider'
import { WorkoutFields } from './WorkoutFields'
import { StudyFields } from './StudyFields'
import { ClimbingFields } from './ClimbingFields'
import { RecordTimeFields, toDate } from './RecordTimeFields'
import { SyncStatus } from './SyncStatus'
import { useRecordDraft } from './RecordDrafts'
import { refreshActivity } from './refreshActivity'

const titles = { WORKOUT: '운동 기록', STUDY: '공부 기록', CLIMBING: '클라이밍 기록' } as const
const midnightSeoul = (date: string) => new Date(`${date}T00:00:00+09:00`)

type RecordType = 'WORKOUT' | 'STUDY' | 'CLIMBING'
export function RecordPage({ type }: { type: RecordType }) {
  const { session, state } = useAuth(), navigate = useNavigate(), queries = useQueryClient()
  const [params] = useSearchParams(), taskId = params.get('taskId') ?? ''
  const apis = useMemo(() => ({
    tasks: new planner.TaskApi(new planner.Configuration({ basePath: '/api/planner/v1', fetchApi: session.fetch })),
    activities: new activity.ActivityApi(new activity.Configuration({ basePath: '/api/activity/v1', fetchApi: session.fetch })),
  }), [session])
  const task = useQuery({ queryKey: ['task', taskId], enabled: Boolean(taskId), queryFn: ({ signal }) => apis.tasks.getTask({ taskId }, { signal }) })
  const { draft, update, clear } = useRecordDraft(`${type}:${taskId}`)
  const { date, note, workout, study, climbing, start, end, snapshot, uncertain, blocked, error } = draft
  const [saved, setSaved] = useState<activity.ActivityResponse | null>(null), [busy, setBusy] = useState(false)
  const setError = (error: string) => update({ error })
  const attempt = useRef(0), mounted = useRef(true)
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; attempt.current++ } }, [])
  const performedDate = date || task.data?.scheduledDate || ''
  const current = (id?: number) => mounted.current && (id === undefined || attempt.current === id) && session.getSnapshot().generation === state.generation
  function leave() { attempt.current++; void navigate(`/today?date=${task.data?.scheduledDate ?? performedDate}`) }
  function build(): activity.CreateActivityRequest {
    const startedAt = toDate(performedDate, start), endedAt = toDate(performedDate, end)
    if (Boolean(startedAt) !== Boolean(endedAt)) throw new Error('시작과 종료 시간을 모두 선택해 주세요.')
    if (startedAt && endedAt && endedAt <= startedAt) throw new Error('종료 시간은 시작 시간보다 늦어야 합니다.')
    return { commandId: crypto.randomUUID(), taskId, activityType: activity.ActivityType[type[0] + type.slice(1).toLowerCase() as 'Workout' | 'Study' | 'Climbing'], completionStatus: activity.ActivityCompletionStatus.Completed, performedAt: midnightSeoul(performedDate), ...(startedAt && endedAt ? { startedAt, endedAt } : {}), ...(note.trim() ? { note: note.trim() } : {}), detail: type === 'WORKOUT' ? { workout: workout.length ? { sets: workout } : {} } : type === 'STUDY' ? { study } : { climbing } }
  }
  async function submit(retry = false) {
    if (busy || blocked) return
    setError(''); setBusy(true)
    const id = ++attempt.current
    try {
      let request: activity.CreateActivityRequest
      try { request = retry && snapshot ? snapshot : structuredClone(build()) }
      catch (reason) { if (current(id)) setError(reason instanceof Error ? reason.message : '입력을 확인해 주세요.'); return }
      update({ date: performedDate, snapshot: request, uncertain: true })
      const result = await apis.activities.createActivity({ createActivityRequest: request })
      if (!current(id)) return
      clear()
      setSaved(result)
      await refreshActivity(queries, state.userId, result.syncState === 'APPLIED')
      if (current(id)) void navigate(`/today?date=${performedDate}&activityId=${result.activityId}`)
    } catch (reason) {
      if (!current(id)) return
      const response = reason && typeof reason === 'object' && 'response' in reason ? (reason as { response: Response }).response : null
      let problem: Partial<activity.ProblemDetails> | null = null
      try { problem = response ? await response.clone().json() : null } catch { /* A proxy may return HTML or an empty body. */ }
      if (!current(id)) return
      const code = problem?.code
      if (response && (response.status === 400 || response.status === 422) && problem?.retryable !== true && code && ['VALIDATION_FAILED', 'MALFORMED_JSON', 'INVALID_INTERVAL', 'INVALID_DETAIL_ITEM', 'DETAIL_TYPE_MISMATCH'].includes(code)) {
        update({ snapshot: null, uncertain: false, blocked: false, error: `기록을 저장하지 못했습니다. 입력을 수정해 주세요. (${code})` })
      } else if (response?.status === 409 && code === 'TASK_REFERENCE_PENDING' && problem?.retryable === true) {
        update({ uncertain: true, blocked: false, error: '할 일 정보가 기록 서비스에 반영되는 중입니다. 잠시 뒤 같은 요청으로 다시 확인해 주세요. (TASK_REFERENCE_PENDING)' })
      } else if (response?.status === 409 && problem?.retryable === false) {
        update({ uncertain: true, blocked: true, error: `충돌로 저장이 거절되었습니다. 오늘 화면에서 할 일 상태를 확인해 주세요. (${code ?? 'CONFLICT'})` })
      } else {
        update({ uncertain: true, blocked: false, error: `서버 응답을 확인하지 못했습니다. 같은 요청으로 결과를 확인해 주세요.${code ? ` (${code})` : ''}` })
      }
    } finally { if (current(id)) setBusy(false) }
  }
  async function check() {
    if (!saved || busy) return
    setBusy(true); setError('')
    try {
      const value = await apis.activities.getActivity({ activityId: saved.activityId })
      if (!current()) return
      setSaved(value)
      if (value.syncState === 'APPLIED') { await queries.invalidateQueries({ queryKey: ['calendar'] }); if (current()) void navigate(`/today?date=${performedDate}`) }
    } catch { if (current()) setError('동기화 상태를 불러오지 못했습니다.') }
    finally { if (current()) setBusy(false) }
  }
  if (!taskId) return <p role="alert">기록할 할 일을 선택해 주세요.</p>
  if (task.isPending) return <p role="status">할 일을 불러오는 중…</p>
  if (task.isError || task.data.taskType !== type) return <p role="alert">이 기록 화면에 맞는 할 일을 불러오지 못했습니다.</p>
  return <section className="record-page"><header className="record-heading"><button type="button" aria-label="기록 취소" onClick={leave}>‹</button><div><h1>{titles[type]}</h1><p>{task.data.title}</p></div></header>
    {saved ? <SyncStatus value={saved} checking={busy} check={() => void check()}/> : <form onSubmit={(e) => { e.preventDefault(); void submit(Boolean(snapshot)) }}>
      <fieldset className="record-inputs" disabled={busy || uncertain}>
      <label>수행일<input type="date" value={performedDate} onChange={(e) => update({ date: e.target.value, snapshot: null })} required/></label>
      {type === 'WORKOUT' ? (
        <WorkoutFields sets={workout} change={(v) => update({ workout: v, snapshot: null })}/>
      ) : type === 'STUDY' ? (
        <StudyFields value={study} change={(v) => update({ study: v, snapshot: null })}/>
      ) : (
        <ClimbingFields detail={climbing} change={(v) => update({ climbing: v, snapshot: null })}/>
      )}
      <RecordTimeFields start={start} end={end} setStart={(v) => update({ start: v, snapshot: null })} setEnd={(v) => update({ end: v, snapshot: null })}/>
      <label>기록 메모<textarea value={note} onChange={(e) => update({ note: e.target.value, snapshot: null })} /></label>
      </fieldset>
      {error && <p role="alert">{error}</p>}
      <div className="form-actions"><button type="button" onClick={leave}>{blocked ? '오늘에서 할 일 확인' : '취소'}</button><button disabled={busy || blocked}>{busy ? '저장 중…' : blocked ? '충돌 상태 확인 필요' : uncertain ? '같은 요청 다시 보내기' : '기록 저장'}</button></div>
    </form>}
    {saved && error && <p role="alert">{error}</p>}
  </section>
}
