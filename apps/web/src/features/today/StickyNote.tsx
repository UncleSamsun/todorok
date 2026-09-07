import { useEffect, useReducer, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import type { planner } from '@todorok/api-client'
import { validNote } from '@todorok/validation'

type NoteApi = Pick<planner.NoteApi, 'getDailyNote' | 'updateDailyNote'>
type Draft = {
  content: string
  version: number | null
  revision: number
  sent: number
  outcome: number
  status: 'idle' | 'dirty' | 'saving' | 'saved' | 'error'
}

export function StickyNote({
  date,
  api,
  isCurrent = () => true,
}: {
  date: string
  api: NoteApi
  isCurrent?: () => boolean
}) {
  const queries = useQueryClient(),
    drafts = useRef(new Map<string, Draft>()),
    mounted = useRef(true)
  const [, refresh] = useReducer((n: number) => n + 1, 0),
    [expanded, setExpanded] = useState(false)
  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
      drafts.current.clear()
    }
  }, [])
  const note = useQuery({
    queryKey: ['note', date],
    queryFn: ({ signal }) => api.getDailyNote({ date }, { signal }),
    retry: false,
  })
  useEffect(() => {
    const current = drafts.current.get(date)
    const clean = current?.status === 'idle' || current?.status === 'saved'
    if (
      note.data &&
      (!current ||
        (clean && (note.data.version ?? -1) > (current.version ?? -1)))
    ) {
      drafts.current.set(date, {
        content: note.data.content,
        version: note.data.version,
        revision: 0,
        sent: 0,
        outcome: 0,
        status: 'idle',
      })
      refresh()
    }
  }, [date, note.data])
  const draft = drafts.current.get(date)
  function cache(result: planner.DailyNoteResponse) {
    queries.setQueryData<planner.DailyNoteResponse>(
      ['note', result.date],
      (old) =>
        old && (old.version ?? -1) > (result.version ?? -1) ? old : result,
    )
  }
  async function save(requestDate: string, item: Draft, retry = false) {
    if (!mounted.current || !isCurrent()) return
    item.status = 'saving'
    refresh()
    let revision = item.revision
    try {
      if (retry) {
        const latest = await api.getDailyNote({ date: requestDate })
        if (!mounted.current || !isCurrent()) return
        item.version = latest.version
        cache(latest)
      }
      // Read the current buffer after retry's GET; remote content never replaces it.
      revision = item.revision
      const content = item.content,
        expectedVersion = item.version
      item.sent = revision
      const result = await api.updateDailyNote({
        date: requestDate,
        updateDailyNoteRequest: { content, expectedVersion },
      })
      if (!mounted.current || !isCurrent()) return
      cache(result)
      if ((result.version ?? -1) > (item.version ?? -1))
        item.version = result.version
      if (revision >= item.outcome) {
        item.outcome = revision
        item.status = revision === item.revision ? 'saved' : 'dirty'
      }
      refresh()
    } catch {
      if (!mounted.current || !isCurrent()) return
      if (revision >= item.outcome) {
        item.outcome = revision
        item.status = 'error'
      }
      refresh()
    }
  }
  useEffect(() => {
    if (!draft || draft.status !== 'dirty' || draft.sent >= draft.revision)
      return
    const timer = setTimeout(() => {
      void save(date, draft)
    }, 500)
    return () => clearTimeout(timer)
  }, [date, draft?.revision, draft?.status])
  return (
    <section className="sticky-note" aria-label="오늘의 스티키노트">
      <button
        type="button"
        className="note-preview"
        aria-expanded={expanded}
        onClick={() => setExpanded(!expanded)}
      >
        스티키노트 ·{' '}
        {draft?.content.split('\n')[0] || '오늘의 생각을 남겨 보세요'}
      </button>
      {note.isError && !draft && (
        <p role="alert">
          메모를 불러오지 못했습니다.{' '}
          <button onClick={() => void note.refetch()}>
            메모 다시 불러오기
          </button>
        </p>
      )}
      {expanded &&
        (draft ? (
          <>
            <label htmlFor="daily-note">날짜 메모</label>
            <textarea
              id="daily-note"
              rows={6}
              maxLength={20000}
              value={draft.content}
              onChange={(e) => {
                if (!validNote(e.target.value)) return
                draft.content = e.target.value
                draft.revision++
                draft.status = 'dirty'
                refresh()
              }}
            />
            {draft.status === 'error' ? (
              <p role="alert">
                ⚠ 저장 실패 · 입력한 내용은 유지됩니다.{' '}
                <button onClick={() => void save(date, draft, true)}>
                  저장 다시 시도
                </button>
              </p>
            ) : (
              <p role="status">
                {draft.status === 'saved'
                  ? '✓ 저장됨'
                  : draft.status === 'saving'
                    ? '◷ 저장 중'
                    : draft.status === 'dirty'
                      ? '◷ 저장 대기'
                      : '날짜별로 자동 저장됩니다.'}
              </p>
            )}
          </>
        ) : (
          <p role="status">메모를 불러오는 중…</p>
        ))}
    </section>
  )
}
