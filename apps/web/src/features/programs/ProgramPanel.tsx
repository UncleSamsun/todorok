import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { activity } from '@todorok/api-client'
import { builtinPrograms } from './builtinPrograms'
import { useAuth } from '../auth/AuthProvider'
import { useRecordTemplates } from '../study/useStudyTemplates'
import { FloatingScheduleAdd } from '../activity/FloatingScheduleAdd'
import { TaskFloatingPanel } from '../today/TaskFloatingPanel'
import { ProgramPresetDialog } from './ProgramPresetDialog'

const nameFor = (catalogs: activity.ProgramCatalogSummary[] | undefined, enrollment: activity.ProgramEnrollmentResponse) =>
  catalogs?.find((catalog) => catalog.catalogKey === enrollment.catalogKey && catalog.catalogVersion === enrollment.catalogVersion)?.name ?? enrollment.catalogKey

export function ProgramPanel() {
  const { session, state } = useAuth(), queries = useQueryClient(), owner = state.userId ?? 'session'
  const api = useMemo(() => new activity.ActivityApi(new activity.Configuration({ basePath: '/api/activity/v1', fetchApi: session.fetch })), [session])
  const catalogs = useQuery({ queryKey: ['program-catalogs'], queryFn: ({ signal }) => api.listPrograms({ signal }) })
  const enrollments = useQuery({ queryKey: ['program-enrollments', owner], queryFn: ({ signal }) => api.listProgramEnrollments({ signal }) })
  const [selected, setSelected] = useState<activity.ProgramCatalogSummary | null>(null)
  const [selectedBuiltin,setSelectedBuiltin]=useState<typeof builtinPrograms[number]|null>(()=>builtinPrograms.find(program=>program.key===new URLSearchParams(location.search).get('program'))??null)
  const [adding,setAdding]=useState<{templateId?:string;title?:string}|null>(null)
  const templatesApi=useMemo(()=>new activity.TemplateApi(new activity.Configuration({basePath:'/api/activity/v1',fetchApi:session.fetch})),[session])
  const customTypes=useRecordTemplates(templatesApi,state.userId,activity.TemplateDomain.Workout,activity.TemplateKind.FreeWorkout,false)
  const customTemplates=customTypes.data?.pages.flatMap(page=>page.items).filter(item=>!item.archived)??[]
  const otherCatalogs=catalogs.data?.filter(catalog=>!builtinPrograms.some(program=>catalog.catalogKey===program.key||catalog.catalogKey.startsWith(program.key+'-')||catalog.source.url===program.source))??[]
  const [initialTest, setInitialTest] = useState(''), [startWeek, setStartWeek] = useState(''), [error, setError] = useState('')
  const register = useMutation({
    mutationFn: (request: activity.EnrollProgramRequest) => api.enrollProgram({ enrollProgramRequest: request }),
    onSuccess: (enrollment) => {
      queries.setQueryData<activity.ProgramEnrollmentResponse[]>(['program-enrollments', owner], (current) => [enrollment, ...(current ?? [])])
      void queries.invalidateQueries({ queryKey: ['program-enrollments', owner] })
      setSelected(null); setInitialTest(''); setStartWeek(''); setError('')
    },
    onError: () => setError('프로그램 등록을 완료하지 못했습니다. 입력과 연결 상태를 확인한 뒤 다시 시도해 주세요.'),
  })
  const active = (enrollments.data ?? []).filter((enrollment) => enrollment.status === activity.ProgramEnrollmentResponseStatusEnum.Active)

  function begin(catalog: activity.ProgramCatalogSummary) {
    setAdding(null)
    setSelected(catalog); setInitialTest(''); setStartWeek(''); setError('')
  }
  function submit(event: React.FormEvent) {
    event.preventDefault()
    if (!selected) return
    const initial = Number(initialTest), start = startWeek === '' ? undefined : Number(startWeek)
    if (!Number.isSafeInteger(initial) || initial < 0 || (start !== undefined && (!Number.isSafeInteger(start) || start < 1 || start > selected.totalWeeks))) {
      setError('초기 검사 횟수와 시작 주차를 확인해 주세요.')
      return
    }
    register.mutate({ commandId: crypto.randomUUID(), catalogKey: selected.catalogKey, catalogVersion: selected.catalogVersion, initialTestValue: initial, ...(start === undefined ? {} : { startWeek: start }) })
  }

  return <section className="program-panel" aria-label="운동 프로그램">
    <h2>운동 프로그램</h2>
    <section className="program-catalogs builtin-programs" aria-label="기본 운동 프로그램">
      {builtinPrograms.map(program => {
        return <button type="button" className="builtin-program-card" key={program.key} onClick={()=>{setAdding(null);setSelected(null);setSelectedBuiltin(program)}}>{program.name}</button>
      })}
      <button type="button" className="builtin-program-card free-workout-card" onClick={()=>setAdding({})}>자유운동</button>
      {otherCatalogs.map(catalog=><button type="button" className="builtin-program-card" key={`${catalog.catalogKey}:${catalog.catalogVersion}`} aria-label={`${catalog.name} 등록`} onClick={()=>begin(catalog)}>{catalog.name}</button>)}
      {customTemplates.map(template=><button type="button" className="builtin-program-card" key={template.templateId} onClick={()=>setAdding({templateId:template.templateId,title:template.currentVersion.name})}>{template.currentVersion.name}</button>)}
    </section>
    {selectedBuiltin&&<ProgramPresetDialog key={selectedBuiltin.key} program={selectedBuiltin} loading={enrollments.isPending||enrollments.isError} enrollment={enrollments.data?.find(item=>item.catalogKey===selectedBuiltin.key||item.catalogKey.startsWith(selectedBuiltin.key+'-'))} close={()=>setSelectedBuiltin(null)}/>}
    {customTypes.hasNextPage&&<button type="button" disabled={customTypes.isFetchingNextPage} onClick={()=>void customTypes.fetchNextPage()}>더 보기</button>}
    {adding&&<FloatingScheduleAdd key={`${adding.templateId??'free'}-${adding.title??''}`} type="WORKOUT" initialTitle={adding.title} initialTemplateId={adding.templateId} close={()=>setAdding(current=>current===adding?null:current)}/>}
    {enrollments.isPending && <p role="status">등록한 프로그램을 불러오는 중…</p>}
    {enrollments.isError && <p role="alert">등록한 프로그램을 불러오지 못했습니다.</p>}
    {active.length > 0 && <section className="program-current"><h3>진행 중인 프로그램</h3>{active.map((enrollment) => <article key={enrollment.enrollmentId}>
      <strong>{nameFor(catalogs.data, enrollment)}</strong>
      <span>{enrollment.currentWeek}주차 {enrollment.currentSession}회</span>
      <small>권장 세트</small><b>{enrollment.target.targetSets.join(' · ')}회</b>
    </article>)}</section>}
    {catalogs.isPending && <p role="status">프로그램을 불러오는 중…</p>}
    {catalogs.isError && <p role="alert">프로그램을 불러오지 못했습니다.</p>}
    {selected && <TaskFloatingPanel label={selected.name} busy={register.isPending} close={()=>setSelected(null)}><form className="program-enrollment" onSubmit={submit}>
      <h3>{selected.name} 등록</h3>
      <label>초기 검사 횟수<input aria-label="초기 검사 횟수" type="number" min="0" step="1" required value={initialTest} onChange={(event) => setInitialTest(event.target.value)} /></label>
      <label>시작 주차<input aria-label="시작 주차" type="number" min="1" max={selected.totalWeeks} step="1" value={startWeek} onChange={(event) => setStartWeek(event.target.value)} /></label>
      {error && <p role="alert">{error}</p>}
      <div className="form-actions"><button type="button" onClick={() => setSelected(null)}>닫기</button><button disabled={register.isPending} type="submit">{register.isPending ? '등록 중…' : '프로그램 시작'}</button></div>
    </form></TaskFloatingPanel>}
  </section>
}
