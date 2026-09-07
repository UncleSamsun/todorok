import { ActivityApi, ActivityResponse, CorrectActivityRequest, CorrectActivityRequestFromJSON, ActivityResponseFromJSON } from './generated/activity/src'

export type ActivityTimestamps = { performedAt: string; startedAt?: string; endedAt?: string }
export type EditableActivity = ActivityResponse & { timestamps?: ActivityTimestamps; legacyStudyPayloadRaw?: string }
export type ActivityCorrection = ActivityTimestamps & { expectedVersion: number; note?: string; detail: CorrectActivityRequest['detail'] }

// Generated Date fields truncate precision beyond milliseconds. Keep the original
// wire timestamps alongside the typed model at the API boundary for round trips.
function rawJsonProperty(source: string, property: string) {
  const match = new RegExp(`"${property}"\\s*:`).exec(source)
  if (!match) return undefined
  let start = match.index + match[0].length
  while (/\s/.test(source[start] ?? '')) start++
  if (source[start] !== '{' && source[start] !== '[') return undefined
  let depth = 0, quoted = false, escaped = false
  for (let index = start; index < source.length; index++) {
    const char = source[index]!
    if (quoted) {
      if (escaped) escaped = false
      else if (char === '\\') escaped = true
      else if (char === '"') quoted = false
      continue
    }
    if (char === '"') quoted = true
    else if (char === '{' || char === '[') depth++
    else if (char === '}' || char === ']') {
      depth--
      if (depth === 0) return source.slice(start, index + 1)
    }
  }
  return undefined
}
async function editable(raw: Response): Promise<EditableActivity> {
  const text = await raw.text(), json = JSON.parse(text)
  return { ...ActivityResponseFromJSON(json), timestamps: { performedAt: json.performedAt, startedAt: json.startedAt ?? undefined, endedAt: json.endedAt ?? undefined }, legacyStudyPayloadRaw: rawJsonProperty(text, 'legacyStudyPayload') }
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
