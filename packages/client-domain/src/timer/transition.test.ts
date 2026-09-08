import assert from 'node:assert/strict'
import test from 'node:test'
import { advanceTimer, abortTimer, idleTimer, pauseTimer, resumeTimer, skipTimer, startTimer } from './transition.ts'

test('deadline 기준으로 준비·운동·휴식 열 라운드를 끝내고 지난 전환을 몰아 재생하지 않는다', () => {
  let state = startTimer(0)
  assert.equal(state.phase, 'PREPARING')
  state = advanceTimer(state, 3_000).state
  assert.equal(state.phase, 'WORK')
  assert.equal(state.round, 1)
  state = advanceTimer(state, 13_000).state
  assert.equal(state.phase, 'REST')
  assert.equal(state.completedRounds, 1)

  const final = advanceTimer(state, 603_000)
  assert.equal(final.state.phase, 'FINISHED')
  assert.equal(final.state.completedRounds, 10)
  assert.equal(final.effect, 'FINISHED')
})

test('일시정지 중에는 시간을 소비하지 않고 재개 시 남은 deadline으로 돌아간다', () => {
  let state = advanceTimer(startTimer(0), 3_000).state
  state = pauseTimer(state, 5_000)
  assert.equal(state.phase, 'PAUSED')
  state = resumeTimer(state, 100_000)
  assert.equal(state.phase, 'WORK')
  assert.equal(state.deadlineAt, 108_000)
  assert.equal(advanceTimer(state, 107_999).state.phase, 'WORK')
  assert.equal(advanceTimer(state, 108_000).state.phase, 'REST')
})

test('건너뛰기는 다음 라운드로 이동하고 중단은 완료한 라운드만 보존한다', () => {
  let state = skipTimer(advanceTimer(startTimer(0), 3_000).state, 4_000)
  assert.equal(state.phase, 'WORK')
  assert.equal(state.round, 2)
  assert.equal(state.completedRounds, 0)
  state = advanceTimer(state, 14_000).state
  assert.equal(state.completedRounds, 1)
  state = abortTimer(state)
  assert.equal(state.phase, 'ABORTED')
  assert.equal(state.completedRounds, 1)
})

test('시작 전과 종료 상태는 변경하지 않고 마지막 라운드 건너뛰기는 완료로 끝난다', () => {
  const idle = idleTimer()
  assert.equal(advanceTimer(idle, 10_000).state, idle)
  assert.equal(pauseTimer(idle, 0), idle)
  assert.equal(resumeTimer(idle, 0), idle)
  assert.equal(abortTimer(idle), idle)

  const finalWork = advanceTimer(startTimer(0), 543_000).state
  assert.equal(finalWork.phase, 'WORK')
  assert.equal(finalWork.round, 10)
  const skipped = skipTimer(finalWork, 543_001)
  assert.equal(skipped.phase, 'FINISHED')
  assert.equal(skipped.completedRounds, 9)
  assert.equal(skipTimer(skipped, 600_000), skipped)
  assert.equal(abortTimer(skipped), skipped)
})
