import { useMemo } from 'react'
import { activity } from '@todorok/api-client'
import { useAuth } from '../auth/AuthProvider'
import { useRecordTemplates } from '../study/useStudyTemplates'

export function DomainTemplateCards({ type, title, add }: {type:'STUDY'|'CLIMBING';title:string;add:(template?:activity.TemplateResponse)=>void}) {
  const {session,state}=useAuth()
  const api=useMemo(()=>new activity.TemplateApi(new activity.Configuration({basePath:'/api/activity/v1',fetchApi:session.fetch})),[session])
  const domain=type==='STUDY'?activity.TemplateDomain.Study:activity.TemplateDomain.Climbing
  const main=useRecordTemplates(api,state.userId,domain,type==='STUDY'?activity.TemplateKind.StudyCategory:activity.TemplateKind.ClimbingSession,false)
  const hangboard=useRecordTemplates(api,state.userId,activity.TemplateDomain.Climbing,activity.TemplateKind.FreeHangboard,false,type==='CLIMBING')
  const templates=[...(main.data?.pages.flatMap(page=>page.items)??[]),...(type==='CLIMBING'?hangboard.data?.pages.flatMap(page=>page.items)??[]:[])].filter(item=>!item.archived)
  return <section className="domain-template-cards" aria-label={`${title} 카테고리`}>
    {templates.map(template=><button type="button" className="domain-template-card" key={template.templateId} onClick={()=>add(template)}>{template.currentVersion.name}</button>)}
    <button type="button" className="domain-template-card" onClick={()=>add()}>자유{title}</button>
    {(main.hasNextPage||hangboard.hasNextPage)&&<button type="button" disabled={main.isFetchingNextPage||hangboard.isFetchingNextPage} onClick={()=>{if(main.hasNextPage)void main.fetchNextPage();if(hangboard.hasNextPage)void hangboard.fetchNextPage()}}>더 보기</button>}
  </section>
}
