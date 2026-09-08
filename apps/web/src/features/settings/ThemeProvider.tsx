import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { planner } from '@todorok/api-client'
import { useAuth } from '../auth/AuthProvider'

type ThemeContextValue = { theme: planner.ThemeMode; setTheme: (theme: planner.ThemeMode) => void; saving: boolean; error: string }
const ThemeContext = createContext<ThemeContextValue | null>(null)

export function ThemeProvider({ children }: { children: ReactNode }) {
  const { session, state } = useAuth(), queries = useQueryClient()
  const api = useMemo(() => new planner.PreferenceApi(new planner.Configuration({ basePath: '/api/planner/v1', fetchApi: session.fetch })), [session])
  const preferences = useQuery({ queryKey: ['preferences', state.userId], queryFn: ({ signal }) => api.getPreferences({ signal }) })
  const [theme, setLocalTheme] = useState(planner.ThemeMode.System), [error, setError] = useState('')
  useEffect(() => { if (preferences.data) setLocalTheme(preferences.data.theme) }, [preferences.data?.revision])
  useEffect(() => {
    if (theme === planner.ThemeMode.System) delete document.documentElement.dataset.theme
    else document.documentElement.dataset.theme = theme.toLowerCase()
  }, [theme])
  const update = useMutation({
    mutationFn: ({ theme, revision }: { theme: planner.ThemeMode; revision: number }) => api.updatePreferences({ updateUserPreferencesRequest: { theme, expectedRevision: revision } }),
    onSuccess: (saved) => { queries.setQueryData(['preferences', state.userId], saved); setLocalTheme(saved.theme); setError('') },
  })
  function setTheme(next: planner.ThemeMode) {
    const current = preferences.data
    if (!current || update.isPending || next === theme) return
    const previous = theme
    setLocalTheme(next); setError('')
    update.mutate({ theme: next, revision: current.revision }, { onError: () => { setLocalTheme(previous); setError('테마를 저장하지 못했습니다. 다시 시도해 주세요.') } })
  }
  return <ThemeContext.Provider value={{ theme, setTheme, saving: update.isPending, error }}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const value = useContext(ThemeContext)
  if (!value) throw new Error('ThemeProvider is required')
  return value
}
