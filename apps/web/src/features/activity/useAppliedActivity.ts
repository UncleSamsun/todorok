import { useEffect, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import type { activity } from '@todorok/api-client'
import { useAuth } from '../auth/AuthProvider'
import { refreshActivity } from './refreshActivity'

export function useAppliedActivity(value: activity.ActivityResponse | undefined, activityId: string) {
  const { session, state } = useAuth(), queries = useQueryClient(), applied = useRef(new Set<string>())
  useEffect(() => {
    if (value?.syncState !== 'APPLIED' || value.activityId !== activityId || session.getSnapshot().generation !== state.generation) return
    const identity = `${state.generation}:${activityId}:${value.version}`
    if (applied.current.has(identity)) return
    applied.current.add(identity)
    void refreshActivity(queries, state.userId, true)
  }, [activityId, queries, value, session, state.generation, state.userId])
}
