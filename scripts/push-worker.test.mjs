import assert from 'node:assert/strict'
import { readFile, access } from 'node:fs/promises'
import vm from 'node:vm'
import test from 'node:test'

test('push worker displays a stable tag and opens only the local today screen', async () => {
  const file='apps/web/public/push-worker.js'
  assert.ok(await access(file).then(()=>true,()=>false), 'Push receiver is not implemented')
  const listeners={}, shown=[], opened=[]
  const self={location:{origin:'https://example.test'},addEventListener:(name,fn)=>{listeners[name]=fn},registration:{scope:'https://example.test/',showNotification:async(...args)=>shown.push(args)},clients:{matchAll:async()=>[],openWindow:async path=>opened.push(path)}}
  vm.runInNewContext(await readFile(file,'utf8'),{self,URL})
  let work
  listeners.push({data:{json:()=>({id:'delivery-1',url:'https://evil.example',title:'arbitrary'})},waitUntil:p=>{work=p}})
  await work
  assert.equal(shown[0][0],'토도록')
  assert.equal(shown[0][1].tag,'todorok-delivery-1')
  listeners.notificationclick({notification:{close(){},data:{url:'https://evil.example'}},waitUntil:p=>{work=p}})
  await work
  assert.deepEqual(opened,['/today'])
})

test('scoped push uses its own icon and focuses only an app window', async () => {
  const listeners = {}, shown = [], navigated = [], focused = [], opened = []
  const windowClient = (path) => ({url: `https://example.test${path}`, navigate: async url => navigated.push([path, url]), focus: async () => focused.push(path)})
  const self = {
    location: {origin: 'https://example.test'},
    registration: {scope: 'https://example.test/todorok/', showNotification: async (...args) => shown.push(args)},
    addEventListener: (name, fn) => { listeners[name] = fn },
    clients: {matchAll: async () => [windowClient('/other-service/'), windowClient('/todorok-else/'), windowClient('/todorok/study')], openWindow: async url => opened.push(url)},
  }
  vm.runInNewContext(await readFile('apps/web/public/push-worker.js', 'utf8'), {self, URL})
  let work
  listeners.push({waitUntil: p => {work = p}})
  await work
  assert.equal(shown[0][1].icon, '/todorok/icon.svg')
  listeners.notificationclick({notification: {close() {}}, waitUntil: p => {work = p}})
  await work
  assert.deepEqual(navigated, [['/todorok/study', '/todorok/today']])
  assert.deepEqual(focused, ['/todorok/study'])
  assert.deepEqual(opened, [])
})

test('scoped push opens an app window when only other services are open', async () => {
  const listeners = {}, opened = []
  const self = {
    location: {origin: 'https://example.test'},
    registration: {scope: 'https://example.test/todorok/'},
    addEventListener: (name, fn) => { listeners[name] = fn },
    clients: {matchAll: async () => [{url: 'https://example.test/another/', navigate: async () => assert.fail('another service must not navigate')}, {url: 'https://elsewhere.test/todorok/today', navigate: async () => assert.fail('another origin must not navigate')}], openWindow: async url => opened.push(url)},
  }
  vm.runInNewContext(await readFile('apps/web/public/push-worker.js', 'utf8'), {self, URL})
  let work
  listeners.notificationclick({notification: {close() {}}, waitUntil: p => {work = p}})
  await work
  assert.deepEqual(opened, ['/todorok/today'])
})
