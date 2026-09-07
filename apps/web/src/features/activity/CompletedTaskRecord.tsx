import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { activity } from '@todorok/api-client'
import { Navigate } from 'react-router'
import { useAuth } from '../auth/AuthProvider'

export function CompletedTaskRecord({ taskId, type }: { taskId: string; type: 'WORKOUT' | 'STUDY' | 'CLIMBING' }) {
  const { session, state } = useAuth()
  const api = useMemo(() => new activity.ActivityApi(new activity.Configuration({ basePath: '/api/activity/v1', fetchApi: session.fetch })), [session])
  const result = useQuery({ queryKey: ['completed-activity', state.userId, taskId, type], staleTime: 0, queryFn: async ({ signal }) => {
    let cursor: string | undefined
    const seen = new Set<string>()
    do {
      signal.throwIfAborted()
      const page = await api.listActivities({ limit: 100, cursor }, { signal })
      signal.throwIfAborted()
      const match = page.items.find((item) => item.taskId === taskId && item.activityType === type && item.status === 'COMPLETED')
      if (match) return match.activityId
      cursor = page.nextCursor
      if (cursor && seen.has(cursor)) throw new Error('Repeated activity cursor')
      if (cursor) seen.add(cursor)
    } while (cursor)
    return null
  } })
  if (result.isPending || result.isFetching) return <p role="status">완료 기록을 찾는 중…</p>
  if (result.isError || !result.data) return <section><p role="alert">완료 기록을 아직 확인하지 못했습니다. 동기화 상태를 확인한 뒤 다시 시도해 주세요.</p><button onClick={() => void result.refetch()}>기록 다시 찾기</button></section>
  return <Navigate to={`/${type.toLowerCase()}?activityId=${result.data}`} replace />
}
