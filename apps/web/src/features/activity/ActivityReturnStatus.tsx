import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { activity } from '@todorok/api-client'
import { useAuth } from '../auth/AuthProvider'
import { SyncStatus } from './SyncStatus'
import { useAppliedActivity } from './useAppliedActivity'

export function ActivityReturnStatus({ activityId }: { activityId: string }) {
  const { session } = useAuth()
  const api = useMemo(() => new activity.ActivityApi(new activity.Configuration({ basePath: '/api/activity/v1', fetchApi: session.fetch })), [session])
  const result = useQuery({ queryKey: ['activity', activityId], queryFn: ({ signal }) => api.getActivity({ activityId }, { signal }), refetchInterval: (q) => q.state.data?.syncState === activity.ActivitySyncState.Pending ? 1500 : false })
  useAppliedActivity(result.data, activityId)
  if (result.isPending) return <p role="status">기록 상태를 불러오는 중…</p>
  if (result.isError) return <p role="alert">기록 상태를 불러오지 못했습니다. <button onClick={() => void result.refetch()}>다시 불러오기</button></p>
  return <SyncStatus value={result.data} checking={result.isFetching} check={() => { void result.refetch() }}/>
}
