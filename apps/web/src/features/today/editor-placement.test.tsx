import {cleanup,fireEvent,render,screen,waitFor} from '@testing-library/react'
import {afterEach,expect,it} from 'vitest'
import {SessionClient} from '@todorok/api-client'
import {App} from '../../App'
afterEach(()=>{cleanup();localStorage.clear();window.history.replaceState({},'','/')})
const task={taskId:'00000000-0000-0000-0000-000000000001',userId:'owner',title:'기존 일정',taskType:'GENERAL',scheduledDate:'2026-10-14',status:'PLANNED',version:0}
function open(){
 const session=new SessionClient({fetcher:async url=>{
  const path=String(url)
  const value=path.endsWith('/refresh')?{accessToken:'token',userId:'owner',expiresAt:'2099-01-01T00:00:00Z'}:path.endsWith('/rollover')?{today:'2026-10-14',movedCount:0}:path.endsWith('/preferences')?{theme:'SYSTEM',notificationsEnabled:false,summaryTime:'08:00',revision:0}:path.includes('/notes/')?{date:'2026-10-14',content:'',version:null}:path.includes('/calendar?')?{days:[]}:path.includes('/calendar/')?{date:'2026-10-14',tasks:[task]}:task
  return Response.json(value)
 }})
 render(<App session={session}/> )
}
it('opens add before the task list and sticky note in reading order',async()=>{
 open();const add=await screen.findByRole('button',{name:'할 일 추가'});await waitFor(()=>expect(add).toBeEnabled());fireEvent.click(add)
 const form=(await screen.findByLabelText('제목')).closest('form')!
 expect(form.compareDocumentPosition(screen.getByRole('heading',{name:'할 일 0 / 1'}))&Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
 expect(form.compareDocumentPosition(screen.getByRole('region',{name:'오늘의 스티키노트'}))&Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
})
it('opens editing before the task list rather than below the sticky note',async()=>{
 open();fireEvent.click(await screen.findByRole('button',{name:'기존 일정 수정'}))
 const form=(await screen.findByLabelText('제목 수정')).closest('form')!
 expect(form.compareDocumentPosition(screen.getByRole('heading',{name:'할 일 0 / 1'}))&Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
})
