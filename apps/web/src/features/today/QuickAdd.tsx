import { useState, type SubmitEvent } from 'react'
import type { planner } from '@todorok/api-client'
import {
  RecurrenceFields,
  initialRule,
  type RepeatOptions,
} from './RecurrenceFields'
export function QuickAdd({
  date,
  type,
  busy,
  error,
  save,
  cancel,
}: {
  date: string
  type: planner.TaskType
  busy: boolean
  error: string
  save: (title: string, date: string, repeat?: RepeatOptions) => void
  cancel: () => void
}) {
  const [title, setTitle] = useState(''),
    [scheduled, setScheduled] = useState(date)
  const [frequency, setFrequency] = useState('NONE')
  const [repeat, setRepeat] = useState<RepeatOptions>({
    rule: initialRule(date),
  })
  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (
      title.trim() &&
      !busy &&
      (frequency !== 'WEEKLY' || repeat.rule.weekdays.size)
    )
      save(title.trim(), scheduled, frequency === 'NONE' ? undefined : repeat)
  }
  return (
    <form className="task-form" onSubmit={submit}>
      <label htmlFor="task-title">제목</label>
      <input
        autoFocus
        id="task-title"
        value={title}
        maxLength={120}
        required
        onChange={(e) => setTitle(e.target.value)}
      />
      <details>
        <summary>옵션</summary>
        <label htmlFor="task-date">날짜</label>
        <input
          id="task-date"
          type="date"
          required
          value={scheduled}
          onChange={(e) => setScheduled(e.target.value)}
        />
        <label htmlFor="task-repeat">반복</label>
        <select
          id="task-repeat"
          value={frequency}
          onChange={(e) => {
            setFrequency(e.target.value)
            if (e.target.value !== 'NONE')
              setRepeat({
                ...repeat,
                rule: {
                  ...(frequency === 'NONE'
                    ? initialRule(scheduled)
                    : repeat.rule),
                  frequency: e.target
                    .value as planner.RecurrenceRuleFrequencyEnum,
                },
              })
          }}
        >
          <option value="NONE">반복 안 함</option>
          <option value="DAILY">일마다</option>
          <option value="WEEKLY">주마다</option>
          <option value="MONTHLY">월마다</option>
        </select>
        {frequency !== 'NONE' && (
          <RecurrenceFields
            id="task-repeat"
            value={repeat}
            change={setRepeat}
          />
        )}
      </details>
      {type !== 'GENERAL' && (
        <p>완료 체크를 누르면 해당 기록 화면으로 이동합니다.</p>
      )}
      {error && <p role="alert">{error}</p>}
      <div className="form-actions">
        <button type="submit" disabled={busy || !title.trim()}>
          {busy ? '저장 중…' : '저장'}
        </button>
        <button type="button" disabled={busy} onClick={cancel}>
          취소
        </button>
      </div>
    </form>
  )
}
