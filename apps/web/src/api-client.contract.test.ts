import { planner } from '@todorok/api-client'
import { describe, expect, it } from 'vitest'

describe('생성 API client 계약', () => {
  it('날짜 전용 값을 timezone 변환 없이 보낸다', () => {
    const json = planner.CreateTaskRequestToJSON({
      title: '날짜 검증',
      taskType: planner.TaskType.General,
      scheduledDate: '2026-09-07',
    })

    expect(json.scheduledDate).toBe('2026-09-07')
  })

  it('보호된 달력 요청에 bearer token을 보낸다', async () => {
    let capturedHeaders: HeadersInit | undefined
    const configuration = new planner.Configuration({
      accessToken: 'test-token',
      fetchApi: async (_url, init) => {
        capturedHeaders = init?.headers
        return new Response(JSON.stringify({
          from: '2026-09-07',
          to: '2026-09-07',
          days: [],
        }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      },
    })

    await new planner.CalendarApi(configuration).getCalendarSummary({
      from: '2026-09-07',
      to: '2026-09-07',
    })

    expect(new Headers(capturedHeaders).get('Authorization'))
      .toBe('Bearer test-token')
  })
})
