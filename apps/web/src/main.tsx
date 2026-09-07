import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { SessionClient } from '@todorok/api-client'
import { createBrowserSessionCoordinator } from './app/browser-session'

const rootElement = document.getElementById('root')

if (!rootElement) {
  throw new Error('root element not found')
}

const session = new SessionClient({ coordinator: createBrowserSessionCoordinator() })
createRoot(rootElement).render(
  <StrictMode>
    <App session={session} />
  </StrictMode>,
)
