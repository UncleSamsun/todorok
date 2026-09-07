import { countLabel, type Summary } from './model'
export function MonthCalendar({
  days,
  selected,
  summaries,
  select,
}: {
  days: string[]
  selected: string
  summaries: Map<string, Summary>
  select: (date: string) => void
}) {
  return (
    <div className="calendar-scroll">
      <div className="month-grid">
        {[...'일월화수목금토'].map((d) => (
          <small key={d}>{d}</small>
        ))}
        {days.map((day) => (
          <button
            key={day}
            className={`date-button ${day === selected ? 'selected' : ''} ${day.slice(0, 7) !== selected.slice(0, 7) ? 'outside-month' : ''}`}
            aria-pressed={day === selected}
            aria-label={countLabel(day, summaries.get(day))}
            onClick={() => select(day)}
          >
            {Number(day.slice(-2))}
            <span className="month-progress">
              {summaries
                .get(day)
                ?.categoryProgress.filter((p) => p.completedCount > 0)
                .map((p) => (
                  <span key={p.taskType} className={p.taskType} />
                ))}
            </span>
          </button>
        ))}
      </div>
    </div>
  )
}
