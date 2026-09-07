import { groups, type Task } from './model'
import type { planner } from '@todorok/api-client'
export function TaskGroups({
  tasks,
  busy,
  add,
  check,
  edit,
}: {
  tasks: Task[]
  busy: boolean
  add: (type: planner.TaskType) => void
  check: (task: Task) => void
  edit: (task: Task) => void
}) {
  return (
    <div className="task-groups">
      {groups.map(([type, label]) => {
        const rows = tasks.filter((t) => t.taskType === type)
        return (
          <section key={type} className="task-group">
            <div className="group-heading">
              <h3>
                <span className={`category-dot ${type}`} />
                {label}{' '}
                <small>
                  {rows.filter((t) => t.status === 'COMPLETED').length} /{' '}
                  {rows.length}
                </small>
              </h3>
              <button aria-label={`${label} 추가`} onClick={() => add(type)}>
                +
              </button>
            </div>
            {rows.map((task) => (
              <div className="task-row" key={task.taskId}>
                <button
                  className="task-check"
                  aria-label={`${task.title} ${task.status === 'SKIPPED' ? '건너뜀 취소' : task.taskType === 'GENERAL' ? (task.status === 'COMPLETED' ? '완료 취소' : '완료') : '기록'}`}
                  aria-pressed={task.status === 'COMPLETED'}
                  disabled={busy}
                  onClick={() => check(task)}
                >
                  {task.status === 'COMPLETED'
                    ? '✓'
                    : task.status === 'SKIPPED'
                      ? '↷'
                      : '○'}
                </button>
                <button
                  className={`task-title ${task.status === 'COMPLETED' ? 'done' : ''}`}
                  onClick={() => edit(task)}
                  aria-label={`${task.title} 수정`}
                >
                  {task.title}
                  {task.seriesId && <small> · 반복</small>}
                  {task.status === 'SKIPPED' && <small> · 건너뜀</small>}
                  {task.completionSummary && <small className="completion-summary">{task.completionSummary}</small>}
                  {task.startedAt && task.endedAt && <small className="time-block">{task.startedAt.toLocaleTimeString('ko-KR', { hour: 'numeric', minute: '2-digit', timeZone: 'Asia/Seoul' })}–{task.endedAt.toLocaleTimeString('ko-KR', { hour: 'numeric', minute: '2-digit', timeZone: 'Asia/Seoul' })}</small>}
                </button>
              </div>
            ))}
          </section>
        )
      })}
    </div>
  )
}
