export type TimerPhase = 'IDLE' | 'PREPARING' | 'WORK' | 'REST' | 'PAUSED' | 'FINISHED' | 'ABORTED'
type ActivePhase = 'PREPARING' | 'WORK' | 'REST'
export type TimerEffect = 'WORK' | 'REST' | 'FINISHED' | null

export type TimerState = {
  phase: TimerPhase
  round: number
  completedRounds: number
  startedAt: number | null
  deadlineAt: number | null
  pausedPhase: ActivePhase | null
  remainingMs: number | null
}

const prepareMs = 3_000, workMs = 10_000, restMs = 50_000, totalRounds = 10
const active = (phase: TimerPhase): phase is ActivePhase => phase === 'PREPARING' || phase === 'WORK' || phase === 'REST'
const work = (state: TimerState, now: number, round = state.round): TimerState => ({ ...state, phase: 'WORK', round, deadlineAt: now + workMs, pausedPhase: null, remainingMs: null })

export function idleTimer(): TimerState {
  return { phase: 'IDLE', round: 0, completedRounds: 0, startedAt: null, deadlineAt: null, pausedPhase: null, remainingMs: null }
}

export function startTimer(now: number): TimerState {
  return { phase: 'PREPARING', round: 1, completedRounds: 0, startedAt: now, deadlineAt: now + prepareMs, pausedPhase: null, remainingMs: null }
}

export function advanceTimer(state: TimerState, now: number): { state: TimerState; effect: TimerEffect } {
  if (!active(state.phase) || state.deadlineAt === null || now < state.deadlineAt) return { state, effect: null }
  let current = state, effect: TimerEffect = null
  while (active(current.phase) && current.deadlineAt !== null && now >= current.deadlineAt) {
    const deadline = current.deadlineAt
    if (current.phase === 'PREPARING') {
      current = work(current, deadline)
      effect = 'WORK'
    } else if (current.phase === 'WORK') {
      current = { ...current, phase: 'REST', completedRounds: current.completedRounds + 1, deadlineAt: deadline + restMs, pausedPhase: null, remainingMs: null }
      effect = 'REST'
    } else if (current.round === totalRounds) {
      current = { ...current, phase: 'FINISHED', deadlineAt: null, pausedPhase: null, remainingMs: null }
      effect = 'FINISHED'
    } else {
      current = work(current, deadline, current.round + 1)
      effect = 'WORK'
    }
  }
  return { state: current, effect }
}

export function pauseTimer(state: TimerState, now: number): TimerState {
  if (!active(state.phase) || state.deadlineAt === null) return state
  return { ...state, phase: 'PAUSED', deadlineAt: null, pausedPhase: state.phase, remainingMs: Math.max(0, state.deadlineAt - now) }
}

export function resumeTimer(state: TimerState, now: number): TimerState {
  if (state.phase !== 'PAUSED' || state.pausedPhase === null || state.remainingMs === null) return state
  return { ...state, phase: state.pausedPhase, deadlineAt: now + state.remainingMs, pausedPhase: null, remainingMs: null }
}

export function skipTimer(state: TimerState, now: number): TimerState {
  if (!active(state.phase)) return state
  if (state.phase === 'PREPARING') return work(state, now)
  if (state.round === totalRounds) return { ...state, phase: 'FINISHED', deadlineAt: null, pausedPhase: null, remainingMs: null }
  return work(state, now, state.round + 1)
}

export function abortTimer(state: TimerState): TimerState {
  if (state.phase === 'IDLE' || state.phase === 'FINISHED' || state.phase === 'ABORTED') return state
  return { ...state, phase: 'ABORTED', deadlineAt: null, pausedPhase: null, remainingMs: null }
}
