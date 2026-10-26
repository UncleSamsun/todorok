import {act,cleanup,fireEvent,render,screen,waitFor,within} from '@testing-library/react'
import {afterEach,expect,it} from 'vitest'
import {SessionClient} from '@todorok/api-client'
import catalogs from '../../../../../contracts/catalog/builtin/catalogs.json'
import {App} from '../../App'

afterEach(()=>{cleanup();history.replaceState({},'','/');localStorage.clear()})
function open(initialEnrollment?:Record<string,unknown>){
  const enrollments:Record<string,any>[]=initialEnrollment?[initialEnrollment]:[],writes:Record<string,unknown>[]=[],taskWrites:unknown[]=[],schedules:Record<string,unknown>[]=[]
  const session=new SessionClient({fetcher:async(url,init)=>{
    const path=String(url)
    if(path.endsWith('/refresh'))return Response.json({accessToken:'token',userId:'owner',expiresAt:'2099-01-01T00:00:00Z'})
    if(path.endsWith('/rollover'))return Response.json({today:'2026-10-15',movedCount:0})
    if(path.endsWith('/preferences'))return Response.json({theme:'SYSTEM',notificationsEnabled:false,summaryTime:'08:00',revision:0})
    if(path.endsWith('/programs'))return Response.json(catalogs.map(c=>({...c,catalogVersion:c.version,totalWeeks:c.weeks.length})))
    if(path.endsWith('/program-enrollments')){
      if(init?.method!=='POST')return Response.json(enrollments)
      const request=JSON.parse(String(init.body));writes.push(request)
      const catalog=catalogs.find(c=>c.catalogKey===request.catalogKey)!,week=request.startWeek??1,selectedSession=request.startSession??1
      const result={enrollmentId:'enrollment',catalogKey:catalog.catalogKey,catalogVersion:1,recommendedWeek:1,startWeek:week,currentWeek:week,currentSession:selectedSession,revision:0,status:'ACTIVE',target:{sessionId:'session',week,session:selectedSession,targetSets:catalog.weeks[week-1].sessions[selectedSession-1].sets}}
      enrollments.push(result);return Response.json(result,{status:201})
    }
    if(path.includes('/program-enrollments/')&&path.endsWith('/sessions')){
      const request=JSON.parse(String(init?.body));schedules.push(request)
      const current=enrollments[0],catalog=catalogs.find(c=>c.catalogKey===current.catalogKey)!
      Object.assign(current,{currentWeek:request.week,currentSession:request.session,revision:(current.revision??0)+1,target:{sessionId:'selected',week:request.week,session:request.session,targetSets:catalog.weeks[request.week-1].sessions[request.session-1].sets}})
      return Response.json(current)
    }
    if(path.endsWith('/tasks')&&init?.method==='POST')taskWrites.push(init.body)
    if(path.includes('/templates'))return Response.json({items:[]})
    if(path.includes('/activities/summary'))return Response.json({month:'2026-10',activityType:'WORKOUT',completedCount:0,durationSeconds:0})
    if(path.includes('/activities?'))return Response.json({items:[]})
    if(path.includes('/calendar?'))return Response.json({days:[]})
    if(path.includes('/calendar/'))return Response.json({date:'2026-10-15',tasks:[]})
    return Response.json({},{status:404})
  }})
  render(<App session={session}/>);return {writes,taskWrites,schedules,session}
}
it('opens pushup preset sets instead of the free-workout form and schedules the preset',async()=>{
  const {writes,taskWrites}=open()
  await waitFor(()=>expect(location.pathname).toBe('/today'))
  await waitFor(()=>expect(screen.getByRole('button',{name:'할 일 추가'})).toBeEnabled())
  await act(async()=>{fireEvent.click(screen.getByRole('link',{name:'운동'}))})
  fireEvent.click(await screen.findByRole('button',{name:'푸시업'}))
  const dialog=screen.getByRole('dialog',{name:'푸시업'})
  expect(within(dialog).getByText('2 · 3 · 2 · 2 · 3+')).toBeVisible()
  expect(within(dialog).queryByLabelText('종류')).not.toBeInTheDocument()
  expect(within(dialog).queryByLabelText('이름')).not.toBeInTheDocument()
  const maximum = await within(dialog).findByLabelText('초기 최대 횟수')
  fireEvent.click(within(dialog).getByRole('button',{name:'다음 주차'}))
  fireEvent.change(maximum,{target:{value:'8'}})
  expect(within(dialog).getByText('9 · 11 · 8 · 8 · 11+')).toBeVisible()
  fireEvent.click(within(dialog).getByRole('button',{name:'2회차 선택'}))
  fireEvent.click(within(dialog).getByRole('button',{name:'일정 추가'}))
  await waitFor(()=>expect(writes).toHaveLength(1))
  expect(writes[0]).toMatchObject({catalogKey:'hundred-pushups-level-2',catalogVersion:1,initialTestValue:8,startWeek:2,startSession:2})
  expect(taskWrites).toHaveLength(0)
  expect(location.pathname).toBe('/workout')
})
it('opens the full pullup preset through week 38 without a workout-kind picker',async()=>{
  open();await waitFor(()=>expect(location.pathname).toBe('/today'))
  await waitFor(()=>expect(screen.getByRole('button',{name:'할 일 추가'})).toBeEnabled())
  await act(async()=>{fireEvent.click(screen.getByRole('link',{name:'운동'}))})
  fireEvent.click(await screen.findByRole('button',{name:'풀업'}))
  const dialog=screen.getByRole('dialog',{name:'풀업'})
  expect(within(dialog).getAllByText('6 · 5 · 5 · 4 · 3')).toHaveLength(3)
  for(let i=1;i<38;i++)fireEvent.click(within(dialog).getByRole('button',{name:'다음 주차'}))
  expect(within(dialog).getByText('38주차')).toBeVisible()
  expect(within(dialog).getAllByText('31 · 18 · 17 · 16 · 16')).toHaveLength(3)
  expect(within(dialog).getByRole('button',{name:'다음 주차'})).toBeDisabled()
  expect(within(dialog).getByRole('combobox',{name:'주차'})).toBeVisible()
  expect(within(dialog).queryByLabelText('종류')).not.toBeInTheDocument()
})
it('loads the current configured program without asking for its type or initial setup again',async()=>{
  const {writes}=open({enrollmentId:'existing',catalogKey:'hundred-pushups-level-2',catalogVersion:1,recommendedWeek:1,startWeek:1,currentWeek:3,currentSession:2,status:'ACTIVE',target:{sessionId:'current',week:3,session:2,targetSets:[14,19,14,14,19]}})
  await waitFor(()=>expect(screen.getByRole('button',{name:'할 일 추가'})).toBeEnabled())
  await act(async()=>{fireEvent.click(screen.getByRole('link',{name:'운동'}))})
  fireEvent.click(await screen.findByRole('button',{name:'푸시업'}))
  const dialog=screen.getByRole('dialog',{name:'푸시업'})
  expect(await within(dialog).findByText('3주차')).toBeVisible()
  expect(within(dialog).getByText('14 · 19 · 14 · 14 · 19+')).toBeVisible()
  expect(within(dialog).queryByLabelText('초기 최대 횟수')).not.toBeInTheDocument()
  expect(within(dialog).getByRole('combobox',{name:'주차'})).toHaveValue('3')
  expect(within(dialog).getByRole('button',{name:'2회차 선택'})).toHaveAttribute('aria-pressed','true')
  expect(writes).toHaveLength(0)
})
it('schedules a selected session in the same program and restores it after a fresh app load',async()=>{
  const {schedules,writes,session}=open({enrollmentId:'existing',catalogKey:'hundred-pushups-level-2',catalogVersion:1,recommendedWeek:1,startWeek:1,currentWeek:3,currentSession:2,revision:4,status:'ACTIVE',target:{sessionId:'current',week:3,session:2,targetSets:[14,19,14,14,19]}})
  await waitFor(()=>expect(screen.getByRole('button',{name:'할 일 추가'})).toBeEnabled())
  await act(async()=>{fireEvent.click(screen.getByRole('link',{name:'운동'}))})
  fireEvent.click(await screen.findByRole('button',{name:'푸시업'}))
  let dialog=screen.getByRole('dialog',{name:'푸시업'})
  fireEvent.change(within(dialog).getByRole('combobox',{name:'주차'}),{target:{value:'4'}})
  fireEvent.click(within(dialog).getByRole('button',{name:'3회차 선택'}))
  fireEvent.click(within(dialog).getByRole('button',{name:'일정 추가'}))
  await waitFor(()=>expect(schedules).toHaveLength(1))
  expect(schedules[0]).toMatchObject({week:4,session:3,expectedRevision:4})
  expect(writes).toHaveLength(0)
  await waitFor(()=>expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  cleanup();history.replaceState({},'','/');render(<App session={session}/>)
  await waitFor(()=>expect(screen.getByRole('button',{name:'할 일 추가'})).toBeEnabled())
  await act(async()=>{fireEvent.click(screen.getByRole('link',{name:'운동'}))})
  fireEvent.click(await screen.findByRole('button',{name:'푸시업'}))
  dialog=screen.getByRole('dialog',{name:'푸시업'})
  await waitFor(()=>expect(within(dialog).getByRole('combobox',{name:'주차'})).toHaveValue('4'))
  expect(within(dialog).getByRole('button',{name:'3회차 선택'})).toHaveAttribute('aria-pressed','true')
})
