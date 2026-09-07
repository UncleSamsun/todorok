import { useEffect, useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { activity, planner } from '@todorok/api-client'
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
import { SeriesEditor } from './SeriesEditor'
import { StickyNote } from './StickyNote'
import { ActivityReturnStatus } from '../activity/ActivityReturnStatus'
import type { Task } from './model'
const apiDate = (value: string) => value
export function TodayPage() {
  const { session, state } = useAuth(),
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
      series: new planner.SeriesApi(config),
      notes: new planner.NoteApi(config),
      templates: new activity.TemplateApi(new activity.Configuration({ basePath: '/api/activity/v1', fetchApi: session.fetch })),
    }
  }, [session])
  const [selected, setSelected] = useState(() => new URLSearchParams(location.search).get('date') ?? seoulToday()),
    [view, setView] = useState(() => {
      try {
        return localStorage.getItem('todorok.calendar-view') === 'month'
          ? 'month'
          : 'week'
      } catch {
        return 'week'
      }
    })
  const [adding, setAdding] = useState<planner.TaskType | null>(() => {
      const value = new URLSearchParams(location.search).get('add')
      return Object.values(planner.TaskType).includes(value as planner.TaskType) ? value as planner.TaskType : null
    }),
    [editing, setEditing] = useState<Readonly<Task> | null>(null),
    [editingSeries, setEditingSeries] =
      useState<Readonly<planner.SeriesResponse> | null>(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(''),
    [addUncertain, setAddUncertain] = useState(false)
  const [today, setToday] = useState(seoulToday)
  const [rolloverEpoch, setRolloverEpoch] = useState(0)
  const [rolloverState, setRolloverState] = useState<
    'pending' | 'ready' | 'error'
  >('pending')
  useEffect(() => {
    let cancelled = false
    void api.tasks
      .rolloverTasks()
      .then(async (result) => {
        if (!result.today) throw new Error('Missing server date')
        if (cancelled) return
        setToday(result.today)
        if (!new URLSearchParams(location.search).has('date')) setSelected(result.today)
        await queries.invalidateQueries({ queryKey: ['calendar'] })
        await queries.invalidateQueries({ queryKey: ['task'] })
        if (!cancelled) setRolloverState('ready')
      })
      .catch(() => {
        if (!cancelled) setRolloverState('error')
      })
    return () => {
      cancelled = true
    }
  }, [api, queries, rolloverEpoch])
  const ready =
    rolloverState === 'ready' ||
    (rolloverState === 'error' && selected !== today)
  function enterToday() {
    setSelected(today)
    setAdding(null)
    setEditing(null)
    setError('')
    setRolloverState('pending')
    setRolloverEpoch((n) => n + 1)
  }
  const days = view === 'week' ? weekDays(selected) : monthDays(selected),
    from = days[0]!,
    to = days[days.length - 1]!
  const range = useQuery({
    queryKey: ['calendar', 'range', from, to],
    enabled: ready,
    queryFn: ({ signal }) =>
      api.calendar.getCalendarSummary(
        { from: apiDate(from), to: apiDate(to) },
        { signal },
      ),
  })
  const detail = useQuery({
    queryKey: ['calendar', 'day', selected],
    enabled: ready,
    queryFn: ({ signal }) =>
      api.calendar.getDayDetail({ date: apiDate(selected) }, { signal }),
  })
  const studyTemplates = useQuery({
    queryKey: ['templates', state.userId, 'STUDY', 'STUDY_CATEGORY', 'active'],
    enabled: adding === planner.TaskType.Study,
    queryFn: ({ signal }) => api.templates.listTemplates({ domain: activity.TemplateDomain.Study, kind: activity.TemplateKind.StudyCategory }, { signal }),
  })
  const summaries = new Map(range.data?.days?.map((d) => [d.date, d]))
  function select(date: string) {
    if (rolloverState === 'pending') return
    if (date === today) {
      enterToday()
      return
    }
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
  async function createSchedule(action: () => Promise<unknown>) {
    if (busy) return
    setBusy(true)
    setError('')
    try {
      await action()
      setAdding(null)
      setAddUncertain(false)
      await queries.invalidateQueries({ queryKey: ['calendar'] })
      await queries.invalidateQueries({ queryKey: ['task'] })
    } catch (reason) {
      const response = reason && typeof reason === 'object' && 'response' in reason ? (reason as { response: Response }).response : null
      const confirmed = response?.status === 400 || response?.status === 422
      setAddUncertain(!confirmed)
      setError(confirmed ? '일정 입력을 확인해 주세요. 작성 중인 내용은 보존됩니다.' : '저장 결과를 확인하지 못했습니다. 같은 요청을 다시 보내 주세요.')
    } finally {
      setBusy(false)
    }
  }
  function check(task: Task) {
    if (task.taskType !== 'GENERAL' && task.status !== 'SKIPPED') {
      void navigate(`/${task.taskType.toLowerCase()}?${task.status === 'COMPLETED' ? 'completedTaskId' : 'taskId'}=${task.taskId}`)
      return
    }
    void mutate(() =>
      task.status === 'COMPLETED' || task.status === 'SKIPPED'
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
    // Keep the draft and expected version on the same edit-session snapshot.
    // Closing the editor explicitly allows a fresh snapshot on the next open.
    if (busy || editing?.taskId === task.taskId) return
    setBusy(true)
    setError('')
    try {
      const current = await queries.fetchQuery({
        queryKey: ['task', task.taskId],
        queryFn: () => api.tasks.getTask({ taskId: task.taskId }),
        staleTime: 0,
      })
      const currentSeries = current.seriesId
        ? await api.series.getSeries({ seriesId: current.seriesId })
        : null
      setEditing(Object.freeze({ ...current }))
      setEditingSeries(currentSeries ? Object.freeze(currentSeries) : null)
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
                view === 'week'
                  ? addDays(selected, 7)
                  : addMonths(selected, 1),
              )
            }
          >
            ›
          </button>
          <button
            disabled={busy || rolloverState === 'pending'}
            onClick={enterToday}
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
            <button
              disabled={!ready}
              onClick={() => {
                if (ready) void range.refetch()
              }}
            >
              다시 불러오기
            </button>
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
        {new URLSearchParams(location.search).get('activityId') && <ActivityReturnStatus activityId={new URLSearchParams(location.search).get('activityId')!} />}
        {rolloverState === 'pending' && (
          <p role="status">지난 할 일을 이월하는 중…</p>
        )}
        {rolloverState === 'error' && (
          <div role="alert">
            <p>지난 할 일을 이월하지 못했습니다.</p>
            <button onClick={enterToday}>이월 다시 시도</button>
          </div>
        )}
        {detail.isPending && <p role="status">할 일을 불러오는 중…</p>}
        {detail.isError && (
          <p role="alert">
            할 일을 불러오지 못했습니다.{' '}
            <button
              disabled={!ready}
              onClick={() => {
                if (ready) void detail.refetch()
              }}
            >
              다시 불러오기
            </button>
          </p>
        )}
        <TaskGroups
          tasks={detail.data?.tasks ?? []}
          busy={busy || !ready}
          add={(type) => {
            if (!busy && ready) {
              setAdding(type)
              setAddUncertain(false)
              setEditing(null)
              setError('')
            }
          }}
          check={check}
          edit={(task) => void edit(task)}
        />
        <StickyNote
          key={state.generation}
          date={selected}
          api={api.notes}
          isCurrent={() =>
            session.getSnapshot().generation === state.generation
          }
        />
        {adding && (
          <QuickAdd
            key={`${adding}-${selected}`}
            date={selected}
            type={adding}
            busy={busy}
            uncertain={addUncertain}
            error={error}
            templates={studyTemplates.data?.items ?? []}
            cancel={() => { setAdding(null); setAddUncertain(false) }}
            save={(title, date, commandId, templateSelection, repeat) =>
              void createSchedule(() =>
                repeat
                  ? api.series.createSeries({
                      createSeriesRequest: {
                        commandId,
                        templateSelection,
                        title,
                        taskType: adding,
                        startDate: date,
                        ...repeat,
                      },
                    })
                  : api.tasks.createTask({
                      createTaskRequest: {
                        commandId,
                        templateSelection,
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
            save={(title, date, note) =>
              void mutate(() =>
                api.tasks.updateTask({
                  taskId: editing.taskId,
                  updateTaskRequest: {
                    note,
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
            skip={() =>
              void mutate(() =>
                api.tasks.skipTask({
                  taskId: editing.taskId,
                  versionCommand: { version: editing.version },
                }),
              )
            }
            reopen={() =>
              void mutate(() =>
                api.tasks.reopenTask({
                  taskId: editing.taskId,
                  versionCommand: { version: editing.version },
                }),
              )
            }
          />
        )}
        {editing && editingSeries && (
          <SeriesEditor
            key={`${editing.taskId}-${editingSeries.version}`}
            series={editingSeries}
            busy={busy}
            save={(request) =>
              void mutate(() =>
                api.series.updateSeries({
                  seriesId: editingSeries.seriesId,
                  updateSeriesRequest: request,
                }),
              )
            }
            archive={() =>
              void mutate(() =>
                api.series.archiveSeries({
                  seriesId: editingSeries.seriesId,
                  versionCommand: { version: editingSeries.version },
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
