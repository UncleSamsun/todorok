import { useState, type FormEvent } from 'react'
import { SessionError } from '@todorok/api-client'
import { useAuth } from './AuthProvider'
import { loginPreferences, rememberLogin } from './loginPreferences'
export function LoginPage() {
  const { session, state } = useAuth()
  const [preferences] = useState(loginPreferences)
  const [email, setEmail] = useState(preferences.email)
  const [rememberId, setRememberId] = useState(Boolean(preferences.email))
  const [autoLogin, setAutoLogin] = useState(preferences.autoLogin)
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    setBusy(true); setError('')
    try { await session.login(email.trim(), password); rememberLogin(email.trim(), rememberId, autoLogin) }
    catch (failure) {
      const status = failure instanceof SessionError ? failure.status : 0
      setError(status === 401 ? '이메일과 비밀번호를 확인해 주세요.' : status === 429 ? '로그인 시도가 많습니다. 잠시 후 다시 시도해 주세요.' : '연결을 확인하고 다시 시도해 주세요.')
    } finally { setBusy(false) }
  }
  return <main className="login-shell"><section className="login-card" aria-labelledby="product-name">
    <p className="eyebrow">오늘을 계획하고 기록하는 한곳</p><h1 id="product-name">토도록</h1>
    <p className="description">나의 하루를 이어서 기록해 보세요.</p>
    {state.error === 'network' && <p role="status">세션을 확인하지 못했습니다. 연결을 확인한 뒤 로그인해 주세요.</p>}
    <form onSubmit={submit}>
      <label htmlFor="email">이메일</label><input id="email" name="email" type="email" autoComplete="username" required value={email} onChange={event => setEmail(event.target.value)} />
      <label htmlFor="password">비밀번호</label><input id="password" name="password" type="password" autoComplete="current-password" required value={password} onChange={event => setPassword(event.target.value)} />
      <div className="login-options">
        <label><input type="checkbox" checked={rememberId} onChange={event => setRememberId(event.target.checked)} />아이디 저장</label>
        <label><input type="checkbox" checked={autoLogin} onChange={event => setAutoLogin(event.target.checked)} />자동 로그인</label>
      </div>
      {error && <p role="alert">{error}</p>}
      <button type="submit" disabled={busy}>{busy ? '로그인 중…' : '로그인'}</button>
    </form>
  </section></main>
}
