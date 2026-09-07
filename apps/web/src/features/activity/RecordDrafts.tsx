import { createContext, useContext, useRef, useState, type ReactNode } from 'react'
import type { activity } from '@todorok/api-client'
import { emptyTime, type TimeValue } from './RecordTimeFields'
import { CorrectionDraftProvider } from './CorrectionDrafts'

type Draft = {
  date: string; note: string; workout: activity.WorkoutSet[]; study: activity.StudyDetail; climbing: activity.ClimbingDetail
  start: TimeValue; end: TimeValue; snapshot: activity.CreateActivityRequest | null
  uncertain: boolean; blocked: boolean; error: string
}
const Drafts = createContext<Map<string, Draft> | null>(null)
export function RecordDraftProvider({ children }: { children: ReactNode }) {
  const [drafts] = useState(() => new Map<string, Draft>())
  return <Drafts.Provider value={drafts}><CorrectionDraftProvider>{children}</CorrectionDraftProvider></Drafts.Provider>
}
// Owned by the keyed authenticated shell: never persisted across sessions or reloads.
export function useRecordDraft(key: string) {
  const drafts = useContext(Drafts)
  if (!drafts) throw new Error('RecordDraftProvider is required')
  const [draft, setDraft] = useState<Draft>(() => drafts.get(key) ?? {
    date: '', note: '', workout: [], study: {}, climbing: {}, start: emptyTime(), end: emptyTime(),
    snapshot: null, uncertain: false, blocked: false, error: '',
  })
  const latest = useRef(draft)
  function update(patch: Partial<Draft>) {
    const next = { ...latest.current, ...patch }
    latest.current = next
    drafts!.set(key, next)
    setDraft(next)
  }
  return { draft, update, clear: () => drafts.delete(key) }
}
