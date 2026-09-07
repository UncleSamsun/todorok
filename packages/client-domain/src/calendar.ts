/** Civil-date arithmetic uses UTC only; API dates never pass through host-local time. */
function date(value: string) {
  return new Date(`${value}T00:00:00Z`)
}
function iso(value: Date) {
  return value.toISOString().slice(0, 10)
}
export function addDays(value: string, days: number) {
  const d = date(value)
  d.setUTCDate(d.getUTCDate() + days)
  return iso(d)
}
export function addMonths(value: string, months: number) {
  const d = date(value),
    day = d.getUTCDate()
  d.setUTCDate(1)
  d.setUTCMonth(d.getUTCMonth() + months)
  const last = new Date(
    Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 0),
  ).getUTCDate()
  d.setUTCDate(Math.min(day, last))
  return iso(d)
}
export function weekDays(value: string) {
  const start = addDays(value, -date(value).getUTCDay())
  return Array.from({ length: 7 }, (_, i) => addDays(start, i))
}
export function monthDays(value: string) {
  const first = `${value.slice(0, 7)}-01`,
    start = weekDays(first)[0]!
  const next = addMonths(first, 1),
    last = addDays(next, -1)
  const end = weekDays(last)[6]!
  const length =
    Math.round((date(end).getTime() - date(start).getTime()) / 86400000) + 1
  return Array.from({ length }, (_, i) => addDays(start, i))
}
export function weekOfMonth(value: string) {
  return (
    Math.floor(
      (date(value).getUTCDate() -
        1 +
        date(`${value.slice(0, 7)}-01`).getUTCDay()) /
        7,
    ) + 1
  )
}
export function seoulToday(now = new Date()) {
  return new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Seoul' }).format(
    now,
  )
}
