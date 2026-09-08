import { useEffect, useRef, useState, type SubmitEvent } from 'react'
import { activity, type planner } from '@todorok/api-client'
import { studyFieldSummary } from '../activity/StudyTemplateFields'
import { StudyTemplateManager } from '../study/StudyTemplateManager'
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
  save: (title: string, date: string, commandId: string, templateSelection?: planner.TemplateSelection, repeat?: RepeatOptions) => void
  cancel: () => void
}) {
  const [title, setTitle] = useState(''),
    [scheduled, setScheduled] = useState(date)
  const [frequency, setFrequency] = useState('NONE')
  const [templateId, setTemplateId] = useState('')
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
  const templateNoun = type === 'STUDY' ? '카테고리' : '기록 유형'
  useEffect(() => {
    if (listedTemplate && !uncertain) setSelectedSnapshot(listedTemplate)
  }, [listedTemplate, uncertain])
  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy || templateConflict) return
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
      } : undefined, frequency === 'NONE' ? undefined : repeat])
      save(...structuredClone(submitted.current))
    }
  }
  return (<>
    <form className="task-form" hidden={Boolean(managingTemplates)} onSubmit={submit}>
      <fieldset disabled={busy || uncertain}>
      {supportsTemplate && <><label htmlFor="record-template">{templateLabel}</label><select id="record-template" required={type === 'STUDY'} value={templateId} onChange={(event) => { setTemplateId(event.target.value); setSelectedSnapshot(templates.find((item) => item.templateId === event.target.value)) }}><option value="">{type === 'STUDY' ? '선택해 주세요' : '기본 기록 (사용자 항목 없음)'}</option>{selectedSnapshot && !templates.some((item) => item.templateId === selectedSnapshot.templateId && !item.archived) && <option value={selectedSnapshot.templateId}>{selectedSnapshot.currentVersion.name} · 이전 선택</option>}{templates.filter((template) => !template.archived).map((template) => <option key={template.templateId} value={template.templateId}>{template.currentVersion.name}</option>)}</select>{loadMore && <button type="button" disabled={loadingMore} onClick={loadMore}>{templateNoun} 더 보기</button>}{type === 'CLIMBING' ? <><button type="button" onClick={() => setManagingTemplates(activity.TemplateKind.ClimbingSession)}>세션 유형 관리</button><button type="button" onClick={() => setManagingTemplates(activity.TemplateKind.FreeHangboard)}>행보드 유형 관리</button></> : <button type="button" onClick={() => setManagingTemplates(type === 'STUDY' ? activity.TemplateKind.StudyCategory : activity.TemplateKind.FreeWorkout)}>{templateNoun} 관리</button>}{selectedTemplate && <ul className="template-preview">{selectedTemplate.currentVersion.fields.map((field) => <li key={field.fieldId}>{studyFieldSummary(field)}</li>)}</ul>}</>}
      <label htmlFor="task-title">제목</label>
      <input
        autoFocus
        id="task-title"
        value={title}
        maxLength={120}
        required
        onChange={(e) => setTitle(e.target.value)}
      />
      <details>
        <summary>옵션</summary>
        <label htmlFor="task-date">날짜</label>
        <input
          id="task-date"
          type="date"
          required
          value={scheduled}
          onChange={(e) => setScheduled(e.target.value)}
        />
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
      </details>
      {type !== 'GENERAL' && (
        <p>완료 체크를 누르면 해당 기록 화면으로 이동합니다.</p>
      )}
      </fieldset>
      {error && <p role="alert">{error}</p>}
      {templateConflict && <section><p>카테고리를 다시 선택하고 현재 항목을 확인해 주세요. 제목·날짜·반복 초안은 유지됩니다.</p>{submittedTemplate.current && <><h3>이전 선택 · {submittedTemplate.current.currentVersion.name} · 버전 {submittedTemplate.current.currentVersion.templateVersion}</h3><ul>{submittedTemplate.current.currentVersion.fields.map((field) => <li key={field.fieldId}>{studyFieldSummary(field)}</li>)}</ul></>}<button type="button" disabled={!listedTemplate || listedTemplate.archived || busy} onClick={confirmTemplate}>최신 카테고리 확인</button></section>}
      <div className="form-actions">
        <button type="submit" disabled={busy || templateConflict || (!uncertain && (!title.trim() || (type === 'STUDY' && (!selectedTemplate || selectedTemplate.archived)) || (selectedTemplate?.archived ?? false)))}>
          {busy ? '저장 중…' : uncertain ? '같은 요청 다시 보내기' : '저장'}
        </button>
        <button type="button" disabled={busy} onClick={cancel}>
          취소
        </button>
      </div>
    </form>
    {managingTemplates && <StudyTemplateManager close={() => setManagingTemplates(null)} {...(type === 'WORKOUT' ? { domain: activity.TemplateDomain.Workout, kind: activity.TemplateKind.FreeWorkout, section: '운동', noun: '기록 유형' } : type === 'CLIMBING' ? { domain: activity.TemplateDomain.Climbing, kind: managingTemplates, section: '클라이밍', noun: managingTemplates === activity.TemplateKind.FreeHangboard ? '행보드 유형' : '세션 유형' } : {})} />}
  </>)
}
