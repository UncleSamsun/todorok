import { useMemo, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { activity, planner } from '@todorok/api-client'
import { seoulToday } from '@todorok/client-domain'
import { useAuth } from '../auth/AuthProvider'
import { useRecordTemplates } from '../study/useStudyTemplates'
import { TaskFloatingPanel } from '../today/TaskFloatingPanel'
import { requestFailure } from './requestFailure'

export function FloatingScheduleAdd({type,initialTemplateId='',initialTitle='',close}:{
  type:'WORKOUT'|'STUDY'|'CLIMBING';initialTemplateId?:string;initialTitle?:string;close:()=>void
}) {
  const {session,state}=useAuth(),queries=useQueryClient()
  const api=useMemo(()=>({tasks:new planner.TaskApi(new planner.Configuration({basePath:'/api/planner/v1',fetchApi:session.fetch})),templates:new activity.TemplateApi(new activity.Configuration({basePath:'/api/activity/v1',fetchApi:session.fetch}))}),[session])
  const domain=type==='STUDY'?activity.TemplateDomain.Study:type==='WORKOUT'?activity.TemplateDomain.Workout:activity.TemplateDomain.Climbing
  const main=useRecordTemplates(api.templates,state.userId,domain,type==='STUDY'?activity.TemplateKind.StudyCategory:type==='WORKOUT'?activity.TemplateKind.FreeWorkout:activity.TemplateKind.ClimbingSession,false)
  const hangboard=useRecordTemplates(api.templates,state.userId,activity.TemplateDomain.Climbing,activity.TemplateKind.FreeHangboard,false,type==='CLIMBING')
  const templates=[...(main.data?.pages.flatMap(page=>page.items)??[]),...(type==='CLIMBING'?hangboard.data?.pages.flatMap(page=>page.items)??[]:[])].filter(item=>!item.archived)
  const [title,setTitle]=useState(initialTitle),[date,setDate]=useState(seoulToday),[templateId,setTemplateId]=useState(initialTemplateId)
  const [busy,setBusy]=useState(false),[uncertain,setUncertain]=useState(false),[error,setError]=useState('')
  const form=useRef<HTMLFormElement>(null),pending=useRef(false),command=useRef(crypto.randomUUID()),request=useRef<planner.CreateTaskRequest|null>(null)
  const label=type==='WORKOUT'?'운동':type==='STUDY'?'공부':'클라이밍'
  async function save() {
    if(pending.current)return
    if(!title.trim()){close();return}
    const template=templates.find(item=>item.templateId===templateId)
    if(!uncertain&&templateId&&!template){setError('선택한 항목을 불러오지 못했습니다.');return}
    pending.current=true;setBusy(true);setError('')
    if(!uncertain)request.current={commandId:command.current,title:title.trim(),taskType:type as planner.TaskType,scheduledDate:date,...(template?{templateSelection:{templateId:template.templateId,expectedTemplateVersion:template.currentVersion.templateVersion}}:{})}
    try {
      await api.tasks.createTask({createTaskRequest:request.current!})
      await queries.invalidateQueries({queryKey:['calendar']})
      await queries.invalidateQueries({queryKey:['task']})
      close()
    } catch(reason) {
      const failure=await requestFailure(reason)
      setUncertain(!failure.rejected)
      if(failure.rejected){command.current=crypto.randomUUID();request.current=null}
      setError(failure.rejected?'일정을 추가하지 못했습니다.':'저장 결과를 확인하지 못했습니다.')
    } finally {pending.current=false;setBusy(false)}
  }
  return <TaskFloatingPanel label={initialTemplateId&&initialTitle?initialTitle:`${label} 추가`} busy={busy} close={()=>{if(!uncertain)form.current?.requestSubmit()}}>
    <form ref={form} className="task-form floating-schedule-add" onSubmit={event=>{event.preventDefault();void save()}}
      onBlur={event=>{if(!event.currentTarget.contains(event.relatedTarget as Node|null)&&!busy&&!uncertain)event.currentTarget.requestSubmit()}}>
      <fieldset disabled={busy||uncertain}>
        {!(type==='WORKOUT'&&initialTemplateId)&&<><label htmlFor="floating-task-title">이름</label>
        <input id="floating-task-title" autoFocus maxLength={120} value={title} onChange={event=>setTitle(event.target.value)} onKeyDown={event=>{if(event.key==='Enter'&&!event.nativeEvent.isComposing){event.preventDefault();form.current?.requestSubmit()}}}/></>}
        <label htmlFor="floating-task-date">날짜</label>
        <input id="floating-task-date" type="date" required value={date} onChange={event=>setDate(event.target.value)}/>
        {!initialTemplateId&&<><label htmlFor="floating-task-template">{type==='STUDY'?'카테고리':'종류'}</label>
        <select id="floating-task-template" value={templateId} onChange={event=>setTemplateId(event.target.value)}>
          <option value="">자유{label}</option>
          {templates.map(template=><option key={template.templateId} value={template.templateId}>{template.currentVersion.name}</option>)}
        </select></>}
      </fieldset>
      {error&&<p role="alert">{error}</p>}
      <div className="form-actions">{uncertain&&<button type="submit" disabled={busy}>같은 요청 다시 보내기</button>}<button type="button" disabled={busy} onClick={()=>uncertain?close():form.current?.requestSubmit()}>닫기</button></div>
    </form>
  </TaskFloatingPanel>
}
