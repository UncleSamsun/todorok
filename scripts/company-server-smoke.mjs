import assert from 'node:assert/strict'
import {randomUUID} from 'node:crypto'
import {readFile,writeFile} from 'node:fs/promises'
import {execFile} from 'node:child_process'
import {promisify} from 'node:util'

// Credential values stay in memory and are never included in the report/output.
const [accessFile,reportFile]=process.argv.slice(2)
const access=JSON.parse(await readFile(accessFile,'utf8'))
const origin=new URL(access.url).origin
let token='',cookie=''
const report={origin,checks:[],tasks:[]}
async function request(route,{method='GET',body,expected=200,authenticated=true}={}){
  const response=await fetch(origin+route,{method,headers:{Origin:origin,...(token&&authenticated?{Authorization:'Bearer '+token}:{}),...(cookie&&authenticated?{Cookie:cookie}:{}),...(body?{'Content-Type':'application/json'}:{})},body:body?JSON.stringify(body):undefined})
  if(response.status!==expected)throw new Error(`${method} ${route}: expected ${expected}, got ${response.status}`)
  const text=await response.text()
  return {data:text&&response.headers.get('content-type')?.includes('json')?JSON.parse(text):text,response}
}
const check=name=>{report.checks.push(name);console.log('PASS '+name)}
async function query(sql){
 const result=await promisify(execFile)('ssh',['-o','BatchMode=yes','-o','StrictHostKeyChecking=yes','ai-server',`kubectl -n todorok-test exec postgres-0 -- psql -U postgres -d todorok -Atc "${sql}"`])
 return result.stdout.trim()
}
async function waitQuery(sql,expected){
 for(let attempt=0;attempt<60;attempt++){
  if(await query(sql)===expected)return
  await new Promise(r=>setTimeout(r,1000))
 }
 throw new Error('CDC database projection did not reach expected state')
}
const html=(await request('/todorok/')).data
assert.match(html,/\/todorok\/assets\//)
for(const asset of [...html.matchAll(/(?:src|href)="(\/todorok\/assets\/[^" ]+)"/g)].map(m=>m[1]))await request(asset)
await request('/todorok/assets/missing-deployment-check.js',{expected:404})
const manifestResponse=await request('/todorok/manifest.webmanifest')
assert.match(manifestResponse.response.headers.get('content-type'),/application\/manifest\+json/)
const manifest=manifestResponse.data
assert.equal(new URL(manifest.scope,access.url).pathname,'/todorok/')
assert.equal(new URL(manifest.start_url,access.url).pathname,'/todorok/')
await request('/todorok/sw.js')
await request('/todorok/push-worker.js')
check('HTTPS, base-path assets, missing asset 404 and scoped PWA')
await request('/api/planner/v1/preferences',{expected:401,authenticated:false})
await request('/api/notification/v1/push/config',{expected:401,authenticated:false})
await request('/api/activity/v1/internal/templates',{expected:404,authenticated:false})
check('Unauthenticated APIs and internal route protection')
const login=await request('/api/planner/v1/auth/login',{method:'POST',body:{email:access.email,password:access.password}})
const setCookie=login.response.headers.get('set-cookie')
assert.match(setCookie,/; Secure/i);assert.match(setCookie,/; HttpOnly/i)
assert.match(setCookie,/Path=\/api\/planner\/v1\/auth/i)
cookie=setCookie.split(';')[0];token=login.data.accessToken
report.userId=login.data.userId
const oldCookie=cookie
const refreshed=await request('/api/planner/v1/auth/refresh',{method:'POST'})
token=refreshed.data.accessToken;cookie=refreshed.response.headers.get('set-cookie').split(';')[0]
assert.notEqual(cookie,oldCookie)
check('Login, Secure HttpOnly cookie and refresh rotation')
assert.ok((await request('/api/notification/v1/push/config')).data.publicKey.length>80)
assert.ok(Array.isArray((await request('/api/notification/v1/deliveries')).data))
check('Authenticated notification API and VAPID availability')
const originalPreferences=(await request('/api/planner/v1/preferences')).data
await request('/api/planner/v1/preferences',{method:'PUT',body:{theme:originalPreferences.theme,notificationsEnabled:true,summaryTime:originalPreferences.summaryTime,expectedRevision:originalPreferences.revision}})
const today=new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Seoul',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date())
const tomorrow=new Date(Date.parse(today+'T00:00:00Z')+86400000).toISOString().slice(0,10)
async function createTask(taskType,reminder=false){
 const task=(await request('/api/planner/v1/tasks',{method:'POST',expected:201,body:{commandId:randomUUID(),title:'[배포검증] '+taskType,taskType,scheduledDate:reminder?tomorrow:today,...(reminder?{scheduledTime:'10:00',reminderEnabled:true}:{})}})).data
 report.tasks.push(task.taskId);return task
}
let general=await createTask('GENERAL')
general=(await request('/api/planner/v1/tasks/'+general.taskId+'/complete',{method:'POST',body:{version:general.version}})).data
assert.equal(general.status,'COMPLETED')
check('Task create and general completion')
const reminder=await createTask('GENERAL',true)
assert.equal(reminder.reminderEnabled,true)
assert.equal(reminder.scheduledTime,'10:00')
check('Explicit Task reminder persisted')
await waitQuery(`select count(*) from notification.task_reminder where task_id='${reminder.taskId}' and enabled`, '1')
await waitQuery(`select count(*) from notification.notification_delivery where task_id='${reminder.taskId}' and kind='TASK_REMINDER' and status='PENDING'`, '1')
check('Task reminder and notification preference CDC scheduled a pending delivery')
const study=await createTask('STUDY')
let activity
const activityBody={commandId:randomUUID(),taskId:study.taskId,activityType:'STUDY',completionStatus:'COMPLETED',performedAt:new Date().toISOString(),detail:{study:{subject:'배포 검증',durationMinutes:1}}}
for(let attempt=0;attempt<60;attempt++){
 try{activity=(await request('/api/activity/v1/activities',{method:'POST',expected:201,body:activityBody})).data;break}catch(error){if(attempt===59)throw error;await new Promise(r=>setTimeout(r,1000))}
}
assert.ok(activity.activityId)
report.activityId=activity.activityId
let projected
for(let attempt=0;attempt<60;attempt++){
 const day=(await request('/api/planner/v1/calendar/'+today)).data
 projected=day.tasks?.find(t=>t.taskId===study.taskId)
 if(projected?.status==='COMPLETED')break
 await new Promise(r=>setTimeout(r,1000))
}
assert.equal(projected?.status,'COMPLETED')
assert.equal(projected.activityId,activity.activityId)
check('HTTP Task → outbox/Kafka → activity → planner completion round trip')
await request('/api/activity/v1/activities/'+activity.activityId+'/void',{method:'POST',body:{reason:'배포 검증 데이터 정리',version:activity.version}})
for(const id of report.tasks){
 let task
 for(const date of [today,tomorrow]){
  const day=(await request('/api/planner/v1/calendar/'+date)).data
  task=day.tasks?.find(t=>t.taskId===id)
  if(task)break
 }
 if(task)await request('/api/planner/v1/tasks/'+id+'?version='+task.version,{method:'DELETE',expected:204})
}
check('Smoke records voided and Tasks soft-deleted')
await waitQuery(`select count(*) from notification.task_reminder where task_id='${reminder.taskId}' and not enabled`, '1')
const currentPreferences=(await request('/api/planner/v1/preferences')).data
await request('/api/planner/v1/preferences',{method:'PUT',body:{theme:originalPreferences.theme,notificationsEnabled:originalPreferences.notificationsEnabled,summaryTime:originalPreferences.summaryTime,expectedRevision:currentPreferences.revision}})
check('Reminder cancellation projected and original preferences restored')
await request('/api/planner/v1/auth/logout',{method:'POST',expected:204})
check('Logout')
report.completedAt=new Date().toISOString()
await writeFile(reportFile,JSON.stringify(report,null,2)+'\n')
