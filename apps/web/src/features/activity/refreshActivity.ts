import type { QueryClient } from '@tanstack/react-query'

export function refreshActivity(queries: QueryClient, owner: string | null, calendar: boolean) {
  return queries.invalidateQueries({ predicate: ({ queryKey }) =>
    (calendar && queryKey[0] === 'calendar') ||
    (queryKey[1] === owner && ['activities', 'activity-summary', 'calendar-summary'].includes(String(queryKey[0]))) })
}
