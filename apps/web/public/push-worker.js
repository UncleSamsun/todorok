const appPath = new URL(self.registration.scope).pathname
const todayPath = `${appPath}today`
self.addEventListener('push', event => {
  let id = 'summary'
  try {
    const payload = event.data?.json()
    if (typeof payload?.id === 'string' && /^[a-zA-Z0-9-]{1,64}$/.test(payload.id)) id = payload.id
  } catch { /* Always show a visible notification, even for an empty payload. */ }
  event.waitUntil(self.registration.showNotification('토도록', {
    body: '오늘의 할 일과 기록을 확인해 보세요.', tag: `todorok-${id}`,
    icon: `${appPath}icon.svg`,
  }))
})
self.addEventListener('notificationclick', event => {
  event.notification.close()
  event.waitUntil((async () => {
    const clients = await self.clients.matchAll({ type: 'window', includeUncontrolled: true })
    for (const client of clients) {
      const url = new URL(client.url)
      if (url.origin === self.location.origin && (url.pathname.startsWith(appPath) || url.pathname === appPath.slice(0, -1))) {
        await client.navigate(todayPath)
        await client.focus()
        return
      }
    }
    await self.clients.openWindow(todayPath)
  })())
})
