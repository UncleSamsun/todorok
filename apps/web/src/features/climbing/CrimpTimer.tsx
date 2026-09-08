import { useEffect, useRef, useState } from 'react'
import { abortTimer, advanceTimer, idleTimer, pauseTimer, resumeTimer, skipTimer, startTimer, type TimerState } from '@todorok/client-domain'

export type CrimpTimerRecord = {
  commandId: string
  completionStatus: 'COMPLETED' | 'PARTIAL'
  startedAt: string
  endedAt: string
  detail: { durationSeconds: number; rounds: { grade: string; attempts: number; completed: boolean }[] }
}

const rounds = [
  '하프 크림프 · 15–20mm', '하프 크림프 · 15–20mm', '하프 크림프 · 15–20mm',
  '세 손가락 오픈 그립 · 30–40mm', '세 손가락 오픈 그립 · 30–40mm', '세 손가락 오픈 그립 · 30–40mm',
  '두 손가락 오픈 · 검지+중지 · 30–40mm', '두 손가락 오픈 · 중지+약지 · 30–40mm',
  '두 손가락 크림프 · 중지+약지 · 15–20mm', '두 손가락 크림프 · 검지+중지 · 15–20mm',
]
const text = { IDLE: '시작 전', PREPARING: '준비', WORK: '운동', REST: '휴식', PAUSED: '일시정지', FINISHED: '완료', ABORTED: '중단' } as const
const live = (state: TimerState) => state.phase === 'PREPARING' || state.phase === 'WORK' || state.phase === 'REST'

export function CrimpTimer({ persist, close }: { persist: (record: CrimpTimerRecord) => Promise<boolean>; close: () => void }) {
  const [timer, setTimer] = useState(idleTimer), [now, setNow] = useState(Date.now()), [saving, setSaving] = useState(false), [error, setError] = useState('')
  const command = useRef(''), persisted = useRef(false)
  useEffect(() => {
    if (!live(timer)) return
    const id = setInterval(() => { const current = Date.now(); setNow(current); setTimer((value) => advanceTimer(value, current).state) }, 250)
    return () => clearInterval(id)
  }, [timer.phase])
  useEffect(() => {
    if (timer.phase !== 'FINISHED' && timer.phase !== 'ABORTED') return
    if (persisted.current || (timer.phase === 'ABORTED' && timer.completedRounds === 0) || timer.startedAt === null) return
    persisted.current = true
    setSaving(true); setError('')
    const detail = { durationSeconds: timer.completedRounds * 60, rounds: rounds.slice(0, timer.completedRounds).map((grade) => ({ grade, attempts: 1, completed: true })) }
    void persist({ commandId: command.current, completionStatus: timer.phase === 'FINISHED' ? 'COMPLETED' : 'PARTIAL', startedAt: new Date(timer.startedAt).toISOString(), endedAt: new Date(Date.now()).toISOString(), detail })
      .then((saved) => { if (!saved) { persisted.current = false; setError('기록을 저장하지 못했습니다. 같은 기록으로 다시 시도해 주세요.') } })
      .catch(() => { persisted.current = false; setError('기록을 저장하지 못했습니다. 같은 기록으로 다시 시도해 주세요.') })
      .finally(() => setSaving(false))
  }, [timer, persist])
  const remaining = timer.deadlineAt === null ? timer.remainingMs ?? 0 : Math.max(0, timer.deadlineAt - now)
  function start() { const current = Date.now(); command.current = crypto.randomUUID(); persisted.current = false; setError(''); setNow(current); setTimer(startTimer(current)) }
  function retry() { persisted.current = false; setTimer({ ...timer }) }
  return <section className="crimp-timer" aria-label="크림프 타이머">
    <header><button type="button" aria-label="타이머 닫기" onClick={close}>‹</button><div><h2>크림프 트레이닝</h2><p>통증이 있으면 즉시 중단하세요.</p></div></header>
    <p className="timer-phase">{text[timer.phase]}</p>
    <p className="timer-count">{String(Math.ceil(remaining / 1000)).padStart(2, '0')}</p>
    {timer.round > 0 && timer.phase !== 'FINISHED' && <><p>{timer.round} / 10 라운드</p><strong>{rounds[timer.round - 1]}</strong></>}
    <p>완료한 라운드 {timer.completedRounds}개</p>
    {timer.phase === 'IDLE' && <button type="button" onClick={start}>타이머 시작</button>}
    {live(timer) && <div className="timer-actions"><button type="button" onClick={() => { const current = Date.now(); setNow(current); setTimer(pauseTimer(timer, current)) }}>일시정지</button><button type="button" onClick={() => { const current = Date.now(); setNow(current); setTimer(skipTimer(timer, current)) }}>라운드 건너뛰기</button><button type="button" onClick={() => setTimer(abortTimer(timer))}>중단하고 기록</button></div>}
    {timer.phase === 'PAUSED' && <div className="timer-actions"><button type="button" onClick={() => { const current = Date.now(); setNow(current); setTimer(resumeTimer(timer, current)) }}>재개</button><button type="button" onClick={() => setTimer(abortTimer(timer))}>중단하고 기록</button></div>}
    {(timer.phase === 'FINISHED' || timer.phase === 'ABORTED') && <>{saving && <p role="status">기록을 저장하는 중…</p>}{error && <><p role="alert">{error}</p><button type="button" onClick={retry}>기록 다시 시도</button></>}<button type="button" onClick={close}>기록 화면으로</button></>}
  </section>
}
