import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { activity } from '@todorok/api-client'
import { useAuth } from '../auth/AuthProvider'

const nameFor = (catalogs: activity.ProgramCatalogSummary[] | undefined, enrollment: activity.ProgramEnrollmentResponse) =>
  catalogs?.find((catalog) => catalog.catalogKey === enrollment.catalogKey && catalog.catalogVersion === enrollment.catalogVersion)?.name ?? enrollment.catalogKey

export function ProgramPanel() {
  const { session, state } = useAuth(), queries = useQueryClient(), owner = state.userId ?? 'session'
  const api = useMemo(() => new activity.ActivityApi(new activity.Configuration({ basePath: '/api/activity/v1', fetchApi: session.fetch })), [session])
  const catalogs = useQuery({ queryKey: ['program-catalogs'], queryFn: ({ signal }) => api.listPrograms({ signal }) })
  const enrollments = useQuery({ queryKey: ['program-enrollments', owner], queryFn: ({ signal }) => api.listProgramEnrollments({ signal }) })
  const [selected, setSelected] = useState<activity.ProgramCatalogSummary | null>(null)
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
    {enrollments.isPending && <p role="status">등록한 프로그램을 불러오는 중…</p>}
    {enrollments.isError && <p role="alert">등록한 프로그램을 불러오지 못했습니다.</p>}
    {active.length > 0 && <section className="program-current"><h3>진행 중인 프로그램</h3>{active.map((enrollment) => <article key={enrollment.enrollmentId}>
      <strong>{nameFor(catalogs.data, enrollment)}</strong>
      <span>{enrollment.currentWeek}주차 {enrollment.currentSession}회</span>
      <small>권장 세트</small><b>{enrollment.target.targetSets.join(' · ')}회</b>
    </article>)}</section>}
    {catalogs.isPending && <p role="status">프로그램을 불러오는 중…</p>}
    {catalogs.isError && <p role="alert">프로그램을 불러오지 못했습니다.</p>}
    {catalogs.data && <section className="program-catalogs"><h3>프로그램 선택</h3>{catalogs.data.map((catalog) => <article key={`${catalog.catalogKey}:${catalog.catalogVersion}`}>
      <div><strong>{catalog.name}</strong><p>{catalog.totalWeeks}주 · 주 {catalog.sessionsPerWeek}회</p><small className="program-source">{catalog.source.label}{catalog.source.url && <> · <a href={catalog.source.url} target="_blank" rel="noreferrer">출처 원문</a></>}</small>{catalog.source.conditions.length > 0 && <p className="program-source">조건: {catalog.source.conditions.join(' · ')}</p>}{catalog.source.cautions.length > 0 && <p className="program-source">주의: {catalog.source.cautions.join(' · ')}</p>}</div>
      <button onClick={() => begin(catalog)}>{catalog.name} 등록</button>
    </article>)}</section>}
    {selected && <form className="program-enrollment" onSubmit={submit}>
      <h3>{selected.name} 등록</h3>
      <label>초기 검사 횟수<input aria-label="초기 검사 횟수" type="number" min="0" step="1" required value={initialTest} onChange={(event) => setInitialTest(event.target.value)} /></label>
      <label>시작 주차<input aria-label="시작 주차" type="number" min="1" max={selected.totalWeeks} step="1" value={startWeek} onChange={(event) => setStartWeek(event.target.value)} /><small>비우면 권장 주차에서 시작합니다.</small></label>
      {error && <p role="alert">{error}</p>}
      <div className="form-actions"><button type="button" onClick={() => setSelected(null)}>취소</button><button disabled={register.isPending} type="submit">{register.isPending ? '등록 중…' : '프로그램 시작'}</button></div>
    </form>}
  </section>
}
