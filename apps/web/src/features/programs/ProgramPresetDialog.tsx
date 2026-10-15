import { useEffect,useMemo,useRef,useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { activity } from '@todorok/api-client'
import presets from '../../../../../contracts/catalog/builtin/presets.json'
import { builtinPrograms } from './builtinPrograms'
import { useAuth } from '../auth/AuthProvider'
import { TaskFloatingPanel } from '../today/TaskFloatingPanel'
import { requestFailure } from '../activity/requestFailure'

type Program=typeof builtinPrograms[number]
type ProgramCommand={mode:'enroll';body:activity.EnrollProgramRequest}|{mode:'schedule';id:string;body:activity.ScheduleProgramSessionRequest}
export function ProgramPresetDialog({program,enrollment,loading=false,close}:{program:Program;enrollment?:activity.ProgramEnrollmentResponse;loading?:boolean;close:()=>void}) {
  const {session,state}=useAuth(),queries=useQueryClient()
  const api=useMemo(()=>new activity.ActivityApi(new activity.Configuration({basePath:'/api/activity/v1',fetchApi:session.fetch})),[session])
  const preset=presets[program.key]
  const [week,setWeek]=useState(enrollment?.currentWeek??1),[maximum,setMaximum]=useState('')
  const [selectedSession,setSelectedSession]=useState(enrollment?.currentSession??1)
  useEffect(()=>{if(enrollment){setWeek(enrollment.currentWeek);setSelectedSession(enrollment.currentSession)}},[enrollment?.enrollmentId,enrollment?.currentWeek,enrollment?.currentSession])
  const [busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[error,setError]=useState('')
  const command=useRef(crypto.randomUUID()),request=useRef<ProgramCommand|null>(null),pending=useRef(false)
  const max=Number(maximum)
  const level=program.key==='hundred-pushups' ? enrollment ? Math.max(0,Number(enrollment.catalogKey.match(/level-(\d+)/)?.[1]??1)-1) : week<=2?(max<=5?0:max<=10?1:2):week<=4?(max<=20?0:max<=25?1:2):week===5?(max<=35?0:max<=40?1:2):(max<=50?0:max<=60?1:2) : 0
  const sessions=preset.weeks[week-1].map((levels,index)=>enrollment&&week===enrollment.currentWeek&&index+1===enrollment.currentSession?enrollment.target.targetSets:levels[Math.min(level,levels.length-1)])
  async function add() {
    if(pending.current||loading)return
    if(!Number.isSafeInteger(max)||max<0){setError('횟수를 확인해 주세요.');return}
    pending.current=true;setBusy(true);setError('')
    if(!uncertain)request.current=enrollment?{mode:'schedule',id:enrollment.enrollmentId,body:{commandId:command.current,week,session:selectedSession,expectedRevision:enrollment.revision??0}}:{mode:'enroll',body:{commandId:command.current,catalogKey:program.key==='hundred-pushups'?`${program.key}-level-${level+1}`:program.key,catalogVersion:1,initialTestValue:max,startWeek:week,startSession:selectedSession}}
    try {
      const saved=request.current!
      const result=saved.mode==='enroll'?await api.enrollProgram({enrollProgramRequest:saved.body}):await api.scheduleProgramSession({enrollmentId:saved.id,scheduleProgramSessionRequest:saved.body})
      queries.setQueryData<activity.ProgramEnrollmentResponse[]>(['program-enrollments',state.userId??'session'],current=>[result,...(current??[]).filter(item=>item.enrollmentId!==result.enrollmentId)])
      await queries.invalidateQueries({queryKey:['calendar']})
      await queries.invalidateQueries({queryKey:['program-enrollments']})
      close()
    } catch(reason) {
      const failure=await requestFailure(reason);setUncertain(!failure.rejected)
      if(failure.rejected){command.current=crypto.randomUUID();request.current=null}
      setError(failure.rejected?'일정을 추가하지 못했습니다.':'저장 결과를 확인하지 못했습니다.')
      if(failure.status===409)await queries.invalidateQueries({queryKey:['program-enrollments']})
    } finally {pending.current=false;setBusy(false)}
  }
  return <TaskFloatingPanel label={program.name} busy={busy} close={close}>
    <div className="preset-week-picker"><button type="button" aria-label="이전 주차" disabled={week===1||busy||uncertain||loading} onClick={()=>setWeek(week-1)}>‹</button><label className="visually-hidden" htmlFor="preset-week">주차</label><select id="preset-week" aria-label="주차" value={week} disabled={busy||uncertain||loading} onChange={event=>setWeek(Number(event.target.value))}>{preset.weeks.map((_,index)=><option key={index} value={index+1}>{index+1}주차</option>)}</select><button type="button" aria-label="다음 주차" disabled={week===preset.weeks.length||busy||uncertain||loading} onClick={()=>setWeek(week+1)}>›</button></div>
    <ol className="preset-sessions">{sessions.map((sets,index)=><li key={index}><button type="button" className="preset-session-row" aria-label={`${index+1}회차 선택`} aria-pressed={selectedSession===index+1} disabled={busy||uncertain||loading} onClick={()=>setSelectedSession(index+1)}><strong>{index+1}회차</strong><span>{sets.map((count,set)=>`${count}${preset.lastSetMinimum&&set===sets.length-1?'+':''}`).join(' · ')}</span></button></li>)}</ol>
    {!enrollment&&!loading&&<label className="preset-maximum">초기 최대 횟수<input aria-label="초기 최대 횟수" type="number" min="0" step="1" value={maximum} disabled={busy||uncertain} onChange={event=>setMaximum(event.target.value)}/></label>}
    {error&&<p role="alert">{error}</p>}
    <div className="form-actions"><button type="button" disabled={busy} onClick={close}>닫기</button><button type="button" className="primary" disabled={busy||loading} onClick={()=>void add()}>{uncertain?'같은 요청 다시 보내기':busy?'추가 중…':'일정 추가'}</button></div>
  </TaskFloatingPanel>
}
