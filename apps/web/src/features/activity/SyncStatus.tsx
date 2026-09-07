import { activity } from '@todorok/api-client'
const labels = { PENDING: '기록됨 · 일정 반영 중', APPLIED: '일정 반영 완료', CONFLICT: '기록됨 · 일정과 충돌', NOT_REQUIRED: '기록됨 · 일정 반영 불필요' } as const
export function SyncStatus({ value, checking, check }: { value: activity.ActivityResponse; checking: boolean; check: () => void }) {
  const state = value.syncState && value.syncState !== activity.ActivitySyncState.UnknownDefaultOpenApi ? value.syncState : activity.ActivitySyncState.Pending
  return <section className={`sync-status ${state}`} aria-live="polite"><strong>{labels[state]}</strong>{value.syncReason && <p>{value.syncReason}</p>}{state !== 'APPLIED' && <button type="button" disabled={checking} onClick={check}>{checking ? '확인 중…' : '상태 다시 확인'}</button>}</section>
}
