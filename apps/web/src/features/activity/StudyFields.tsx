import type { activity } from '@todorok/api-client'
import { StudyTemplateFields } from './StudyTemplateFields'
export function StudyFields({ value, change, definitions }: { value: activity.StudyDetail; change: (v: activity.StudyDetail) => void; definitions?: activity.FieldDefinition[] }) {
  if (definitions) return <StudyTemplateFields definitions={definitions} value={value} change={change} />
  return <fieldset><legend>공부 내용</legend><label>과목<input value={value.subject ?? ''} onChange={(e) => change({ ...value, subject: e.target.value || undefined })}/></label><label>집중 시간(분)<input type="number" min="0" value={value.durationMinutes ?? ''} onChange={(e) => change({ ...value, durationMinutes: e.target.value === '' ? undefined : Number(e.target.value) })}/></label></fieldset>
}
