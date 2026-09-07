import { useEffect, useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { activity } from '@todorok/api-client'
import { useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthProvider'
import { WorkoutFields } from './WorkoutFields'
import { StudyFields } from './StudyFields'
import { ClimbingFields } from './ClimbingFields'
import { RecordTimeFields, emptyTime, toDate, type TimeValue } from './RecordTimeFields'
import { SyncStatus } from './SyncStatus'

type RecordType = 'WORKOUT' | 'STUDY' | 'CLIMBING'
const dateInSeoul = (value: Date) => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(value)
const timeValue = (value?: Date): TimeValue => {
  if (!value) return emptyTime()
  const parts = new Intl.DateTimeFormat('en-GB', { timeZone: 'Asia/Seoul', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(value)
  const hour24 = Number(parts.find((part) => part.type === 'hour')?.value ?? 0)
  return { period: hour24 >= 12 ? 'PM' : 'AM', hour: String(hour24 % 12 || 12), minute: parts.find((part) => part.type === 'minute')?.value ?? '00' }
}

export function ActivityRecordPage({ type, activityId }: { type: RecordType; activityId: string }) {
  const { session, state } = useAuth(), navigate = useNavigate(), queries = useQueryClient()
  const api = useMemo(() => new activity.ActivityApi(new activity.Configuration({ basePath: '/api/activity/v1', fetchApi: session.fetch })), [session])
  const record = useQuery({ queryKey: ['activity', state.userId, activityId], queryFn: ({ signal }) => api.getActivity({ activityId }, { signal }) })
  const [date, setDate] = useState(''), [note, setNote] = useState(''), [workout, setWorkout] = useState<activity.WorkoutSet[]>([]), [study, setStudy] = useState<activity.StudyDetail>({}), [climbing, setClimbing] = useState<activity.ClimbingDetail>({}), [start, setStart] = useState<TimeValue>(emptyTime), [end, setEnd] = useState<TimeValue>(emptyTime)
  const [snapshot, setSnapshot] = useState<activity.CorrectActivityRequest | null>(null), [busy, setBusy] = useState(false), [error, setError] = useState(''), [initialized, setInitialized] = useState(false)
  useEffect(() => { if (!record.data || initialized) return; const value = record.data; setDate(dateInSeoul(value.performedAt)); setNote(value.note ?? ''); setWorkout(value.detail.workout?.sets ?? []); setStudy(value.detail.study ?? {}); setClimbing(value.detail.climbing ?? {}); setStart(timeValue(value.startedAt)); setEnd(timeValue(value.endedAt)); setInitialized(true) }, [record.data, initialized])
  const back = () => navigate(`/${type.toLowerCase()}`)
  async function refreshRelated(value: activity.ActivityResponse, oldDate: string) {
    queries.setQueryData(['activity', state.userId, activityId], value)
    await Promise.all([
      queries.invalidateQueries({ queryKey: ['activities', state.userId] }),
      queries.invalidateQueries({ queryKey: ['activity-summary', state.userId] }),
      queries.invalidateQueries({ queryKey: ['calendar-summary', state.userId] }),
      queries.invalidateQueries({ queryKey: ['calendar', 'day', oldDate] }),
      queries.invalidateQueries({ queryKey: ['calendar', 'day', dateInSeoul(value.performedAt)] }),
    ])
  }
  function build() {
    const startedAt = toDate(date, start), endedAt = toDate(date, end)
    if (Boolean(startedAt) !== Boolean(endedAt)) throw new Error('시작과 종료 시간을 모두 선택해 주세요.')
    return { expectedVersion: record.data!.version, performedAt: new Date(`${date}T00:00:00+09:00`), ...(startedAt && endedAt ? { startedAt, endedAt } : {}), ...(note.trim() ? { note: note.trim() } : {}), detail: type === 'WORKOUT' ? { workout: workout.length ? { sets: workout } : {} } : type === 'STUDY' ? { study } : { climbing } } satisfies activity.CorrectActivityRequest
  }
  async function save() {
    if (busy || !record.data) return
    setBusy(true); setError('')
    let request: activity.CorrectActivityRequest
    try { request = snapshot ?? structuredClone(build()); setSnapshot(request) } catch (reason) { setError(reason instanceof Error ? reason.message : '입력을 확인해 주세요.'); setBusy(false); return }
    try { const saved = await api.correctActivity({ activityId, correctActivityRequest: request }); setSnapshot(null); await refreshRelated(saved, dateInSeoul(record.data.performedAt)); record.refetch(); setInitialized(false) }
    catch (reason) {
      const response = reason && typeof reason === 'object' && 'response' in reason ? (reason as { response: Response }).response : null
      if (response?.status === 409) setError('다른 변경과 충돌했습니다. 작성 중인 내용은 보존했습니다. 새 기록을 확인한 뒤 다시 수정해 주세요.')
      else {
        try {
          const current = await api.getActivity({ activityId })
          const sameResult = current.version > request.expectedVersion && dateInSeoul(current.performedAt) === dateInSeoul(request.performedAt) && (current.note ?? '') === (request.note ?? '') && JSON.stringify(current.detail) === JSON.stringify(request.detail)
          if (sameResult) { setSnapshot(null); await refreshRelated(current, dateInSeoul(record.data.performedAt)); await record.refetch(); setInitialized(false) }
          else setError('수정 결과를 확인하지 못했습니다. 현재 기록이 초안과 달라 자동으로 덮어쓰지 않았습니다. 작성 중인 내용은 보존됩니다.')
        } catch { setError('수정 결과를 확인하지 못했습니다. 현재 기록을 확인해 주세요. 작성 중인 내용은 보존됩니다.') }
      }
    }
    finally { setBusy(false) }
  }
  async function voidRecord() {
    if (!record.data || busy || record.data.status === 'VOIDED') return
    setBusy(true); setError('')
    try { const saved = await api.voidActivity({ activityId, voidActivityRequest: { reason: '사용자 취소', version: record.data.version } }); await refreshRelated(saved, dateInSeoul(record.data.performedAt)); await record.refetch(); setInitialized(false) }
    catch (reason) { const response = reason && typeof reason === 'object' && 'response' in reason ? (reason as { response: Response }).response : null; if (response?.status === 409) setError('취소 요청이 다른 변경과 충돌했습니다. 현재 기록을 다시 확인해 주세요.'); else setError('취소 결과를 확인하지 못했습니다. 현재 기록을 다시 확인해 주세요.') }
    finally { setBusy(false) }
  }
  if (record.isPending) return <p role="status">기록 상세를 불러오는 중…</p>
  if (record.isError || record.data.activityType !== type) return <p role="alert">기록 상세를 불러오지 못했습니다.</p>
  if (record.data.status === 'VOIDED') return <section className="record-page"><header className="record-heading"><button aria-label="기록 목록" onClick={back}>‹</button><h1>취소된 기록</h1></header><p>이 기록은 취소되었으며 이력은 보존됩니다.</p><SyncStatus value={record.data} checking={busy} check={() => void record.refetch()} /></section>
  return <section className="record-page"><header className="record-heading"><button aria-label="기록 목록" onClick={back}>‹</button><h1>기록 수정</h1></header><form onSubmit={(event) => { event.preventDefault(); void save() }}><fieldset className="record-inputs" disabled={busy}><label>수행일<input type="date" value={date} onChange={(event) => setDate(event.target.value)} required /></label>{type === 'WORKOUT' ? <WorkoutFields sets={workout} change={setWorkout} /> : type === 'STUDY' ? <StudyFields value={study} change={setStudy} /> : <ClimbingFields detail={climbing} change={setClimbing} />}<RecordTimeFields start={start} end={end} setStart={setStart} setEnd={setEnd} /><label>기록 메모<textarea value={note} onChange={(event) => setNote(event.target.value)} /></label></fieldset>{error && <p role="alert">{error}</p>}<div className="form-actions"><button type="button" onClick={() => void voidRecord()}>기록 취소</button><button disabled={busy}>{busy ? '저장 중…' : '수정 저장'}</button></div></form></section>
}
export default ActivityRecordPage
