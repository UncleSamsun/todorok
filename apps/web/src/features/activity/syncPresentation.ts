import { activity } from '@todorok/api-client'

export const syncLabels = { PENDING: '기록됨 · 일정 반영 중', APPLIED: '일정 반영 완료', CONFLICT: '기록됨 · 일정과 충돌', NOT_REQUIRED: '기록됨 · 일정 반영 불필요' } as const

export function displaySyncState(state: activity.ActivitySyncState | undefined) {
  return state && state !== activity.ActivitySyncState.UnknownDefaultOpenApi ? state : activity.ActivitySyncState.Pending
}

const reasons: Record<string, string> = {
  TASK_NOT_FOUND: '연결된 할 일을 찾을 수 없습니다. 오늘 화면에서 할 일을 확인해 주세요.',
  TASK_DELETED: '연결된 할 일이 삭제되어 일정에 반영하지 못했습니다.',
  TASK_SKIPPED: '연결된 할 일이 건너뛰기로 표시되어 있습니다. 오늘 화면에서 확인해 주세요.',
  TASK_TYPE_MISMATCH: '기록과 연결된 할 일의 종류가 다릅니다. 오늘 화면에서 확인해 주세요.',
  OTHER_ACTIVITY_LINKED: '이 할 일에는 다른 기록이 연결되어 있습니다. 오늘 화면에서 확인해 주세요.',
  TASK_ALREADY_COMPLETED: '연결된 할 일이 이미 완료되어 있습니다. 오늘 화면에서 확인해 주세요.',
  ACTIVE_OCCURRENCE_EXISTS: '같은 반복 일정의 다른 할 일이 진행 중입니다. 오늘 화면에서 확인해 주세요.',
}

export function syncExplanation(state: ReturnType<typeof displaySyncState>, reason: string | undefined) {
  if (!reason || state === 'APPLIED' || state === 'NOT_REQUIRED') return null
  if (state === 'CONFLICT') return reasons[reason] ?? '기록은 저장되었지만 일정에 반영하지 못했습니다. 오늘 화면에서 할 일 상태를 확인해 주세요.'
  return '기록은 저장되었습니다. 일정에 반영될 때까지 잠시 기다려 주세요.'
}
