import { afterEach, beforeEach, expect, it, vi } from 'vitest'

vi.mock('react-dom/client', () => ({ createRoot: () => ({ render: vi.fn() }) }))
vi.mock('./App', () => ({ App: () => null }))
vi.mock('@todorok/api-client', () => ({ SessionClient: class {} }))
vi.mock('./app/browser-session', () => ({ createBrowserSessionCoordinator: vi.fn() }))

const reload = vi.fn()
let onPreloadError: (event: Event) => void

beforeEach(() => {
  sessionStorage.clear()
  document.body.innerHTML = '<div id="root"></div>'
  reload.mockClear()
  vi.stubGlobal('window', {
    location: { reload },
    addEventListener: (_type: string, listener: (event: Event) => void) => { onPreloadError = listener },
  })
})
afterEach(() => { vi.unstubAllGlobals(); vi.unstubAllEnvs(); vi.restoreAllMocks() })

async function loadBuild(build: string) {
  vi.resetModules()
  vi.stubEnv('VITE_APP_BUILD_ID', build)
  await import('./main')
}
function failPreload() {
  const event = new Event('vite:preloadError', { cancelable: true })
  onPreloadError(event)
  return event
}

it('reloads once for a stale build even after the document reloads', async () => {
  await loadBuild('build-a')
  expect(failPreload().defaultPrevented).toBe(true)
  await loadBuild('build-a')
  failPreload()
  expect(reload).toHaveBeenCalledTimes(1)
})

it('can recover a later deployment in the same browser tab', async () => {
  await loadBuild('build-a')
  failPreload()
  await loadBuild('build-b')
  expect(failPreload().defaultPrevented).toBe(true)
  expect(reload).toHaveBeenCalledTimes(2)
})

it('lets a repeated failure propagate instead of hiding it after the retry', async () => {
  await loadBuild('build-a')
  failPreload()
  expect(failPreload().defaultPrevented).toBe(false)
})

it('does not throw or risk a reload loop when browser storage is unavailable', async () => {
  await loadBuild('build-a')
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Denied', 'SecurityError') })
  let event: Event | undefined
  expect(() => { event = failPreload() }).not.toThrow()
  expect(event?.defaultPrevented).toBe(false)
  expect(reload).not.toHaveBeenCalled()
})
