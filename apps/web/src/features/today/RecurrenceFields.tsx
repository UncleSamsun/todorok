import { planner } from '@todorok/api-client'
import { isoWeekday } from '@todorok/client-domain'
export type RepeatOptions = { rule: planner.RecurrenceRule; endDate?: string }
export const initialRule = (date: string): planner.RecurrenceRule => ({
  frequency: planner.RecurrenceRuleFrequencyEnum.Daily,
  interval: 1,
  weekdays: new Set([isoWeekday(date)]),
  monthDay: Number(date.slice(8, 10)),
})
export function RecurrenceFields({
  id,
  value,
  change,
}: {
  id: string
  value: RepeatOptions
  change: (value: RepeatOptions) => void
}) {
  const { rule } = value
  const update = (patch: Partial<planner.RecurrenceRule>) =>
    change({ ...value, rule: { ...rule, ...patch } })
  return (
    <>
      <label htmlFor={`${id}-interval`}>반복 간격</label>
      <input
        id={`${id}-interval`}
        type="number"
        min={1}
        max={365}
        required
        value={rule.interval}
        onChange={(e) => update({ interval: Number(e.target.value) })}
      />
      {rule.frequency === 'WEEKLY' && (
        <fieldset className="recurrence-weekdays">
          <legend>반복 요일</legend>
          {['월', '화', '수', '목', '금', '토', '일'].map((day, i) => (
            <label key={day}>
              <input
                type="checkbox"
                aria-label={`${day}요일`}
                checked={rule.weekdays.has(i + 1)}
                onChange={(e) =>
                  update({
                    weekdays: new Set(
                      e.target.checked
                        ? [...rule.weekdays, i + 1].sort((a, b) => a - b)
                        : [...rule.weekdays].filter((d) => d !== i + 1),
                    ),
                  })
                }
              />
              {day}
            </label>
          ))}
          {!rule.weekdays.size && (
            <p role="alert">요일을 하나 이상 선택해 주세요.</p>
          )}
        </fieldset>
      )}
      {rule.frequency === 'MONTHLY' && (
        <>
          <label htmlFor={`${id}-month-day`}>반복 일자</label>
          <input
            id={`${id}-month-day`}
            type="number"
            min={1}
            max={31}
            required
            value={rule.monthDay}
            onChange={(e) => update({ monthDay: Number(e.target.value) })}
          />
          <p>해당 일자가 없는 달은 건너뜁니다.</p>
        </>
      )}
      <label htmlFor={`${id}-end`}>반복 종료일</label>
      <input
        id={`${id}-end`}
        type="date"
        value={value.endDate ?? ''}
        onChange={(e) =>
          change({ ...value, endDate: e.target.value || undefined })
        }
      />
    </>
  )
}
