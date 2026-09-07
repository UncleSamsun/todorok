export type TimeValue = { period: 'AM' | 'PM'; hour: string; minute: string }
export const emptyTime = (): TimeValue => ({ period: 'AM', hour: '', minute: '' })

export function toDate(date: string, value: TimeValue) {
  if (!value.hour && !value.minute) return undefined
  if (!value.hour || !value.minute) throw new Error('시간의 시와 분을 모두 선택해 주세요.')
  let hour = Number(value.hour) % 12
  if (value.period === 'PM') hour += 12
  return new Date(`${date}T${String(hour).padStart(2, '0')}:${value.minute}:00+09:00`)
}

function TimeSelects({ label, value, change }: { label: string; value: TimeValue; change: (v: TimeValue) => void }) {
  return <fieldset className="time-selects"><legend>{label}</legend>
    <select aria-label={`${label} 오전 오후`} value={value.period} onChange={(e) => change({ ...value, period: e.target.value as TimeValue['period'] })}><option value="AM">오전</option><option value="PM">오후</option></select>
    <select aria-label={`${label} 시`} value={value.hour} onChange={(e) => change({ ...value, hour: e.target.value })}><option value="">시</option>{Array.from({ length: 12 }, (_, i) => <option key={i + 1}>{i + 1}</option>)}</select>
    <select aria-label={`${label} 분`} value={value.minute} onChange={(e) => change({ ...value, minute: e.target.value })}><option value="">분</option>{Array.from({ length: 12 }, (_, i) => String(i * 5).padStart(2, '0')).map((m) => <option key={m}>{m}</option>)}</select>
  </fieldset>
}

export function RecordTimeFields({ start, end, setStart, setEnd }: { start: TimeValue; end: TimeValue; setStart: (v: TimeValue) => void; setEnd: (v: TimeValue) => void }) {
  return <details><summary>실제 시간 추가</summary><div className="time-pair"><TimeSelects label="시작" value={start} change={setStart}/><TimeSelects label="종료" value={end} change={setEnd}/></div></details>
}
