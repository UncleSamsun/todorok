import { weekOfMonth } from '@todorok/client-domain'
import { countLabel, groups, type Summary } from './model'
export function WeekCalendar({
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
      <div className="week-grid">
        <span className="week-label">{weekOfMonth(selected)}주차</span>
        {days.map((day, i) => (
          <button
            key={day}
            className={`date-button ${day === selected ? 'selected' : ''}`}
            aria-pressed={day === selected}
            aria-label={countLabel(day, summaries.get(day))}
            onClick={() => select(day)}
          >
            <small>{'일월화수목금토'[i]}</small>
            {Number(day.slice(-2))}
          </button>
        ))}
        {groups.map(([type, label]) => (
          <div className="week-category" key={type}>
            <span>{label}</span>
            {days.map((day) => {
              const progress = summaries
                  .get(day)
                  ?.categoryProgress.find((p) => p.taskType === type),
                total = progress?.totalCount ?? 0,
                complete = progress?.completedCount ?? 0
              return (
                <div
                  className={`progress-cell ${type}`}
                  role="img"
                  key={day}
                  aria-label={`${day} ${label}, 완료 ${complete}개 / 전체 ${total}개`}
                >
                  <span
                    style={{
                      width: `${total ? (complete / total) * 100 : 0}%`,
                    }}
                  />
                </div>
              )
            })}
          </div>
        ))}
      </div>
    </div>
  )
}
