import { QueryClient } from '@tanstack/react-query'
import { expect, it } from 'vitest'
import { refreshActivity } from './refreshActivity'

it('refreshes the completing owner’s program progress without invalidating another owner', async () => {
  const queries = new QueryClient()
  queries.setQueryData(['program-enrollments', 'owner'], [])
  queries.setQueryData(['program-enrollments', 'other'], [])
  await refreshActivity(queries, 'owner', false)
  expect(queries.getQueryState(['program-enrollments', 'owner'])?.isInvalidated).toBe(true)
  expect(queries.getQueryState(['program-enrollments', 'other'])?.isInvalidated).toBe(false)
})
