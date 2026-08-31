const wait = (delayMs) => new Promise((resolve) => setTimeout(resolve, delayMs))

export async function fetchWithRetry(
  url,
  { attempts = 3, delayMs = 500, fetcher = fetch } = {},
) {
  let lastError

  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      return await fetcher(url)
    } catch (error) {
      lastError = error
      if (attempt < attempts) {
        await wait(delayMs)
      }
    }
  }

  throw lastError
}
