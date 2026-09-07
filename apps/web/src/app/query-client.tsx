import { QueryClient } from '@tanstack/react-query'
export function createSessionQueryClient() {
  return new QueryClient({ defaultOptions: {
    queries: { retry: false, staleTime: 30_000, refetchOnWindowFocus: false },
    mutations: { retry: false },
  } })
}
