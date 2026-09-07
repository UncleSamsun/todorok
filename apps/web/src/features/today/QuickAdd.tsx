import { useEffect, useRef, useState, type SubmitEvent } from 'react'
import type { activity, planner } from '@todorok/api-client'
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
  save,
  cancel,
}: {
  date: string
  type: planner.TaskType
  busy: boolean
  uncertain?: boolean
  error: string
  templates?: activity.TemplateResponse[]
  save: (title: string, date: string, commandId: string, templateSelection?: planner.TemplateSelection, repeat?: RepeatOptions) => void
  cancel: () => void
}) {
  const [title, setTitle] = useState(''),
    [scheduled, setScheduled] = useState(date)
  const [frequency, setFrequency] = useState('NONE')
  const [templateId, setTemplateId] = useState('')
  const [managingTemplates, setManagingTemplates] = useState(false)
  const commandId = useRef(crypto.randomUUID())
  const [repeat, setRepeat] = useState<RepeatOptions>({
    rule: initialRule(date),
  })
  const selectedTemplate = templates.find((template) => template.templateId === templateId)
  useEffect(() => {
    if (templateId && !selectedTemplate) setTemplateId('')
  }, [templateId, selectedTemplate])
  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (
      title.trim() &&
      !busy &&
      (frequency !== 'WEEKLY' || repeat.rule.weekdays.size)
    )
      save(title.trim(), scheduled, commandId.current, type === 'STUDY' && selectedTemplate ? {
        templateId: selectedTemplate.templateId,
        expectedTemplateVersion: selectedTemplate.currentVersion.templateVersion,
      } : undefined, frequency === 'NONE' ? undefined : repeat)
  }
  return (<>
    <form className="task-form" hidden={managingTemplates} onSubmit={submit}>
      <fieldset disabled={busy || uncertain}>
      {type === 'STUDY' && <><label htmlFor="study-template">공부 카테고리</label><select id="study-template" required value={templateId} onChange={(event) => setTemplateId(event.target.value)}><option value="">선택해 주세요</option>{templates.filter((template) => !template.archived).map((template) => <option key={template.templateId} value={template.templateId}>{template.currentVersion.name}</option>)}</select><button type="button" onClick={() => setManagingTemplates(true)}>카테고리 관리</button>{selectedTemplate && <ul className="template-preview">{selectedTemplate.currentVersion.fields.map((field) => <li key={field.fieldId}>{studyFieldSummary(field)}</li>)}</ul>}</>}
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
      <div className="form-actions">
        <button type="submit" disabled={busy || !title.trim() || (type === 'STUDY' && !selectedTemplate)}>
          {busy ? '저장 중…' : uncertain ? '같은 요청 다시 보내기' : '저장'}
        </button>
        <button type="button" disabled={busy} onClick={cancel}>
          취소
        </button>
      </div>
    </form>
    {managingTemplates && <StudyTemplateManager close={() => setManagingTemplates(false)} />}
  </>)
}
