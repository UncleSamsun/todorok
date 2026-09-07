import { ActivityApi, ActivityResponse, CorrectActivityRequestFromJSON, ActivityResponseFromJSON } from './generated/activity/src'

export type ActivityTimestamps = { performedAt: string; startedAt?: string; endedAt?: string }
export type EditableActivity = ActivityResponse & { timestamps?: ActivityTimestamps }
export type ActivityCorrection = ActivityTimestamps & { expectedVersion: number; note?: string; detail: ActivityResponse['detail'] }

// Generated Date fields truncate precision beyond milliseconds. Keep the original
// wire timestamps alongside the typed model at the API boundary for round trips.
async function editable(raw: Response): Promise<EditableActivity> {
  const json = await raw.json()
  return { ...ActivityResponseFromJSON(json), timestamps: { performedAt: json.performedAt, startedAt: json.startedAt ?? undefined, endedAt: json.endedAt ?? undefined } }
}
export async function getEditableActivity(api: ActivityApi, activityId: string, signal?: AbortSignal) {
  return editable((await api.getActivityRaw({ activityId }, { signal })).raw)
}
export async function correctEditableActivity(api: ActivityApi, activityId: string, body: ActivityCorrection) {
  const precise = api.withPreMiddleware(async ({ url, init }) => ({ url, init: { ...init, body: JSON.stringify(body) } }))
  return editable((await precise.correctActivityRaw({ activityId, correctActivityRequest: CorrectActivityRequestFromJSON(body) })).raw)
}
export async function voidEditableActivity(api: ActivityApi, activityId: string, body: { reason: string; version: number }) {
  return editable((await api.voidActivityRaw({ activityId, voidActivityRequest: body })).raw)
}
export function activityTimestamps(value: EditableActivity): ActivityTimestamps {
  return value.timestamps ?? { performedAt: value.performedAt.toISOString(), startedAt: value.startedAt?.toISOString(), endedAt: value.endedAt?.toISOString() }
}
function instant(value?: string) {
  if (!value) return null
  const fraction = /\.(\d+)/.exec(value)?.[1] ?? ''
  return BigInt(Date.parse(value)) * 1_000_000n + BigInt(fraction.padEnd(9, '0').slice(3, 9))
}
export function positiveActivityInterval(start: string, end: string) { return instant(end)! > instant(start)! }
function canonical(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(canonical)
  if (value && typeof value === 'object') return Object.fromEntries(Object.entries(value).sort(([a], [b]) => a.localeCompare(b)).map(([key, item]) => [key, canonical(item)]))
  return value
}
export function matchesActivityCorrection(value: EditableActivity, baseline: EditableActivity, request: ActivityCorrection) {
  const times = activityTimestamps(value)
  return value.activityId === baseline.activityId && value.taskId === baseline.taskId && value.activityType === baseline.activityType && value.status === baseline.status && value.version === request.expectedVersion + 1 &&
    instant(times.performedAt) === instant(request.performedAt) && instant(times.startedAt) === instant(request.startedAt) && instant(times.endedAt) === instant(request.endedAt) &&
    (value.note ?? '') === (request.note ?? '') && JSON.stringify(canonical(value.detail)) === JSON.stringify(canonical(request.detail))
}
