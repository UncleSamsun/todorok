import { describe, it, expect } from 'vitest'
import {
  addDays,
  addMonths,
  monthDays,
  weekDays,
  weekOfMonth,
  seoulToday,
  isoWeekday,
} from '@todorok/client-domain'
describe('calendar civil dates', () => {
  it('crosses leap years and year boundaries without local timezone shifts', () => {
    expect(addDays('2024-02-28', 1)).toBe('2024-02-29')
    expect(isoWeekday('2026-09-06')).toBe(7)
    expect(isoWeekday('2026-09-07')).toBe(1)
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01')
    expect(addMonths('2024-01-31', 1)).toBe('2024-02-29')
    expect(addMonths('2026-12-31', 1)).toBe('2027-01-31')
    expect(addMonths('2026-03-31', -1)).toBe('2026-02-28')
    expect(weekDays('2027-01-01')).toEqual([
      '2026-12-27',
      '2026-12-28',
      '2026-12-29',
      '2026-12-30',
      '2026-12-31',
      '2027-01-01',
      '2027-01-02',
    ])
    expect(monthDays('2026-08-31')).toHaveLength(42)
    expect(weekOfMonth('2026-08-31')).toBe(6)
    expect(seoulToday(new Date('2026-12-31T15:00:00Z'))).toBe('2027-01-01')
  })
})
