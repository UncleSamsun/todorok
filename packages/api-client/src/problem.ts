export interface FieldProblem {
  field: string
  code: string
  message: string
}

export interface ApiProblem {
  type: string
  title: string
  status: number
  detail?: string
  instance?: string
  code: string
  traceId: string
  retryable: boolean
  fieldErrors?: FieldProblem[]
}

export type ClassifiedApiError =
  | { kind: 'problem'; problem: ApiProblem }
  | { kind: 'network'; cause: TypeError }
  | { kind: 'invalid-response'; payload: unknown }

export function classifyApiError(value: unknown): ClassifiedApiError {
  if (value instanceof TypeError) return { kind: 'network', cause: value }
  if (isApiProblem(value)) return { kind: 'problem', problem: value }
  return { kind: 'invalid-response', payload: value }
}

export async function requestJson(
  input: string | URL,
  init: RequestInit = {},
  fetcher: typeof fetch = fetch,
): Promise<unknown> {
  const response = await fetcher(input, init)
  const contentType = response.headers.get('content-type') ?? ''
  const payload: unknown = contentType.includes('json') ? await response.json() : await response.text()
  if (!response.ok) throw payload
  return payload
}

function isApiProblem(value: unknown): value is ApiProblem {
  if (!isRecord(value)) return false
  if (!hasOnlyKeys(value, ['type', 'title', 'status', 'detail', 'instance', 'code', 'traceId', 'retryable', 'fieldErrors'])) return false
  return typeof value.type === 'string' && isUriReference(value.type)
    && typeof value.title === 'string'
    && Number.isInteger(value.status) && Number(value.status) >= 400 && Number(value.status) <= 599
    && (value.detail === undefined || typeof value.detail === 'string')
    && (value.instance === undefined || (typeof value.instance === 'string' && isUriReference(value.instance)))
    && typeof value.code === 'string' && /^[A-Z][A-Z0-9_]+$/.test(value.code)
    && typeof value.traceId === 'string' && value.traceId.length > 0
    && typeof value.retryable === 'boolean'
    && (value.fieldErrors === undefined || (Array.isArray(value.fieldErrors) && value.fieldErrors.every(isFieldProblem)))
}

function isFieldProblem(value: unknown): value is FieldProblem {
  return isRecord(value)
    && hasOnlyKeys(value, ['field', 'code', 'message'])
    && typeof value.field === 'string' && value.field.length > 0
    && typeof value.code === 'string' && /^[A-Z][A-Z0-9_]+$/.test(value.code)
    && typeof value.message === 'string' && value.message.length > 0
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function hasOnlyKeys(value: Record<string, unknown>, allowed: readonly string[]): boolean {
  const allowedKeys = new Set(allowed)
  return Object.keys(value).every((key) => allowedKeys.has(key))
}

function isUriReference(value: string): boolean {
  if (/[\u0000-\u0020\u007f]/.test(value) || /%(?![0-9a-fA-F]{2})/.test(value)) return false
  try {
    new URL(value, 'https://uri-reference.invalid/')
    return true
  } catch {
    return false
  }
}
