import { activity } from '@todorok/api-client'

const fieldTypeLabel: Record<activity.TemplateFieldType, string> = {
  NUMBER: '숫자',
  TIME: '시간',
  SHORT_TEXT: '짧은 글',
  CHECK: '체크',
  MEMO: '긴 메모',
  '11184809': '알 수 없음',
}

export function studyFieldSummary(field: activity.FieldDefinition) {
  return [field.name, fieldTypeLabel[field.type], field.unit].filter(Boolean).join(' · ')
}

function fieldLabel(field: activity.FieldDefinition) {
  if (field.type === activity.TemplateFieldType.Time) return `${field.name} (초${field.unit ? `, 표시 단위: ${field.unit}` : ''})`
  return field.unit ? `${field.name} (${field.unit})` : field.name
}

export function StudyTemplateFields({
  definitions,
  value,
  change,
}: {
  definitions: activity.FieldDefinition[]
  value: activity.StudyDetail
  change: (value: activity.StudyDetail) => void
}) {
  const values = new Map((value.fields ?? []).map((field) => [field.fieldId, field]))
  function set(field: activity.FieldDefinition, next: string) {
    const remaining = (value.fields ?? []).filter((item) => item.fieldId !== field.fieldId)
    let input: activity.FieldInput | undefined
    if (next !== '') {
      if (field.type === activity.TemplateFieldType.Number) input = { fieldId: field.fieldId, type: field.type, numberValue: Number(next) }
      if (field.type === activity.TemplateFieldType.Time) input = { fieldId: field.fieldId, type: field.type, timeSeconds: Number(next) }
      if (field.type === activity.TemplateFieldType.ShortText) input = { fieldId: field.fieldId, type: field.type, textValue: next }
      if (field.type === activity.TemplateFieldType.Check) input = { fieldId: field.fieldId, type: field.type, checked: next === 'true' }
      if (field.type === activity.TemplateFieldType.Memo) input = { fieldId: field.fieldId, type: field.type, memoValue: next }
    }
    const ordered = definitions.flatMap((definition) => {
      if (definition.fieldId === field.fieldId) return input ? [input] : []
      const existing = remaining.find((item) => item.fieldId === definition.fieldId)
      return existing ? [existing] : []
    })
    change({ ...value, fields: ordered })
  }
  return <fieldset><legend>공부 내용</legend>
    {definitions.map((field) => {
      const input = values.get(field.fieldId)
      if (field.type === activity.TemplateFieldType.Check) return <label key={field.fieldId}>{fieldLabel(field)}<select aria-label={fieldLabel(field)} value={input?.checked === undefined ? '' : String(input.checked)} onChange={(event) => set(field, event.target.value)}><option value="">미입력</option><option value="true">예</option><option value="false">아니오</option></select></label>
      if (field.type === activity.TemplateFieldType.Memo) return <label key={field.fieldId}>{fieldLabel(field)}<textarea maxLength={20000} value={input?.memoValue ?? ''} onChange={(event) => set(field, event.target.value)} /></label>
      const current = field.type === activity.TemplateFieldType.Number ? input?.numberValue : field.type === activity.TemplateFieldType.Time ? input?.timeSeconds : input?.textValue
      return <label key={field.fieldId}>{fieldLabel(field)}<input type={field.type === activity.TemplateFieldType.Number || field.type === activity.TemplateFieldType.Time ? 'number' : 'text'} min={field.type === activity.TemplateFieldType.Number || field.type === activity.TemplateFieldType.Time ? 0 : undefined} step={field.type === activity.TemplateFieldType.Time ? 1 : undefined} maxLength={field.type === activity.TemplateFieldType.ShortText ? 120 : undefined} value={current ?? ''} onChange={(event) => set(field, event.target.value)} /></label>
    })}
  </fieldset>
}
