import { useState } from 'react'
import type { planner } from '@todorok/api-client'
import { RecurrenceFields, type RepeatOptions } from './RecurrenceFields'
export function SeriesEditor({
  series,
  busy,
  save,
  archive,
}: {
  series: Readonly<planner.SeriesResponse>
  busy: boolean
  save: (request: planner.UpdateSeriesRequest) => void
  archive: () => void
}) {
  const [title, setTitle] = useState(series.title)
  const [note, setNote] = useState(series.note ?? '')
  const [repeat, setRepeat] = useState<RepeatOptions>({
    rule: series.rule,
    endDate: series.endDate,
  })
  if (series.archived)
    return <p>반복이 중단되었습니다. 이 할 일은 남아 있습니다.</p>
  return (
    <details className="task-form">
      <summary>반복 설정</summary>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          if (
            !busy &&
            (repeat.rule.frequency !== 'WEEKLY' || repeat.rule.weekdays.size)
          )
            save({
              title: title.trim(),
              note,
              ...repeat,
              version: series.version,
            })
        }}
      >
        <p>시작일 {series.startDate}. 수정 내용은 다음 회차부터 적용합니다.</p>
        <label htmlFor="series-title">반복 제목</label>
        <input
          id="series-title"
          required
          maxLength={120}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <label htmlFor="series-note">반복 메모</label>
        <textarea
          id="series-note"
          maxLength={20000}
          value={note}
          onChange={(e) => setNote(e.target.value)}
        />
        <label htmlFor="series-frequency">반복 종류</label>
        <select
          id="series-frequency"
          value={repeat.rule.frequency}
          onChange={(e) =>
            setRepeat({
              ...repeat,
              rule: {
                ...repeat.rule,
                frequency: e.target
                  .value as planner.RecurrenceRuleFrequencyEnum,
              },
            })
          }
        >
          <option value="DAILY">일마다</option>
          <option value="WEEKLY">주마다</option>
          <option value="MONTHLY">월마다</option>
        </select>
        <RecurrenceFields id="series-rule" value={repeat} change={setRepeat} />
        <div className="form-actions">
          <button type="submit" disabled={busy || !title.trim()}>
            반복 수정 저장
          </button>
          <button type="button" disabled={busy} onClick={archive}>
            반복 중단
          </button>
        </div>
      </form>
    </details>
  )
}
