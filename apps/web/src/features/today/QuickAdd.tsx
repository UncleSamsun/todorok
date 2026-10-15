import { useEffect, useRef, useState, type SubmitEvent } from 'react'
import { activity, type planner } from '@todorok/api-client'
import { studyFieldSummary } from '../activity/StudyTemplateFields'
import { StudyTemplateManager } from '../study/StudyTemplateManager'
import { builtinPrograms } from '../programs/builtinPrograms'
import { TaskReminderFields, type TaskReminder } from './TaskReminderFields'
import {
  RecurrenceFields,
  initialRule,
  type RepeatOptions,
} from './RecurrenceFields'
export function QuickAdd({
  date,
  type,
  busy,
  uncertain = false,
  error,
  templates = [],
  loadMore,
  loadingMore = false,
  templateConflict = false,
  confirmTemplate,
  rejectedAttempt = 0,
  save,
  cancel,
  inline = false,
  chooseProgram,
  initialTemplateId = '',
}: {
  date: string
  type: planner.TaskType
  busy: boolean
  uncertain?: boolean
  error: string
  templates?: activity.TemplateResponse[]
  loadMore?: () => void
  loadingMore?: boolean
  templateConflict?: boolean
  confirmTemplate?: () => void
  rejectedAttempt?: number
  save: (title: string, date: string, commandId: string, templateSelection?: planner.TemplateSelection, repeat?: RepeatOptions, reminder?: TaskReminder) => void
  cancel: () => void
  inline?: boolean
  chooseProgram?: (key: string) => void
  initialTemplateId?: string
}) {
  const [title, setTitle] = useState(''),
    [scheduled, setScheduled] = useState(date)
  const [frequency, setFrequency] = useState('NONE')
  const [optionsOpen, setOptionsOpen] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const [optionMode, setOptionMode] = useState<'task' | 'repeat'>('task')
  const [reminder, setReminder] = useState<TaskReminder>({ scheduledTime: '', reminderEnabled: false })
  const [templateId, setTemplateId] = useState(initialTemplateId)
  const [managingTemplates, setManagingTemplates] = useState<activity.TemplateKind | null>(null)
  const commandId = useRef(crypto.randomUUID())
  const submitted = useRef<Parameters<typeof save> | null>(null)
  const submittedTemplate = useRef<activity.TemplateResponse | undefined>(undefined)
  useEffect(() => { commandId.current = crypto.randomUUID(); submitted.current = null }, [rejectedAttempt])
  const [selectedSnapshot, setSelectedSnapshot] = useState<activity.TemplateResponse | undefined>()
  const [repeat, setRepeat] = useState<RepeatOptions>({
    rule: initialRule(date),
  })
  const listedTemplate = templates.find((template) => template.templateId === templateId)
  const selectedTemplate = uncertain ? selectedSnapshot : listedTemplate ?? selectedSnapshot
  const supportsTemplate = type === 'STUDY' || type === 'WORKOUT' || type === 'CLIMBING'
  const templateLabel = type === 'STUDY' ? '공부 카테고리' : type === 'WORKOUT' ? '운동 기록 유형' : '클라이밍 기록 유형'
  const typeLabel = type === 'STUDY' ? '공부' : type === 'WORKOUT' ? '운동' : type === 'CLIMBING' ? '클라이밍' : '할 일'
  const templateNoun = type === 'STUDY' ? '카테고리' : '기록 유형'
  useEffect(() => {
    if (listedTemplate && !uncertain) setSelectedSnapshot(listedTemplate)
  }, [listedTemplate, uncertain])
  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy || templateConflict || (inline && submitted.current && !uncertain)) return
    if (uncertain && submitted.current) { save(...structuredClone(submitted.current)); return }
    if (
      title.trim() &&
      !busy &&
      (frequency !== 'WEEKLY' || repeat.rule.weekdays.size)
    )
    {
      submittedTemplate.current = selectedTemplate ? structuredClone(selectedTemplate) : undefined
      submitted.current = structuredClone([title.trim(), scheduled, commandId.current, supportsTemplate && selectedTemplate ? {
        templateId: selectedTemplate.templateId,
        expectedTemplateVersion: selectedTemplate.currentVersion.templateVersion,
      } : undefined, frequency === 'NONE' ? undefined : repeat, frequency === 'NONE' && reminder.scheduledTime ? reminder : undefined])
      save(...structuredClone(submitted.current))
    }
  }
  return (<>
    <form className={`task-form${inline ? ' task-inline-add' + (supportsTemplate ? ' has-template-picker' : '') : ''}`} hidden={Boolean(managingTemplates)} onSubmit={submit}
      onBlur={event => {
        if (inline && !event.currentTarget.contains(event.relatedTarget as Node | null) && !busy && !uncertain && !submitted.current) {
          if (title.trim()) event.currentTarget.requestSubmit()
          else cancel()
        }
      }}>
      <fieldset disabled={busy || uncertain}>
      {supportsTemplate && <>
        <label className={inline ? 'visually-hidden' : undefined} htmlFor="record-template">{templateLabel}</label>
        <select className={inline ? 'inline-template-select' : undefined} id="record-template" required={!inline && type === 'STUDY'} value={templateId}
          onChange={event => {
            if(event.target.value.startsWith('program:')) { chooseProgram?.(event.target.value.slice(8)); return }
            if(event.target.value === '__more__') { loadMore?.(); return }
            setTemplateId(event.target.value)
            setSelectedSnapshot(templates.find(item=>item.templateId===event.target.value))
          }}>
          <option value="">{inline ? `자유${typeLabel}` : type === 'STUDY' ? '카테고리 선택' : type === 'WORKOUT' ? '자유운동' : '기본 기록 (사용자 항목 없음)'}</option>
          {type === 'WORKOUT' && chooseProgram && builtinPrograms.map(program=><option key={program.key} value={`program:${program.key}`}>{program.name}</option>)}
          {selectedSnapshot && !templates.some(item=>item.templateId===selectedSnapshot.templateId&&!item.archived) && <option value={selectedSnapshot.templateId}>{selectedSnapshot.currentVersion.name} · 이전 선택</option>}
          {templates.filter(item=>!item.archived).map(item=><option key={item.templateId} value={item.templateId}>{item.currentVersion.name}</option>)}
          {inline && loadMore && <option value="__more__" disabled={loadingMore}>{loadingMore ? '불러오는 중…' : '더 보기…'}</option>}
        </select>
        {!inline && <>
          {loadMore && <button type="button" disabled={loadingMore} onClick={loadMore}>{templateNoun} 더 보기</button>}
          {type === 'CLIMBING' ? <><button type="button" onClick={()=>setManagingTemplates(activity.TemplateKind.ClimbingSession)}>세션 유형 관리</button><button type="button" onClick={()=>setManagingTemplates(activity.TemplateKind.FreeHangboard)}>행보드 유형 관리</button></> : <button type="button" onClick={()=>setManagingTemplates(type==='STUDY'?activity.TemplateKind.StudyCategory:activity.TemplateKind.FreeWorkout)}>{templateNoun} 관리</button>}
          {selectedTemplate && <ul className="template-preview">{selectedTemplate.currentVersion.fields.map(field=><li key={field.fieldId}>{studyFieldSummary(field)}</li>)}</ul>}
        </>}
      </>}
      {inline && <span className="draft-check" aria-hidden="true">○</span>}
      <label className={inline ? 'visually-hidden' : undefined} htmlFor="task-title">제목</label>
      <input
        autoFocus
        id="task-title"
        value={title}
        maxLength={120}
        required
        placeholder={inline ? `${typeLabel} 이름 입력` : undefined}
        onKeyDown={event => {
          if (inline && event.key === 'Escape' && !busy && !uncertain) { event.preventDefault(); cancel() }
          if (inline && event.key === 'Enter' && !event.nativeEvent.isComposing) { event.preventDefault(); event.currentTarget.form?.requestSubmit() }
        }}
        onChange={(e) => setTitle(e.target.value)}
      />
      {inline && <button type="button" className="inline-options-toggle" aria-label={`새 ${typeLabel} 메뉴`} aria-expanded={menuOpen} aria-controls="new-task-menu" onClick={() => { setMenuOpen(!menuOpen); setOptionsOpen(false) }}>…</button>}
      {inline && menuOpen && <div className="task-menu-popup draft-menu" id="new-task-menu" role="menu">
        <button type="button" role="menuitem" onClick={()=>{setOptionMode('task');setMenuOpen(false);setOptionsOpen(true)}}>일정 수정</button>
        <button type="button" role="menuitem" onClick={()=>{setOptionMode('repeat');setMenuOpen(false);setOptionsOpen(true)}}>반복 설정</button>
      </div>}
      <details id="new-task-options" open={inline ? optionsOpen : undefined}>
        <summary hidden={inline}>옵션</summary>
        {inline && <header><strong>{optionMode === 'task' ? '일정 수정' : '반복 설정'}</strong></header>}
        {(!inline || optionMode === 'task') && <>
        <label htmlFor="task-date">날짜</label>
        <input
          id="task-date"
          type="date"
          required
          value={scheduled}
          onChange={(e) => setScheduled(e.target.value)}
        />
        {!inline && frequency === 'NONE' && <TaskReminderFields id="new-task" value={reminder} change={setReminder} />}
        {frequency !== 'NONE' && <p>시간·알림은 등록 후 각 회차의 할 일 수정에서 지정할 수 있습니다.</p>}
        </>}
        {(!inline || optionMode === 'repeat') && <>
        <label htmlFor="task-repeat">반복</label>
        <select
          id="task-repeat"
          value={frequency}
          onChange={(e) => {
            setFrequency(e.target.value)
            if (e.target.value !== 'NONE')
              setRepeat({
                ...repeat,
                rule: {
                  ...(frequency === 'NONE'
                    ? initialRule(scheduled)
                    : repeat.rule),
                  frequency: e.target
                    .value as planner.RecurrenceRuleFrequencyEnum,
                },
              })
          }}
        >
          <option value="NONE">반복 안 함</option>
          <option value="DAILY">일마다</option>
          <option value="WEEKLY">주마다</option>
          <option value="MONTHLY">월마다</option>
        </select>
        {frequency !== 'NONE' && (
          <RecurrenceFields
            id="task-repeat"
            value={repeat}
            change={setRepeat}
          />
        )}
        </>}
        {inline && <div className="form-actions"><button type="button" onClick={()=>setOptionsOpen(false)}>닫기</button></div>}
      </details>
      {!inline && type !== 'GENERAL' && (
        <p>완료 체크를 누르면 해당 기록 화면으로 이동합니다.</p>
      )}
      </fieldset>
      {error && <p role="alert">{error}</p>}
      {templateConflict && <section><p>카테고리를 다시 선택하고 현재 항목을 확인해 주세요. 제목·날짜·반복 초안은 유지됩니다.</p>{submittedTemplate.current && <><h3>이전 선택 · {submittedTemplate.current.currentVersion.name} · 버전 {submittedTemplate.current.currentVersion.templateVersion}</h3><ul>{submittedTemplate.current.currentVersion.fields.map((field) => <li key={field.fieldId}>{studyFieldSummary(field)}</li>)}</ul></>}<button type="button" disabled={!listedTemplate || listedTemplate.archived || busy} onClick={confirmTemplate}>최신 카테고리 확인</button></section>}
      {(!inline || uncertain || error) && <div className="form-actions">
        <button type="submit" disabled={busy || templateConflict || (!uncertain && (!title.trim() || (!inline && type === 'STUDY' && (!selectedTemplate || selectedTemplate.archived)) || (selectedTemplate?.archived ?? false)))}>
          {busy ? '저장 중…' : uncertain ? '같은 요청 다시 보내기' : '저장'}
        </button>
        <button type="button" disabled={busy} onClick={cancel}>
          취소
        </button>
      </div>}
    </form>
    {managingTemplates && <StudyTemplateManager close={() => setManagingTemplates(null)} {...(type === 'WORKOUT' ? { domain: activity.TemplateDomain.Workout, kind: activity.TemplateKind.FreeWorkout, section: '운동', noun: '기록 유형' } : type === 'CLIMBING' ? { domain: activity.TemplateDomain.Climbing, kind: managingTemplates, section: '클라이밍', noun: managingTemplates === activity.TemplateKind.FreeHangboard ? '행보드 유형' : '세션 유형' } : {})} />}
  </>)
}
