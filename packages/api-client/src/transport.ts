import { requestJson } from './problem.ts'

export interface ApiRequest {
  path: string
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
}

export interface ApiTransport {
  request<T>(request: ApiRequest): Promise<T>
}

export class FetchApiTransport implements ApiTransport {
  private readonly baseUrl: string
  private readonly fetcher: typeof fetch

  constructor(baseUrl: string, fetcher: typeof fetch = fetch) {
    this.baseUrl = baseUrl
    this.fetcher = fetcher
  }

  async request<T>(request: ApiRequest): Promise<T> {
    return await requestJson(`${this.baseUrl}${request.path}`, {
      method: request.method ?? 'GET',
      body: request.body === undefined ? undefined : JSON.stringify(request.body),
      headers: request.body === undefined ? undefined : { 'content-type': 'application/json' },
    }, this.fetcher) as T
  }
}

export { classifyApiError, requestJson } from './problem.ts'
export type { ApiProblem, ClassifiedApiError, FieldProblem } from './problem.ts'
