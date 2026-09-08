import { useState } from 'react'
import { planner } from '@todorok/api-client'
import { useTheme } from './ThemeProvider'
export default function SettingsPage() {
  const { theme, setTheme, saving, error } = useTheme()
  const [view, setView] = useState(() => { try { return localStorage.getItem('todorok.calendar-view') === 'month' ? 'month' : 'week' } catch { return 'week' } })
  function change(value: string) { setView(value); try { localStorage.setItem('todorok.calendar-view', value) } catch { /* preference is optional */ } }
  return <section className="settings-page"><h1>설정</h1><label htmlFor="theme">테마</label><select id="theme" value={theme} disabled={saving} onChange={event => setTheme(event.target.value as planner.ThemeMode)}><option value="SYSTEM">시스템 설정</option><option value="LIGHT">라이트</option><option value="DARK">다크</option></select>{error && <p role="alert">{error}</p>}<label htmlFor="calendar-view">기본 달력 보기</label><select id="calendar-view" value={view} onChange={event => change(event.target.value)}><option value="week">주 보기</option><option value="month">월 보기</option></select></section>
}
