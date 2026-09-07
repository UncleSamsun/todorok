import { lazy, Suspense, useEffect, useState } from 'react'
import {
  BrowserRouter,
  Navigate,
  NavLink,
  Route,
  Routes,
  useNavigate,
} from 'react-router'
import { useAuth } from '../features/auth/AuthProvider'
import { LoginPage } from '../features/auth/LoginPage'
import { TodayPage } from '../features/today/TodayPage'
export { seoulToday } from '@todorok/client-domain'
const Workout = lazy(() => import('../features/workout/WorkoutPage'))
const Study = lazy(() => import('../features/study/StudyPage'))
const Climbing = lazy(() => import('../features/climbing/ClimbingPage'))
const Settings = lazy(() => import('../features/settings/SettingsPage'))
function ProtectedShell() {
  const { session } = useAuth()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function logout() {
    if (busy) return
    setBusy(true)
    setError('')
    try {
      await session.logout()
    } catch {
      setError(
        '로그아웃을 완료하지 못했습니다. 연결을 확인한 뒤 다시 시도해 주세요.',
      )
    } finally {
      setBusy(false)
    }
  }
  return (
    <div className="workspace-shell">
      <header className="app-header">
        <div>
          <span className="brand">토도록</span>
          <p className="brand-subtitle">
            할 일은 가볍게, 기록은 필요할 때 자세하게.
          </p>
        </div>
        <button disabled={busy} onClick={logout}>
          {busy ? '로그아웃 중…' : '로그아웃'}
        </button>
      </header>
      <nav aria-label="주요 메뉴">
        {[
          ['today', '오늘'],
          ['workout', '운동'],
          ['study', '공부'],
          ['climbing', '클라이밍'],
          ['settings', '설정'],
        ].map(([path, title]) => (
          <NavLink key={path} to={`/${path}`}>
            {title}
          </NavLink>
        ))}
      </nav>
      {error && <p role="alert">{error}</p>}
      <main>
        <Suspense fallback={<p role="status">화면을 불러오는 중…</p>}>
          <Routes>
            <Route path="/today" element={<TodayPage />} />
            <Route path="/workout" element={<Workout />} />
            <Route path="/study" element={<Study />} />
            <Route path="/climbing" element={<Climbing />} />
            <Route path="/settings" element={<Settings />} />
            <Route path="*" element={<Navigate to="/today" replace />} />
          </Routes>
        </Suspense>
      </main>
    </div>
  )
}
function SessionRoutes() {
  const { state } = useAuth()
  const [entered, setEntered] = useState<number | null>(null)
  if (state.status === 'restoring')
    return (
      <main className="login-shell">
        <p role="status">세션을 확인하는 중…</p>
      </main>
    )
  if (state.status === 'anonymous')
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  if (entered !== state.generation)
    return <EnterSession generation={state.generation} onEnter={setEntered} />
  return <ProtectedShell key={state.generation} />
}
function EnterSession({
  generation,
  onEnter,
}: {
  generation: number
  onEnter: (generation: number) => void
}) {
  const navigate = useNavigate()
  useEffect(() => {
    void navigate('/today', { replace: true })
    onEnter(generation)
  }, [generation, onEnter, navigate])
  return <p role="status">오늘을 여는 중…</p>
}
export function AppRouter() {
  return (
    <BrowserRouter>
      <SessionRoutes />
    </BrowserRouter>
  )
}
