import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import PushSettings from './PushSettings'
const state = vi.hoisted(() => ({ fetch: vi.fn(), generation: 1 }))
vi.mock('../auth/AuthProvider', () => ({ useAuth: () => ({ session: { fetch: state.fetch, getSnapshot: () => ({ generation: state.generation }) } }) }))
const permission=vi.fn(), subscribe=vi.fn(), getSubscription=vi.fn(), unsubscribe=vi.fn()
beforeEach(() => {
  state.fetch.mockReset(); state.generation=1
  permission.mockReset().mockResolvedValue('granted'); subscribe.mockReset();getSubscription.mockReset().mockResolvedValue(null);unsubscribe.mockReset().mockResolvedValue(true)
  vi.stubGlobal('Notification', {requestPermission:permission})
  vi.stubGlobal('PushManager', function(){})
  vi.stubGlobal('matchMedia', () => ({ matches:true }))
  Object.defineProperty(navigator, 'serviceWorker', {configurable:true,value:{ready:Promise.resolve({pushManager:{subscribe,getSubscription}})}})
})
afterEach(()=>{cleanup();vi.unstubAllGlobals();delete (navigator as unknown as Record<string,unknown>).serviceWorker})
it('requests permission only on click and persists the browser keys through the authenticated API', async()=>{
  const subscription={endpoint:'https://fcm.googleapis.com/x',keys:{p256dh:'key',auth:'secret'}}
  subscribe.mockResolvedValue({toJSON:()=>subscription})
  state.fetch.mockImplementation(async(path:string)=> path.endsWith('/config') ? Response.json({publicKey:'vapid-public'}) : Response.json({id:'id'}))
  render(<PushSettings />)
  expect(permission).not.toHaveBeenCalled();expect(state.fetch).not.toHaveBeenCalled()
  fireEvent.click(screen.getByRole('button',{name:'이 기기에서 알림 받기'}))
  expect(permission).toHaveBeenCalledOnce()
  await screen.findByText(/이 기기에 알림을 연결했습니다/)
  expect(subscribe).toHaveBeenCalledWith({userVisibleOnly:true,applicationServerKey:'vapid-public'})
  expect(state.fetch).toHaveBeenLastCalledWith('/api/notification/v1/push/subscriptions',expect.objectContaining({method:'POST',body:JSON.stringify(subscription)}))
})
it('permission denial makes no API request and explains recovery',async()=>{
  permission.mockResolvedValue('denied');render(<PushSettings />)
  fireEvent.click(screen.getByRole('button',{name:'이 기기에서 알림 받기'}))
  await screen.findByText(/알림 권한이 거절됐습니다/)
  expect(state.fetch).not.toHaveBeenCalled()
})
it('retries a failed server save using the existing subscription',async()=>{
  const existing={toJSON:()=>({endpoint:'https://fcm.googleapis.com/x',keys:{p256dh:'key',auth:'secret'}})}
  getSubscription.mockResolvedValue(existing)
  let posts=0
  state.fetch.mockImplementation(async(path:string)=>path.endsWith('/config')?Response.json({publicKey:'vapid-public'}):(++posts===1?new Response(null,{status:503}):Response.json({id:'id'})))
  render(<PushSettings />)
  fireEvent.click(screen.getByRole('button',{name:'이 기기에서 알림 받기'}))
  await screen.findByText(/설정을 저장하지 못했습니다/)
  fireEvent.click(screen.getByRole('button',{name:'이 기기에서 알림 받기'}))
  await screen.findByText(/이 기기에 알림을 연결했습니다/)
  expect(subscribe).not.toHaveBeenCalled();expect(posts).toBe(2)
})
it('does not unsubscribe locally when the server delete fails, so it remains retryable',async()=>{
  getSubscription.mockResolvedValue({endpoint:'https://fcm.googleapis.com/x',unsubscribe})
  state.fetch.mockResolvedValue(new Response(null,{status:503}));render(<PushSettings />)
  fireEvent.click(screen.getByRole('button',{name:'이 기기 알림 해제'}))
  await screen.findByText(/解제하지 못했습니다|해제하지 못했습니다/)
  expect(unsubscribe).not.toHaveBeenCalled()
})
