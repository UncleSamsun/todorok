import { useState } from 'react'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { SessionClient } from '@todorok/api-client'
import { AuthProvider } from './features/auth/AuthProvider'
import { AppRouter } from './app/router'
import { createSessionQueryClient } from './app/query-client'
import './styles.css'

export function App({ session: suppliedSession, queryClient }: { session?: SessionClient; queryClient?: QueryClient }) {
  const [session] = useState(() => suppliedSession ?? new SessionClient())
  const [queries] = useState(() => queryClient ?? createSessionQueryClient())
  return <QueryClientProvider client={queries}><AuthProvider session={session}><AppRouter /></AuthProvider></QueryClientProvider>
}
