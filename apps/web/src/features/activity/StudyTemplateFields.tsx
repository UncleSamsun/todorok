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
  if (field.type === activity.TemplateFieldType.Time) return `${field.name} (${field.unit || '초'})`
  return field.unit ? `${field.name} (${field.unit})` : field.name
}

const timeFactor = (field: activity.FieldDefinition) => field.unit === '시간' ? 3600 : field.unit === '분' ? 60 : 1
function seconds(value: string, field: activity.FieldDefinition) {
  const result = Number(value) * timeFactor(field), nearest = Math.round(result)
  // Only absorb binary floating point noise, never round fractional seconds.
  return Math.abs(result - nearest) <= Number.EPSILON * Math.max(1, Math.abs(result)) ? nearest : result
}
export function validateStudyFields(value: activity.StudyDetail) {
  for (const field of value.fields ?? []) {
    if (field.type === 'NUMBER' && !Number.isFinite(field.numberValue)) throw new Error('숫자는 유한한 값으로 입력해 주세요.')
    if (field.type === 'TIME' && (!Number.isSafeInteger(field.timeSeconds) || field.timeSeconds! < 0)) throw new Error('시간은 0 이상의 정수 초로 표현할 수 있어야 합니다.')
  }
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
      if (field.type === activity.TemplateFieldType.Time) input = { fieldId: field.fieldId, type: field.type, timeSeconds: seconds(next, field) }
      if (field.type === activity.TemplateFieldType.ShortText) input = { fieldId: field.fieldId, type: field.type, textValue: next }
      if (field.type === activity.TemplateFieldType.Check) input = { fieldId: field.fieldId, type: field.type, checked: next === 'true' }
      if (field.type === activity.TemplateFieldType.Memo) input = { fieldId: field.fieldId, type: field.type, memoValue: next }
    }
    const ordered = definitions.flatMap((definition) => {
      if (definition.fieldId === field.fieldId) return input ? [input] : []
      const existing = remaining.find((item) => item.fieldId === definition.fieldId)
      return existing ? [existing] : []
    })
    change({ ...value, fields: [...ordered, ...remaining.filter((item) => !definitions.some((definition) => definition.fieldId === item.fieldId))] })
  }
  return <fieldset><legend>공부 내용</legend>
    {definitions.map((field) => {
      const input = values.get(field.fieldId)
      if (field.type === activity.TemplateFieldType.Check) return <label key={field.fieldId}>{fieldLabel(field)}<select aria-label={fieldLabel(field)} value={input?.checked === undefined ? '' : String(input.checked)} onChange={(event) => set(field, event.target.value)}><option value="">미입력</option><option value="true">예</option><option value="false">아니오</option></select></label>
      if (field.type === activity.TemplateFieldType.Memo) return <label key={field.fieldId}>{fieldLabel(field)}<textarea maxLength={20000} value={input?.memoValue ?? ''} onChange={(event) => set(field, event.target.value)} /></label>
      const current = field.type === activity.TemplateFieldType.Number ? input?.numberValue : field.type === activity.TemplateFieldType.Time ? (input?.timeSeconds === undefined ? undefined : input.timeSeconds / timeFactor(field)) : input?.textValue
      return <label key={field.fieldId}>{fieldLabel(field)}<input type={field.type === activity.TemplateFieldType.Number || field.type === activity.TemplateFieldType.Time ? 'number' : 'text'} min={field.type === activity.TemplateFieldType.Time ? 0 : undefined} step="any" maxLength={field.type === activity.TemplateFieldType.ShortText ? 120 : undefined} value={current ?? ''} onChange={(event) => { const next = event.target.value; const time = seconds(next, field); event.target.setCustomValidity(field.type === 'TIME' && next !== '' && (!Number.isSafeInteger(time) || time < 0) ? '시간은 0 이상의 정수 초로 표현할 수 있어야 합니다.' : ''); set(field, next) }} /></label>
    })}
  </fieldset>
}
