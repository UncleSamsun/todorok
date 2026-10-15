export type TaskReminder = { scheduledTime: string; reminderEnabled: boolean }

export function TaskReminderFields({ id, value, change, disabled = false }: {
  id: string; value: TaskReminder; change: (value: TaskReminder) => void; disabled?: boolean
}) {
  return <fieldset className="task-reminder-fields" disabled={disabled}>
    <label htmlFor={`${id}-time`}>예정 시간</label>
    <input id={`${id}-time`} type="time" value={value.scheduledTime} onChange={event => change({
      scheduledTime: event.target.value, reminderEnabled: Boolean(event.target.value) && value.reminderEnabled,
    })} />
    <label className="reminder-choice" htmlFor={`${id}-reminder`}><input id={`${id}-reminder`} type="checkbox"
      disabled={!value.scheduledTime} checked={value.reminderEnabled}
      onChange={event => change({ ...value, reminderEnabled: event.target.checked })} />이 시간에 알림 받기</label>
    {!value.scheduledTime && <small className="reminder-hint">예정 시간을 입력하면 알림을 선택할 수 있습니다.</small>}
    <p>한국 시간 기준입니다. 기기 알림을 설정에서 켜야 받을 수 있습니다. 22시~07시에는 보내지 않으며, 30분이 지난 알림은 만료됩니다.</p>
  </fieldset>
}
