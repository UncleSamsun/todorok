import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { CrimpTimer, type CrimpTimerRecord } from './CrimpTimer'

beforeEach(() => { vi.useFakeTimers(); vi.setSystemTime(new Date('2026-09-08T10:00:00+09:00')) })
afterEach(() => { cleanup(); vi.useRealTimers() })

it('한 라운드 뒤 중단하면 부분 클라이밍 기록을 같은 command로 저장한다', async () => {
  const persist = vi.fn(async (_record: CrimpTimerRecord) => true)
  render(<CrimpTimer persist={persist} close={() => undefined} />)
  fireEvent.click(screen.getByRole('button', { name: '타이머 시작' }))
  await act(async () => { await vi.advanceTimersByTimeAsync(13_000) })
  await act(async () => { fireEvent.click(screen.getByRole('button', { name: '중단하고 기록' })); await Promise.resolve() })

  expect(persist).toHaveBeenCalledTimes(1)
  const saved = persist.mock.calls[0]?.[0]
  expect(saved).toMatchObject({ completionStatus: 'PARTIAL', detail: { durationSeconds: 60, rounds: [{ attempts: 1, completed: true }] } })
  expect(saved?.commandId).toEqual(expect.any(String))
})
