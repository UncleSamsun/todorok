import { useRef, useState } from 'react'
import { planner } from '@todorok/api-client'
import type { Task } from './model'
import { initialRule, RecurrenceFields, type RepeatOptions } from './RecurrenceFields'

export function TaskRepeatEditor({ task, busy, error, save, close }: {
  task: Readonly<Task>; busy: boolean; error: string
  save: (request: planner.CreateSeriesRequest) => Promise<void>; close: () => void
}) {
  const [repeat, setRepeat] = useState<RepeatOptions>({rule:initialRule(task.scheduledDate)})
  const [uncertain, setUncertain] = useState(false)
  const commandId = useRef(crypto.randomUUID())
  const request = useRef<planner.CreateSeriesRequest | null>(null)
  return <form className="task-form" aria-label="반복 설정" onSubmit={event => {
    event.preventDefault()
    if (busy) return
    if (!uncertain) request.current = {
      commandId:commandId.current,sourceTaskId:task.taskId,sourceTaskVersion:task.version,
      title:task.title,taskType:task.taskType,startDate:task.scheduledDate,note:task.note,
      ...repeat,
    }
    void save(request.current!).catch(reason => {
      if (reason === 'rejected') { commandId.current=crypto.randomUUID(); request.current=null; setUncertain(false) }
      else setUncertain(true)
    })
  }}>
    <fieldset disabled={busy || uncertain}>
      <label htmlFor="task-repeat-frequency">반복 종류</label>
      <select id="task-repeat-frequency" value={repeat.rule.frequency} onChange={event => setRepeat({...repeat,rule:{...repeat.rule,frequency:event.target.value as planner.RecurrenceRuleFrequencyEnum}})}>
        <option value="DAILY">일마다</option><option value="WEEKLY">주마다</option><option value="MONTHLY">월마다</option>
      </select>
      <RecurrenceFields id="task-repeat-settings" value={repeat} change={setRepeat} />
    </fieldset>
    {error && <p role="alert">{error}</p>}
    <div className="form-actions"><button type="submit" disabled={busy || (!uncertain && repeat.rule.frequency === 'WEEKLY' && !repeat.rule.weekdays.size)}>{uncertain ? '같은 요청 다시 보내기' : '반복 적용'}</button><button type="button" disabled={busy} onClick={close}>닫기</button></div>
  </form>
}
