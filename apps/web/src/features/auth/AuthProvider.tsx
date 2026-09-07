import { createContext, useContext, useEffect, useSyncExternalStore, type ReactNode } from 'react'
import { type SessionClient, type SessionSnapshot } from '@todorok/api-client'
import { useQueryClient } from '@tanstack/react-query'
const AuthContext = createContext<{ session: SessionClient; state: SessionSnapshot } | null>(null)
export function AuthProvider({ session, children }: { session: SessionClient; children: ReactNode }) {
  const queries = useQueryClient()
  const state = useSyncExternalStore(session.subscribe, session.getSnapshot)
  useEffect(() => {
    let generation = session.getSnapshot().generation
    const unsubscribe = session.subscribe(() => {
      const next = session.getSnapshot().generation
      if (generation !== next) { generation = next; queries.clear() }
    })
    const disconnect = session.connect()
    void session.restore()
    return () => { unsubscribe(); disconnect() }
  }, [session, queries])
  return <AuthContext.Provider value={{ session, state }}>{children}</AuthContext.Provider>
}
export function useAuth() {
  const auth = useContext(AuthContext)
  if (!auth) throw new Error('AuthProvider is required')
  return auth
}
