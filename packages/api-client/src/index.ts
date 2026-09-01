export interface ApiRequest {
  path: string
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
}
export interface ApiTransport {
  request<T>(request: ApiRequest): Promise<T>
}
