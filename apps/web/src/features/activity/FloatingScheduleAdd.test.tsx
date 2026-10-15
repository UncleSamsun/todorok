import { cleanup,fireEvent,render,screen,waitFor } from '@testing-library/react'
import { afterEach,expect,it } from 'vitest'
import { SessionClient } from '@todorok/api-client'
import { App } from '../../App'

afterEach(()=>{cleanup();history.replaceState({},'','/');localStorage.clear()})

it.each([['운동','WORKOUT','자유운동','/workout'],['공부','STUDY','자유공부','/study'],['클라이밍','CLIMBING','자유클라이밍','/climbing']])('adds %s from a floating form without leaving its tab',async(label,type,free,path)=>{
  const writes:Record<string,unknown>[]=[]
  const session=new SessionClient({fetcher:async(url,init)=>{
    const endpoint=String(url)
    if(endpoint.endsWith('/refresh'))return Response.json({accessToken:'token',userId:'owner',expiresAt:'2099-01-01T00:00:00Z'})
    if(endpoint.endsWith('/rollover'))return Response.json({today:'2026-10-15',movedCount:0})
    if(endpoint.endsWith('/preferences'))return Response.json({theme:'SYSTEM',notificationsEnabled:false,summaryTime:'08:00',revision:0})
    if(endpoint.includes('/templates'))return Response.json({items:[]})
    if(endpoint.endsWith('/programs')||endpoint.endsWith('/program-enrollments'))return Response.json([])
    if(endpoint.includes('/activities/summary'))return Response.json({month:'2026-10',activityType:type,completedCount:0,durationSeconds:0})
    if(endpoint.includes('/calendar?'))return Response.json({days:[]})
    if(endpoint.includes('/calendar/'))return Response.json({date:'2026-10-15',tasks:[]})
    if(endpoint.includes('/activities?'))return Response.json({items:[]})
    if(endpoint.endsWith('/tasks')&&init?.method==='POST'){
      const body=JSON.parse(String(init.body));writes.push(body)
      return Response.json({...body,taskId:'created',userId:'owner',status:'PLANNED',version:0},{status:201})
    }
    return Response.json({},{status:404})
  }})
  render(<App session={session}/>)
  await waitFor(()=>expect(location.pathname).toBe('/today'))
  fireEvent.click(await screen.findByRole('link',{name:label}))
  fireEvent.click(await screen.findByRole('button',{name:free}))
  const name=await screen.findByLabelText('이름'),date=screen.getByLabelText('날짜')
  fireEvent.change(name,{target:{value:'새 일정'}})
  fireEvent.blur(name,{relatedTarget:date})
  expect(writes).toHaveLength(0)
  fireEvent.change(date,{target:{value:'2026-10-16'}})
  fireEvent.blur(date,{relatedTarget:document.body})
  await waitFor(()=>expect(writes).toHaveLength(1))
  await waitFor(()=>expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  expect(location.pathname).toBe(path)
  expect(writes[0]).toMatchObject({title:'새 일정',taskType:type,scheduledDate:'2026-10-16'})
  expect(writes[0]).not.toHaveProperty('templateSelection')
})
