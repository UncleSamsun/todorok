import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { SessionClient } from '@todorok/api-client'
import { createBrowserSessionCoordinator } from './app/browser-session'

const rootElement = document.getElementById('root')

window.addEventListener('vite:preloadError', (event) => {
  event.preventDefault()
  const key = 'todorok.preload-recovery'
  if (sessionStorage.getItem(key) === '1') return
  sessionStorage.setItem(key, '1')
  window.location.reload()
})

if (!rootElement) {
  throw new Error('root element not found')
}

const session = new SessionClient({ coordinator: createBrowserSessionCoordinator() })
createRoot(rootElement).render(
  <StrictMode>
    <App session={session} />
  </StrictMode>,
)
