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
  return typeof value.type === 'string'
    && typeof value.title === 'string'
    && Number.isInteger(value.status) && Number(value.status) >= 400 && Number(value.status) <= 599
    && typeof value.code === 'string'
    && typeof value.traceId === 'string' && value.traceId.length > 0
    && typeof value.retryable === 'boolean'
    && (value.fieldErrors === undefined || (Array.isArray(value.fieldErrors) && value.fieldErrors.every(isFieldProblem)))
}

function isFieldProblem(value: unknown): value is FieldProblem {
  return isRecord(value)
    && typeof value.field === 'string'
    && typeof value.code === 'string'
    && typeof value.message === 'string'
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
