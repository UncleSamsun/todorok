const EMAIL = 'todorok.login.email'
const AUTO = 'todorok.login.auto'
const ACTIVE = 'todorok.login.active'

export function loginPreferences() {
  try { return { email: localStorage.getItem(EMAIL) ?? '', autoLogin: localStorage.getItem(AUTO) !== 'false' } }
  catch { return { email: '', autoLogin: true } }
}
export function rememberLogin(email: string, rememberId: boolean, autoLogin: boolean) {
  try {
    if (rememberId) localStorage.setItem(EMAIL, email)
    else localStorage.removeItem(EMAIL)
    localStorage.setItem(AUTO, String(autoLogin))
  } catch { /* Storage restrictions must not prevent signing in. */ }
  try { sessionStorage.setItem(ACTIVE, 'true') } catch { /* Session remains in memory. */ }
}
export function shouldRestoreLogin() {
  if (loginPreferences().autoLogin) return true
  try { return sessionStorage.getItem(ACTIVE) === 'true' } catch { return false }
}
export function forgetActiveLogin() {
  try { sessionStorage.removeItem(ACTIVE) } catch { /* Storage may be disabled. */ }
}
