import { useMemo, useState } from 'react'
import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { activity, planner } from '@todorok/api-client'
import { seoulToday } from '@todorok/client-domain'
import { useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthProvider'
import { useActivityMonth } from './ActivityMonth'
import { StudyTemplateManager } from '../study/StudyTemplateManager'

type RecordType = 'WORKOUT' | 'STUDY' | 'CLIMBING'
const apiType = (type: RecordType) => activity.ActivityType[type[0] + type.slice(1).toLowerCase() as 'Workout' | 'Study' | 'Climbing']
const monthRange = (month: string) => { const [year, value] = month.split('-').map(Number); const end = new Date(Date.UTC(year, value, 0)).getUTCDate(); return { from: `${month}-01`, to: `${month}-${String(end).padStart(2, '0')}` } }
const displayMonth = (month: string) => { const [year, value] = month.split('-'); return `${year}년 ${Number(value)}월` }
const performedDate = (value: Date) => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(value)

export function DomainPage({ type, title }: { type: RecordType; title: string }) {
  const { session, state } = useAuth(), navigate = useNavigate(), today = seoulToday(), { month, previous, next, atCurrent } = useActivityMonth()
  const [pastOpen, setPastOpen] = useState(false), [pastDate, setPastDate] = useState(today), [managingTemplates, setManagingTemplates] = useState<activity.TemplateKind | null>(null)
  const apis = useMemo(() => ({ calendar: new planner.CalendarApi(new planner.Configuration({ basePath: '/api/planner/v1', fetchApi: session.fetch })), activities: new activity.ActivityApi(new activity.Configuration({ basePath: '/api/activity/v1', fetchApi: session.fetch })) }), [session])
  const range = monthRange(month), owner = state.userId ?? 'session'
  const activitySummary = useQuery({ queryKey: ['activity-summary', owner, month, type], queryFn: ({ signal }) => apis.activities.getMonthlyActivitySummary({ month, activityType: apiType(type) }, { signal }) })
  const registration = useQuery({ queryKey: ['calendar', 'summary', owner, month], queryFn: ({ signal }) => apis.calendar.getCalendarSummary(range, { signal }) })
  const registeredCount = registration.data?.days.reduce((sum, calendarDay) => sum + (calendarDay.categoryProgress.find((item) => item.taskType === type)?.totalCount ?? 0), 0)
  const day = useQuery({ queryKey: ['calendar', 'day', pastDate], enabled: pastOpen, queryFn: ({ signal }) => apis.calendar.getDayDetail({ date: pastDate }, { signal }) })
  const history = useInfiniteQuery({ queryKey: ['activities', owner], initialPageParam: undefined as string | undefined, queryFn: ({ pageParam, signal }) => apis.activities.listActivities({ cursor: pageParam, limit: 20 }, { signal }), getNextPageParam: (page) => page.nextCursor })
  const records = history.data?.pages.flatMap((page) => page.items).filter((item) => item.activityType === type) ?? []
  if (managingTemplates) return <StudyTemplateManager close={() => setManagingTemplates(null)} {...(type === 'WORKOUT'
    ? { domain: activity.TemplateDomain.Workout, kind: activity.TemplateKind.FreeWorkout, section: '운동', noun: '기록 유형' }
    : type === 'CLIMBING'
      ? { domain: activity.TemplateDomain.Climbing, kind: managingTemplates, section: '클라이밍', noun: managingTemplates === activity.TemplateKind.FreeHangboard ? '행보드 유형' : '세션 유형' }
      : {})} />
  return <section className="domain-page">
    <header className="domain-summary-heading"><div className="month-picker"><button aria-label="이전 달" onClick={previous}>‹</button><strong>{displayMonth(month)}</strong><button aria-label="다음 달" disabled={atCurrent} onClick={next}>›</button></div><div className="domain-actions">{type === 'STUDY' && <button onClick={() => setManagingTemplates(activity.TemplateKind.StudyCategory)}>카테고리 관리</button>}{type === 'WORKOUT' && <button onClick={() => setManagingTemplates(activity.TemplateKind.FreeWorkout)}>기록 유형 관리</button>}{type === 'CLIMBING' && <><button onClick={() => setManagingTemplates(activity.TemplateKind.ClimbingSession)}>세션 유형 관리</button><button onClick={() => setManagingTemplates(activity.TemplateKind.FreeHangboard)}>행보드 유형 관리</button></>}<button onClick={() => navigate(`/today?date=${today}&add=${type}`)}>추가</button><button onClick={() => setPastOpen((value) => !value)}>지난 기록</button></div></header>
    <section className="domain-summary" aria-label={`${title} 월간 요약`}>
      <div><span>완료</span>{activitySummary.isError ? <><strong>—</strong><p role="alert">활동 요약을 불러오지 못했습니다.</p><button onClick={() => void activitySummary.refetch()}>다시 시도</button></> : <strong>{activitySummary.data ? `${activitySummary.data.completedCount}회` : '…'}</strong>}</div>
      <div><span>시간</span>{activitySummary.isError ? <strong>—</strong> : <strong>{activitySummary.data ? `${Math.floor(activitySummary.data.durationSeconds / 60)}분` : '…'}</strong>}</div>
      <div><span>등록</span>{registration.isError ? <><strong>—</strong><p role="alert">등록 요약을 불러오지 못했습니다.</p><button onClick={() => void registration.refetch()}>다시 시도</button></> : <strong>{registeredCount === undefined ? '…' : `${registeredCount}개`}</strong>}</div>
    </section>
    {pastOpen && <section className="past-record"><label>지난 수행일<input type="date" max={today} value={pastDate} onChange={(event) => setPastDate(event.target.value)} /></label>{day.isPending && <p role="status">할 일을 불러오는 중…</p>}{day.isError && <p role="alert">할 일을 불러오지 못했습니다.</p>}{day.data?.tasks.filter((task) => task.taskType === type && task.status !== 'DELETED').map((task) => <button key={task.taskId} onClick={() => navigate(`/${type.toLowerCase()}?${task.status === 'COMPLETED' ? 'completedTaskId' : 'taskId'}=${task.taskId}`)}>{task.title} 기록</button>)}</section>}
    <section className="domain-history"><h2>기록</h2>{history.isPending && <p role="status">기록을 불러오는 중…</p>}{history.isError && <p role="alert">기록을 불러오지 못했습니다.</p>}{records.map((record) => <button className="history-row" key={record.activityId} onClick={() => navigate(`/${type.toLowerCase()}?activityId=${record.activityId}`)}><span>{performedDate(record.performedAt)}</span><strong>{record.note || `${title} 기록`}</strong><small>{record.status === 'VOIDED' ? '취소됨' : record.syncState}</small></button>)}{history.hasNextPage && <button onClick={() => void history.fetchNextPage()} disabled={history.isFetchingNextPage}>{history.isFetchingNextPage ? '불러오는 중…' : '더 불러오기'}</button>}</section>
  </section>
}
