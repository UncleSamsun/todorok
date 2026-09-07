import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { planner } from '@todorok/api-client'
import { WeekCalendar } from './WeekCalendar'
import { MonthCalendar } from './MonthCalendar'
afterEach(cleanup)
const day: planner.CalendarDaySummary = {
  date: '2026-09-07',
  totalCount: 3,
  completedCount: 1,
  categoryProgress: [
    { taskType: planner.TaskType.General, totalCount: 2, completedCount: 1 },
    { taskType: planner.TaskType.Workout, totalCount: 1, completedCount: 0 },
    { taskType: planner.TaskType.Study, totalCount: 0, completedCount: 0 },
    { taskType: planner.TaskType.Climbing, totalCount: 0, completedCount: 0 },
  ],
}
it('fills a half-complete weekly category by half and exposes exact counts', () => {
  render(
    <WeekCalendar
      days={['2026-09-07']}
      selected="2026-09-07"
      summaries={new Map([[day.date, day]])}
      select={() => {}}
    />,
  )
  const cell = screen.getByRole('img', {
    name: '2026-09-07 할 일, 완료 1개 / 전체 2개',
  })
  expect(cell.firstElementChild).toHaveStyle({ width: '50%' })
  expect(cell).not.toHaveClass('selected')
  expect(
    screen.getByRole('button', { name: '2026-09-07, 완료 1개 / 전체 3개' }),
  ).toHaveAttribute('aria-pressed', 'true')
})
it('shows only completed categories inside the monthly bar', () => {
  const { container } = render(
    <MonthCalendar
      days={['2026-09-07']}
      selected="2026-09-07"
      summaries={new Map([[day.date, day]])}
      select={() => {}}
    />,
  )
  expect(container.querySelectorAll('.month-progress span')).toHaveLength(1)
  expect(container.querySelector('.month-progress span')).toHaveClass('GENERAL')
})
