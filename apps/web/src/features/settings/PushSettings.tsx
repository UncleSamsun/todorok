import { useState } from 'react'
import { useAuth } from '../auth/AuthProvider'

export default function PushSettings() {
  const { session } = useAuth()
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const supported = typeof Notification !== 'undefined' && 'serviceWorker' in navigator && 'PushManager' in window
  const standalone = window.matchMedia?.('(display-mode: standalone)').matches || (navigator as Navigator & { standalone?: boolean }).standalone
  const available = supported && standalone

  async function request(path: string, init?: RequestInit) {
    const response = await session.fetch(`/api/notification/v1${path}`, init)
    if (!response.ok) throw new Error('기기 알림 설정을 저장하지 못했습니다. 다시 시도해 주세요.')
    return response
  }
  async function enable() {
    if (!available || busy) return
    setBusy(true); setMessage('')
    try {
      // Invoke synchronously from the click, before network or service-worker awaits.
      const permission = await Notification.requestPermission()
      if (permission !== 'granted') { setMessage('알림 권한이 거절됐습니다. 기기 설정에서 허용한 뒤 다시 시도해 주세요.'); return }
      const config = await (await request('/push/config')).json() as { publicKey: string }
      if (!config.publicKey) throw new Error('알림 서비스가 아직 준비되지 않았습니다.')
      const registration = await navigator.serviceWorker.ready
      const existing = await registration.pushManager.getSubscription()
      const subscription = existing ?? await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: config.publicKey })
      await request('/push/subscriptions', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(subscription.toJSON()) })
      setMessage('이 기기에 알림을 연결했습니다. 오늘 요약 설정을 켜면 알림을 받을 수 있습니다.')
    } catch (error) { setMessage(error instanceof Error ? error.message : '알림을 연결하지 못했습니다. 다시 시도해 주세요.') }
    finally { setBusy(false) }
  }
  async function disable() {
    if (!available || busy) return
    setBusy(true); setMessage('')
    try {
      const registration = await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.getSubscription()
      if (subscription) {
        await request('/push/subscriptions', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ endpoint: subscription.endpoint }) })
        await subscription.unsubscribe()
      }
      setMessage('이 기기의 알림 연결을 해제했습니다.')
    } catch { setMessage('알림 연결을 해제하지 못했습니다. 다시 시도해 주세요.') }
    finally { setBusy(false) }
  }
  return <section aria-label="기기 알림"><button disabled={!available || busy} onClick={() => void enable()}>이 기기에서 알림 받기</button>
    <button disabled={!available || busy} onClick={() => void disable()}>이 기기 알림 해제</button>
    {!available && <p>지원하는 브라우저에서 홈 화면에 추가한 앱으로 열면 기기 알림을 연결할 수 있습니다.</p>}
    {message && <p role="status">{message}</p>}
  </section>
}
