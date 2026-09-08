import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { planner } from '@todorok/api-client'
import { useAuth } from '../auth/AuthProvider'

type PreferenceValue = Pick<planner.UserPreferencesResponse, 'theme' | 'notificationsEnabled' | 'summaryTime' | 'revision'>
type ThemeContextValue = PreferenceValue & { setTheme: (theme: planner.ThemeMode) => void; setNotifications: (enabled: boolean, summaryTime?: string) => void; saving: boolean; error: string }
const ThemeContext = createContext<ThemeContextValue | null>(null)

export function ThemeProvider({ children }: { children: ReactNode }) {
  const { session, state } = useAuth(), queries = useQueryClient()
  const api = useMemo(() => new planner.PreferenceApi(new planner.Configuration({ basePath: '/api/planner/v1', fetchApi: session.fetch })), [session])
  const preferences = useQuery({ queryKey: ['preferences', state.userId], queryFn: ({ signal }) => api.getPreferences({ signal }) })
  const [value, setValue] = useState<PreferenceValue>({ theme: planner.ThemeMode.System, notificationsEnabled: false, summaryTime: '08:00', revision: 0 }), [error, setError] = useState('')
  useEffect(() => { if (preferences.data) setValue(preferences.data) }, [preferences.data?.revision])
  useEffect(() => {
    if (value.theme === planner.ThemeMode.System) delete document.documentElement.dataset.theme
    else document.documentElement.dataset.theme = value.theme.toLowerCase()
  }, [value.theme])
  const update = useMutation({
    mutationFn: (next: PreferenceValue) => api.updatePreferences({ updateUserPreferencesRequest: { theme: next.theme, notificationsEnabled: next.notificationsEnabled, summaryTime: next.summaryTime, expectedRevision: next.revision } }),
    onSuccess: (saved) => { queries.setQueryData(['preferences', state.userId], saved); setValue(saved); setError('') },
  })
  function save(next: PreferenceValue) {
    if (!preferences.data || update.isPending) return
    const previous = value
    setValue(next); setError('')
    update.mutate(next, { onError: () => { setValue(previous); setError('설정을 저장하지 못했습니다. 다시 시도해 주세요.') } })
  }
  function setTheme(theme: planner.ThemeMode) { if (theme !== value.theme) save({ ...value, theme }) }
  function setNotifications(notificationsEnabled: boolean, summaryTime = value.summaryTime) { if (notificationsEnabled !== value.notificationsEnabled || summaryTime !== value.summaryTime) save({ ...value, notificationsEnabled, summaryTime }) }
  return <ThemeContext.Provider value={{ ...value, setTheme, setNotifications, saving: update.isPending, error }}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const value = useContext(ThemeContext)
  if (!value) throw new Error('ThemeProvider is required')
  return value
}
