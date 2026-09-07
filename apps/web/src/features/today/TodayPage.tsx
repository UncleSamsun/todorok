import { useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { planner } from '@todorok/api-client'
import {
  addDays,
  addMonths,
  monthDays,
  seoulToday,
  weekDays,
} from '@todorok/client-domain'
import { useAuth } from '../auth/AuthProvider'
import { WeekCalendar } from './WeekCalendar'
import { MonthCalendar } from './MonthCalendar'
import { QuickAdd } from './QuickAdd'
import { TaskGroups } from './TaskGroups'
import { TaskEditor } from './TaskEditor'
import type { Task } from './model'
const apiDate = (value: string) => value
export function TodayPage() {
  const { session } = useAuth(),
    queries = useQueryClient(),
    navigate = useNavigate()
  const api = useMemo(() => {
    const config = new planner.Configuration({
      basePath: '/api/planner/v1',
      fetchApi: session.fetch,
    })
    return {
      tasks: new planner.TaskApi(config),
      calendar: new planner.CalendarApi(config),
    }
  }, [session])
  const [selected, setSelected] = useState(seoulToday),
    [view, setView] = useState(() => {
      try {
        return localStorage.getItem('todorok.calendar-view') === 'month'
          ? 'month'
          : 'week'
      } catch {
        return 'week'
      }
    })
  const [adding, setAdding] = useState<planner.TaskType | null>(null),
    [editing, setEditing] = useState<Task | null>(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState('')
  const days = view === 'week' ? weekDays(selected) : monthDays(selected),
    from = days[0]!,
    to = days[days.length - 1]!
  const range = useQuery({
    queryKey: ['calendar', 'range', from, to],
    queryFn: ({ signal }) =>
      api.calendar.getCalendarSummary(
        { from: apiDate(from), to: apiDate(to) },
        { signal },
      ),
  })
  const detail = useQuery({
    queryKey: ['calendar', 'day', selected],
    queryFn: ({ signal }) =>
      api.calendar.getDayDetail({ date: apiDate(selected) }, { signal }),
  })
  const summaries = new Map(range.data?.days?.map((d) => [d.date, d]))
  function select(date: string) {
    setSelected(date)
    setAdding(null)
    setEditing(null)
    setError('')
  }
  function changeView(value: string) {
    setView(value)
    try {
      localStorage.setItem('todorok.calendar-view', value)
    } catch {
      /* optional device preference */
    }
  }
  async function mutate(action: () => Promise<unknown>) {
    if (busy) return
    setBusy(true)
    setError('')
    try {
      await action()
      setAdding(null)
      setEditing(null)
      await queries.invalidateQueries({ queryKey: ['calendar'] })
      await queries.invalidateQueries({ queryKey: ['task'] })
    } catch {
      setError(
        '저장하지 못했습니다. 연결이나 변경된 내용을 확인한 뒤 다시 시도해 주세요.',
      )
    } finally {
      setBusy(false)
    }
  }
  function check(task: Task) {
    if (task.taskType !== 'GENERAL') {
      void navigate(`/${task.taskType.toLowerCase()}?taskId=${task.taskId}`)
      return
    }
    void mutate(() =>
      task.status === 'COMPLETED'
        ? api.tasks.reopenTask({
            taskId: task.taskId,
            versionCommand: { version: task.version },
          })
        : api.tasks.completeTask({
            taskId: task.taskId,
            versionCommand: { version: task.version },
          }),
    )
  }
  async function edit(task: Task) {
    if (busy) return
    setBusy(true)
    setError('')
    try {
      const current = await queries.fetchQuery({
        queryKey: ['task', task.taskId],
        queryFn: () => api.tasks.getTask({ taskId: task.taskId }),
        staleTime: 0,
      })
      setEditing(current)
      setAdding(null)
    } catch {
      setError('할 일을 불러오지 못했습니다. 다시 시도해 주세요.')
    } finally {
      setBusy(false)
    }
  }
  return (
    <div className="today-layout">
      <section className="calendar-panel" aria-label="달력">
        <div className="calendar-toolbar">
          <button
            aria-label="이전"
            disabled={busy}
            onClick={() =>
              select(
                view === 'week'
                  ? addDays(selected, -7)
                  : addMonths(selected, -1),
              )
            }
          >
            ‹
          </button>
          <span className="calendar-month">
            {selected.slice(0, 4)}년 {Number(selected.slice(5, 7))}월
          </span>
          <button
            aria-label="다음"
            disabled={busy}
            onClick={() =>
              select(
                view === 'week' ? addDays(selected, 7) : addMonths(selected, 1),
              )
            }
          >
            ›
          </button>
          <button
            disabled={busy || selected === seoulToday()}
            onClick={() => select(seoulToday())}
          >
            오늘
          </button>
          <div className="view-switch">
            {[
              ['week', '주'],
              ['month', '월'],
            ].map(([value, label]) => (
              <button
                key={value}
                aria-pressed={view === value}
                onClick={() => changeView(value!)}
              >
                {label}
              </button>
            ))}
          </div>
        </div>
        {range.isError ? (
          <p role="alert">
            달력을 불러오지 못했습니다.{' '}
            <button onClick={() => void range.refetch()}>다시 불러오기</button>
          </p>
        ) : range.isPending ? (
          <p role="status">달력을 불러오는 중…</p>
        ) : null}
        {view === 'week' ? (
          <WeekCalendar
            days={days}
            selected={selected}
            summaries={summaries}
            select={(date) => !busy && select(date)}
          />
        ) : (
          <MonthCalendar
            days={days}
            selected={selected}
            summaries={summaries}
            select={(date) => !busy && select(date)}
          />
        )}
      </section>
      <section className="day-panel" aria-label="선택 날짜">
        <h2>
          <time dateTime={selected}>{selected}</time>
        </h2>
        {detail.isPending && <p role="status">할 일을 불러오는 중…</p>}
        {detail.isError && (
          <p role="alert">
            할 일을 불러오지 못했습니다.{' '}
            <button onClick={() => void detail.refetch()}>다시 불러오기</button>
          </p>
        )}
        <TaskGroups
          tasks={detail.data?.tasks ?? []}
          busy={busy}
          add={(type) => {
            if (!busy) {
              setAdding(type)
              setEditing(null)
              setError('')
            }
          }}
          check={check}
          edit={(task) => void edit(task)}
        />
        {adding && (
          <QuickAdd
            key={`${adding}-${selected}`}
            date={selected}
            type={adding}
            busy={busy}
            error={error}
            cancel={() => setAdding(null)}
            save={(title, date) =>
              void mutate(() =>
                api.tasks.createTask({
                  createTaskRequest: {
                    title,
                    taskType: adding,
                    scheduledDate: apiDate(date),
                  },
                }),
              )
            }
          />
        )}
        {editing && (
          <TaskEditor
            key={editing.taskId}
            task={editing}
            busy={busy}
            error={error}
            cancel={() => setEditing(null)}
            save={(title, date) =>
              void mutate(() =>
                api.tasks.updateTask({
                  taskId: editing.taskId,
                  updateTaskRequest: {
                    title,
                    scheduledDate: apiDate(date),
                    version: editing.version,
                  },
                }),
              )
            }
            remove={() =>
              void mutate(() =>
                api.tasks.deleteTask({
                  taskId: editing.taskId,
                  version: editing.version,
                }),
              )
            }
          />
        )}
        {error && !adding && !editing && <p role="alert">{error}</p>}
      </section>
    </div>
  )
}
