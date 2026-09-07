import { createContext, useContext, useRef, useState, type ReactNode } from 'react'
import type { activity, ActivityCorrection, EditableActivity } from '@todorok/api-client'
import { emptyTime, type TimeValue } from './RecordTimeFields'
export type CorrectionOperation = { kind: 'patch'; body: ActivityCorrection } | { kind: 'void'; body: { version: number; reason: string } }
export type CorrectionDraft = {
  baseline: EditableActivity | null; date: string; note: string; workout: activity.WorkoutSet[]; study: activity.StudyDetail; climbing: activity.ClimbingDetail
  start: TimeValue; end: TimeValue; startDirty: boolean; endDirty: boolean; dateDirty: boolean
  operation: CorrectionOperation | null; mode: 'ready' | 'uncertain' | 'conflict'; error: string
}
const Drafts = createContext<Map<string, CorrectionDraft> | null>(null)
export function CorrectionDraftProvider({ children }: { children: ReactNode }) {
  const [drafts] = useState(() => new Map<string, CorrectionDraft>())
  return <Drafts.Provider value={drafts}>{children}</Drafts.Provider>
}
export function useCorrectionDraft(id: string) {
  const drafts = useContext(Drafts)
  if (!drafts) throw new Error('CorrectionDraftProvider is required')
  const [draft, setDraft] = useState<CorrectionDraft>(() => drafts.get(id) ?? { baseline: null, date: '', note: '', workout: [], study: {}, climbing: {}, start: emptyTime(), end: emptyTime(), startDirty: false, endDirty: false, dateDirty: false, operation: null, mode: 'ready', error: '' })
  const latest = useRef(draft)
  function update(patch: Partial<CorrectionDraft>) {
    const next = { ...latest.current, ...patch }
    latest.current = next
    drafts!.set(id, next)
    setDraft(next)
  }
  return { draft, update }
}
