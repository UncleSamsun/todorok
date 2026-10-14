import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { SessionClient } from '@todorok/api-client'
import { createBrowserSessionCoordinator } from './app/browser-session'

const rootElement = document.getElementById('root')

window.addEventListener('vite:preloadError', (event) => {
  const key = 'todorok.preload-recovery'
  const build = import.meta.env.VITE_APP_BUILD_ID
  // Persist before reloading so a failed recovery cannot loop across documents.
  // A later build gets its own retry, even when this tab stays open for days.
  try {
    if (sessionStorage.getItem(key) === build) return
    sessionStorage.setItem(key, build)
  } catch {
    // Without a persistent guard, leave the original failure observable.
    return
  }
  event.preventDefault()
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
