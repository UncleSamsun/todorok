const baseUrl = process.env.TODOROK_BASE_URL ?? 'http://localhost'
const endpoints = [
  '/',
  '/health',
  '/api/planner/actuator/health',
  '/api/activity/actuator/health',
]

for (const endpoint of endpoints) {
  const response = await fetch(new URL(endpoint, baseUrl))
  if (!response.ok) {
    throw new Error(`${endpoint} 응답 실패: HTTP ${response.status}`)
  }
  console.log(`${endpoint} 확인 완료: HTTP ${response.status}`)
}
