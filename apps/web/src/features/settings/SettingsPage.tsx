import { useState } from 'react'
export default function SettingsPage() {
  const [view, setView] = useState(() => { try { return localStorage.getItem('todorok.calendar-view') === 'month' ? 'month' : 'week' } catch { return 'week' } })
  function change(value: string) { setView(value); try { localStorage.setItem('todorok.calendar-view', value) } catch { /* preference is optional */ } }
  return <section><h1>설정</h1><label htmlFor="calendar-view">기본 달력 보기</label><select id="calendar-view" value={view} onChange={event => change(event.target.value)}><option value="week">주 보기</option><option value="month">월 보기</option></select></section>
}
