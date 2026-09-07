import { useState, type SubmitEvent } from 'react'
import type { Task } from './model'
export function TaskEditor({
  task,
  busy,
  error,
  save,
  remove,
  skip,
  reopen,
  cancel,
}: {
  task: Task
  busy: boolean
  error: string
  save: (title: string, date: string, note: string) => void
  remove: () => void
  skip: () => void
  reopen: () => void
  cancel: () => void
}) {
  const [title, setTitle] = useState(task.title),
    [date, setDate] = useState(task.scheduledDate),
    [note, setNote] = useState(task.note ?? '')
  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (title.trim() && !busy) save(title.trim(), date, note)
  }
  return (
    <form className="task-form" aria-label="할 일 수정" onSubmit={submit}>
      <label htmlFor="edit-title">제목 수정</label>
      <input
        autoFocus
        id="edit-title"
        required
        maxLength={120}
        value={title}
        onChange={(e) => setTitle(e.target.value)}
      />
      <label htmlFor="edit-date">날짜 수정</label>
      <input
        id="edit-date"
        type="date"
        required
        value={date}
        onChange={(e) => setDate(e.target.value)}
      />
      <label htmlFor="task-note">할 일 메모</label>
      <textarea
        id="task-note"
        maxLength={20000}
        value={note}
        onChange={(e) => setNote(e.target.value)}
      />
      {task.seriesId && (
        <p>
          이 메모는 이번 할 일에 저장됩니다. 다음 회차의 메모는 아래 반복
          설정에서 수정할 수 있습니다.
        </p>
      )}
      {error && <p role="alert">{error}</p>}
      <div className="form-actions">
        <button disabled={busy || !title.trim()} type="submit">
          수정 저장
        </button>
        <button type="button" disabled={busy} onClick={remove}>
          삭제
        </button>
        {task.status === 'PLANNED' && (
          <button type="button" disabled={busy} onClick={skip}>
            건너뜀
          </button>
        )}
        {(task.status === 'SKIPPED' ||
          (task.status === 'COMPLETED' && task.taskType === 'GENERAL')) && (
          <button type="button" disabled={busy} onClick={reopen}>
            다시 할 일로
          </button>
        )}
        <button type="button" disabled={busy} onClick={cancel}>
          닫기
        </button>
      </div>
    </form>
  )
}
