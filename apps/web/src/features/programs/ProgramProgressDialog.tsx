import { useMemo, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { activity } from '@todorok/api-client'
import { useAuth } from '../auth/AuthProvider'
import { TaskFloatingPanel } from '../today/TaskFloatingPanel'
import { requestFailure } from '../activity/requestFailure'

export function ProgramProgressDialog({ enrollment, catalog, close }: {
  enrollment: activity.ProgramEnrollmentResponse; catalog: activity.ProgramCatalogSummary; close: () => void
}) {
  const { session, state } = useAuth(), queries = useQueryClient()
  const api = useMemo(() => new activity.ActivityApi(new activity.Configuration({ basePath: '/api/activity/v1', fetchApi: session.fetch })), [session])
  const [week, setWeek] = useState(enrollment.currentWeek), [slot, setSlot] = useState(enrollment.currentSession)
  const [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false), [error, setError] = useState('')
  const pending = useRef(false), request = useRef<activity.ScheduleProgramSessionRequest | null>(null)
  async function save() {
    if (pending.current) return
    pending.current = true; setBusy(true); setError('')
    request.current ??= { commandId: crypto.randomUUID(), week, session: slot, expectedRevision: enrollment.revision ?? 0 }
    try {
      const result = await api.scheduleProgramSession({ enrollmentId: enrollment.enrollmentId, scheduleProgramSessionRequest: request.current })
      queries.setQueryData<activity.ProgramEnrollmentResponse[]>(['program-enrollments', state.userId ?? 'session'], current =>
        (current ?? []).map(item => item.enrollmentId === result.enrollmentId ? result : item))
      await Promise.all([queries.invalidateQueries({ queryKey: ['program-enrollments'] }), queries.invalidateQueries({ queryKey: ['calendar'] })])
      close()
    } catch (reason) {
      const failure = await requestFailure(reason)
      setUncertain(!failure.rejected)
      if (failure.rejected) request.current = null
      setError(failure.rejected ? '프로그램 상태를 저장하지 못했습니다. 현재 상태를 확인한 뒤 다시 시도해 주세요.' : '저장 결과를 확인하지 못했습니다. 같은 요청을 다시 보내 주세요.')
      if (failure.status === 409) await queries.invalidateQueries({ queryKey: ['program-enrollments'] })
    } finally { pending.current = false; setBusy(false) }
  }
  return <TaskFloatingPanel label={`${catalog.name} 상태 수정`} busy={busy} close={close}>
    <form className="program-enrollment" onSubmit={event => { event.preventDefault(); void save() }}>
      <h3>프로그램 상태 수정</h3>
      <label>주차<select value={week} disabled={busy || uncertain} onChange={event => setWeek(Number(event.target.value))}>
        {Array.from({ length: catalog.totalWeeks }, (_, index) => <option key={index + 1} value={index + 1}>{index + 1}주차</option>)}
      </select></label>
      <label>회차<select value={slot} disabled={busy || uncertain} onChange={event => setSlot(Number(event.target.value))}>
        {Array.from({ length: catalog.sessionsPerWeek }, (_, index) => <option key={index + 1} value={index + 1}>{index + 1}회차</option>)}
      </select></label>
      <p>선택한 주차와 회차부터 이어갑니다. 이전 운동 기록은 유지됩니다.</p>
      {error && <p role="alert">{error}</p>}
      <div className="form-actions"><button type="button" disabled={busy} onClick={close}>닫기</button>
        <button type="submit" disabled={busy}>{busy ? '저장 중…' : uncertain ? '같은 요청 다시 보내기' : '상태 저장'}</button></div>
    </form>
  </TaskFloatingPanel>
}
