import { activity } from '@todorok/api-client'
import { displaySyncState, syncExplanation, syncLabels } from './syncPresentation'
export function SyncStatus({ value, checking, check }: { value: activity.ActivityResponse; checking: boolean; check: () => void }) {
  const state = displaySyncState(value.syncState), explanation = syncExplanation(state, value.syncReason)
  return <section className={`sync-status ${state}`} aria-live="polite"><strong>{syncLabels[state]}</strong>{explanation && <p>{explanation}</p>}{(state === 'PENDING' || state === 'CONFLICT') && <button type="button" disabled={checking} onClick={check}>{checking ? '확인 중…' : '상태 다시 확인'}</button>}</section>
}
