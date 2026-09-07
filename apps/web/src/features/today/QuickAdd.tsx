import { useState, type SubmitEvent } from 'react'
import type { planner } from '@todorok/api-client'
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
  save: (title: string, date: string) => void
  cancel: () => void
}) {
  const [title, setTitle] = useState(''),
    [scheduled, setScheduled] = useState(date)
  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (title.trim() && !busy) save(title.trim(), scheduled)
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
