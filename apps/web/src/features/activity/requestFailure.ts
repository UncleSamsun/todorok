export async function requestFailure(reason: unknown) {
  const response = reason && typeof reason === 'object' && 'response' in reason ? (reason as { response: Response }).response : null
  let problem: { code?: string; retryable?: boolean } | null = null
  try { problem = response ? await response.clone().json() : null } catch { /* Proxies can return non-JSON rejections. */ }
  return { status: response?.status, code: problem?.code, retryable: problem?.retryable,
    rejected: Boolean(response && [400, 403, 404, 409, 413, 415, 422].includes(response.status)) }
}
