import { groups, type Task } from './model'
import type { planner } from '@todorok/api-client'
import { useState, type ReactNode } from 'react'
import { TaskMenu } from './TaskMenu'
import { TaskFloatingPanel } from './TaskFloatingPanel'
import { InlineTaskName } from './InlineTaskName'
export function TaskGroups({
  tasks,
  busy,
  add,
  check,
  edit,
  remove,
  adding,
  addForm,
  editingTaskId,
  editForm,
  closeEdit = () => {},
  rename,
  editLabel = '일정 수정',
}: {
  tasks: Task[]
  busy: boolean
  add: (type: planner.TaskType) => void
  check: (task: Task) => void
  edit: (task: Task, mode?: 'task' | 'repeat' | 'memo') => void
  remove?: (task: Task) => void
  adding?: planner.TaskType | null
  addForm?: ReactNode
  editingTaskId?: string
  editForm?: ReactNode
  closeEdit?: () => void
  rename?: (task: Task, title: string) => Promise<boolean>
  editLabel?: string
}) {
  const [renaming, setRenaming] = useState<Task | null>(null)
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
              <button aria-label={`${label} 추가`} disabled={busy} onClick={() => add(type)}>
                +
              </button>
            </div>
            {rows.map((task) => (
              <div className="task-row-shell" key={task.taskId}>
              <div className="task-row">
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
                {renaming?.taskId === task.taskId && rename ? <InlineTaskName task={renaming} busy={busy} save={rename} close={()=>setRenaming(null)} /> : <button
                  className={`task-title ${task.status === 'COMPLETED' ? 'done' : ''}`}
                  disabled={busy}
                  onClick={() => rename ? setRenaming(task) : edit(task)}
                  aria-label={`${task.title} 수정`}
                >
                  {task.title}
                  {task.scheduledTime && <small> · {task.scheduledTime}{task.reminderEnabled ? ' 알림' : ''}</small>}
                  {task.seriesId && <small> · 반복</small>}
                  {task.status === 'SKIPPED' && <small> · 건너뜀</small>}
                  {task.completionSummary && <small className="completion-summary">{task.completionSummary}</small>}
                  {task.startedAt && task.endedAt && <small className="time-block">{task.startedAt.toLocaleTimeString('ko-KR', { hour: 'numeric', minute: '2-digit', timeZone: 'Asia/Seoul' })}–{task.endedAt.toLocaleTimeString('ko-KR', { hour: 'numeric', minute: '2-digit', timeZone: 'Asia/Seoul' })}</small>}
                </button>}
                <TaskMenu task={task} busy={busy} edit={(mode) => edit(task, mode)} remove={remove ? () => remove(task) : undefined} />
              </div>
              {editingTaskId === task.taskId && <TaskFloatingPanel label={editLabel} close={closeEdit} busy={busy}>{editForm}</TaskFloatingPanel>}
              </div>
            ))}
            {adding === type && addForm}
          </section>
        )
      })}
    </div>
  )
}
