import type { activity } from '@todorok/api-client'
import { studyFieldSummary } from './StudyTemplateFields'

export const sameField = (before: activity.FieldDefinition, after: activity.FieldDefinition) => before.fieldId === after.fieldId && before.type === after.type && (before.unit ?? '') === (after.unit ?? '')
function inputText(input?: activity.FieldInput) {
  if (!input) return '미입력'
  return String(input.numberValue ?? input.timeSeconds ?? input.textValue ?? input.checked ?? input.memoValue ?? '') + (input.type === 'TIME' ? '초' : '')
}
export function PreviousStudyInput({ template, study }: { template: activity.TemplateVersion; study: activity.StudyDetail }) {
  return <section className="previous-input"><h3>이전 입력 · {template.name} · 버전 {template.templateVersion}</h3><p>현재 세션에서만 보관됩니다. 최신 기록에 자동으로 저장되지 않으며 로그아웃하거나 새로고침하면 정리됩니다.</p><dl>{template.fields.map((field) => <div key={field.fieldId}><dt>{studyFieldSummary(field)}</dt><dd style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{inputText(study.fields?.find((input) => input.fieldId === field.fieldId))}</dd></div>)}</dl></section>
}
export function TemplateChange({ before, after, study, apply, disabled }: { before: activity.TemplateVersion; after: activity.TemplateVersion; study: activity.StudyDetail; apply: () => void; disabled: boolean }) {
  return <section className="template-change"><h2>카테고리 정의가 변경되었습니다</h2><p>버전 {before.templateVersion} → {after.templateVersion}. 동일한 항목·형식·단위의 입력만 옮깁니다. 제거되거나 형식·단위가 바뀐 값은 이전 입력에 보존됩니다.</p>
    <PreviousStudyInput template={before} study={study} />
    <h3>최신 항목 · {after.name}</h3><ul>{after.fields.map((field) => { const old = before.fields.find((item) => item.fieldId === field.fieldId); return <li key={field.fieldId}>{studyFieldSummary(field)} — {old && sameField(old, field) ? '입력 유지 가능' : old ? '형식·단위 변경: 새 입력 필요' : '새 항목'}</li> })}</ul>
    <button type="button" disabled={disabled} onClick={apply}>변경 확인 후 최신 항목 적용</button>
  </section>
}
