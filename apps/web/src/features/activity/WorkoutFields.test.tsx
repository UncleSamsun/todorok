import { useState } from 'react'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import type { activity } from '@todorok/api-client'
import { WorkoutFields } from './WorkoutFields'

afterEach(cleanup)

it('keeps visible field names and units after entering a workout set', () => {
  function Form() {
    const [sets, change] = useState<activity.WorkoutSet[]>([{}])
    return <WorkoutFields sets={sets} change={change} />
  }
  render(<Form />)
  for (const [name, value, label] of [
    ['운동 1', '스쿼트', '운동'], ['횟수 1', '10', '횟수'],
    ['무게 1', '20', '무게 (kg)'], ['시간 초 1', '60', '시간 (초)'],
  ]) {
    const input = screen.getByLabelText(name)
    fireEvent.change(input, { target: { value } })
    expect(input.closest('label')).toHaveTextContent(label)
    expect(screen.getByText(label)).toBeVisible()
  }
  fireEvent.click(screen.getByRole('button', { name: '세트 추가' }))
  expect(screen.getByLabelText('무게 2').closest('label')).toHaveTextContent('무게 (kg)')
})
