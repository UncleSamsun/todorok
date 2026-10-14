import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { planner } from '@todorok/api-client'
import { QuickAdd } from './QuickAdd'
import { TaskEditor } from './TaskEditor'

afterEach(cleanup)
it('requires an explicit time before enabling a reminder and submits both choices', () => {
  const save=vi.fn()
  render(<QuickAdd date="2026-10-13" type={planner.TaskType.General} busy={false} error="" save={save} cancel={()=>{}} />)
  fireEvent.change(screen.getByLabelText('제목'),{target:{value:'아침 계획'}})
  expect(screen.getByLabelText('이 시간에 알림 받기')).toBeDisabled()
  fireEvent.change(screen.getByLabelText('예정 시간'),{target:{value:'09:30'}})
  fireEvent.click(screen.getByLabelText('이 시간에 알림 받기'))
  fireEvent.submit(screen.getByRole('button',{name:'저장'}).closest('form')!)
  expect(save.mock.calls[0][5]).toEqual({scheduledTime:'09:30',reminderEnabled:true})
})
it('preserves the original reminder on edit and permits explicitly clearing the time', () => {
  const save=vi.fn()
  render(<TaskEditor task={{taskId:'task',userId:'owner',title:'계획',taskType:planner.TaskType.General,scheduledDate:'2026-10-13',status:planner.TaskStatus.Planned,version:0,scheduledTime:'09:30',reminderEnabled:true} as planner.TaskResponse} busy={false} error="" save={save} remove={()=>{}} skip={()=>{}} reopen={()=>{}} cancel={()=>{}} />)
  expect(screen.getByLabelText('이 시간에 알림 받기')).toBeChecked()
  fireEvent.change(screen.getByLabelText('예정 시간'),{target:{value:''}})
  expect(screen.getByLabelText('이 시간에 알림 받기')).not.toBeChecked()
  fireEvent.submit(screen.getByRole('form',{name:'할 일 수정'}))
  expect(save).toHaveBeenCalledWith('계획','2026-10-13','',{scheduledTime:'',reminderEnabled:false})
})
