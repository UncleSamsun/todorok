import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { planner } from '@todorok/api-client'
import { TaskGroups } from './TaskGroups'
import { QuickAdd } from './QuickAdd'
import { InlineTaskName } from './InlineTaskName'
import { TaskEditor } from './TaskEditor'

afterEach(cleanup)

it('saves an existing name on blur without opening the settings dialog', async () => {
  const save=vi.fn().mockResolvedValue(true),close=vi.fn()
  const task={taskId:'task',title:'산책',taskType:planner.TaskType.General,status:planner.TaskStatus.Planned} as planner.TaskResponse
  render(<InlineTaskName task={task} busy={false} save={save} close={close} />)
  const input=screen.getByLabelText('산책 이름')
  fireEvent.change(input,{target:{value:'저녁 산책'}})
  fireEvent.blur(input)
  expect(save).toHaveBeenCalledWith(task,'저녁 산책')
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
})

it('keeps schedule and memo fields in separate editors', () => {
  const task={taskId:'task',title:'산책',scheduledDate:'2026-10-15',taskType:planner.TaskType.General,status:planner.TaskStatus.Planned} as planner.TaskResponse
  const props={task,busy:false,error:'',save:vi.fn(),remove:vi.fn(),skip:vi.fn(),reopen:vi.fn(),cancel:vi.fn()}
  const {rerender}=render(<TaskEditor {...props} mode="schedule" />)
  expect(screen.getByLabelText('제목 수정')).toBeVisible()
  expect(screen.getByLabelText('날짜 수정')).toBeVisible()
  expect(screen.queryByLabelText('할 일 메모')).not.toBeInTheDocument()
  expect(screen.queryByLabelText('예정 시간')).not.toBeInTheDocument()
  rerender(<TaskEditor {...props} mode="memo" />)
  expect(screen.getByLabelText('할 일 메모')).toBeVisible()
  expect(screen.queryByLabelText('제목 수정')).not.toBeInTheDocument()
  expect(screen.queryByLabelText('날짜 수정')).not.toBeInTheDocument()
})

it('places the new task draft inside its category and opens settings from the row menu', () => {
  const edit = vi.fn()
  const task = {taskId:'task', title:'산책', taskType:planner.TaskType.General, status:planner.TaskStatus.Planned} as planner.TaskResponse
  render(<TaskGroups tasks={[task]} busy={false} add={vi.fn()} check={vi.fn()} edit={edit}
    adding={planner.TaskType.General} addForm={<div data-testid="draft">새 할 일</div>} />)
  expect(screen.getByTestId('draft').closest('section')).toHaveTextContent('할 일')
  fireEvent.click(screen.getByRole('button', {name:'산책 메뉴'}))
  fireEvent.click(screen.getByRole('menuitem', {name:'일정 수정'}))
  expect(edit).toHaveBeenCalledWith(task, 'task')
})

it('starts with only the name in the draft and reveals options from the ellipsis', () => {
  const save = vi.fn()
  render(<QuickAdd inline date="2026-10-15" type={planner.TaskType.General} busy={false} error="" save={save} cancel={vi.fn()} />)
  expect(screen.getByLabelText('제목')).toBeVisible()
  expect(screen.getByLabelText('날짜')).not.toBeVisible()
  fireEvent.click(screen.getByRole('button', {name:'새 할 일 메뉴'}))
  fireEvent.click(screen.getByRole('menuitem',{name:'일정 수정'}))
  expect(screen.getByLabelText('날짜')).toBeVisible()
  expect(screen.queryByLabelText('예정 시간')).not.toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('제목'), {target:{value:'산책'}})
  fireEvent.submit(screen.getByLabelText('제목').closest('form')!)
  expect(save).toHaveBeenCalledWith('산책','2026-10-15',expect.any(String),undefined,undefined,undefined)
})

it('automatically saves on leaving the row, but not when moving to its options', () => {
  const save=vi.fn()
  const cancel=vi.fn()
  render(<QuickAdd inline date="2026-10-15" type={planner.TaskType.General} busy={false} error="" save={save} cancel={cancel} />)
  const input=screen.getByLabelText('제목')
  expect(screen.queryByRole('button',{name:'저장'})).not.toBeInTheDocument()
  fireEvent.blur(input,{relatedTarget:document.body})
  expect(save).not.toHaveBeenCalled()
  expect(cancel).toHaveBeenCalledTimes(1)
  fireEvent.change(input,{target:{value:'산책'}})
  fireEvent.blur(input,{relatedTarget:screen.getByRole('button',{name:'새 할 일 메뉴'})})
  expect(save).not.toHaveBeenCalled()
  fireEvent.blur(input,{relatedTarget:document.body})
  expect(save).toHaveBeenCalledTimes(1)
  fireEvent.blur(input,{relatedTarget:document.body})
  expect(save).toHaveBeenCalledTimes(1)
})
