import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { planner } from '@todorok/api-client'
import { seoulToday } from '@todorok/client-domain'
import { useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthProvider'

export function DomainPage({ type, title }: { type: 'WORKOUT' | 'STUDY' | 'CLIMBING'; title: string }) {
  const { session } = useAuth(), navigate = useNavigate(), today = seoulToday()
  const api = useMemo(() => new planner.CalendarApi(new planner.Configuration({ basePath: '/api/planner/v1', fetchApi: session.fetch })), [session])
  const day = useQuery({ queryKey: ['calendar', 'day', today], queryFn: ({ signal }) => api.getDayDetail({ date: today }, { signal }) })
  const tasks = day.data?.tasks?.filter((task) => task.taskType === type) ?? []
  return <section className="domain-page"><h1>{title}</h1><section className="domain-today"><h2>오늘 {title}</h2>{day.isPending && <p role="status">오늘 항목을 불러오는 중…</p>}{day.isError && <p role="alert">오늘 항목을 불러오지 못했습니다.</p>}{tasks.map((task) => <div className="task-row" key={task.taskId}><button className="task-check" disabled={task.status === 'COMPLETED'} aria-label={`${task.title} ${task.status === 'COMPLETED' ? '완료' : '기록'}`} onClick={() => navigate(`/${type.toLowerCase()}?taskId=${task.taskId}`)}>{task.status === 'COMPLETED' ? '✓' : '○'}</button><span>{task.title}{task.completionSummary && <small className="completion-summary">{task.completionSummary}</small>}</span></div>)}</section></section>
}
