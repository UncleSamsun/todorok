import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { activity } from '@todorok/api-client'
import { SyncStatus } from './SyncStatus'

afterEach(cleanup)

it.each(['APPLIED', 'NOT_REQUIRED'] as const)('hides manual confirmation for final %s and never displays server reason codes', (syncState) => {
  const check = vi.fn()
  render(<SyncStatus value={{ syncState, syncReason: syncState } as activity.ActivityResponse} checking={false} check={check} />)
  expect(screen.queryByText(syncState)).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: '상태 다시 확인' })).not.toBeInTheDocument()
})

it.each(['PENDING', 'CONFLICT'] as const)('keeps confirmation useful for %s without exposing arbitrary backend reasons', (syncState) => {
  const check = vi.fn()
  render(<SyncStatus value={{ syncState, syncReason: 'INTERNAL_PLANNER_ERROR' } as activity.ActivityResponse} checking={false} check={check} />)
  expect(screen.queryByText('INTERNAL_PLANNER_ERROR')).not.toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: '상태 다시 확인' }))
  expect(check).toHaveBeenCalledOnce()
})

it('explains a deleted linked task and disables confirmation while checking', () => {
  render(<SyncStatus value={{ syncState: 'CONFLICT', syncReason: 'TASK_DELETED' } as activity.ActivityResponse} checking={true} check={vi.fn()} />)
  expect(screen.getByText('연결된 할 일이 삭제되어 일정에 반영하지 못했습니다.')).toBeVisible()
  expect(screen.getByRole('button', { name: '확인 중…' })).toBeDisabled()
  expect(screen.queryByText('TASK_DELETED')).not.toBeInTheDocument()
})
