# 알림 기능 재개 계획 — 2026-10-12

기존 MVP 작업13과 기능 인계의 VAPID 구독/발송 worker를 이어 구현한다.

1. notification OpenAPI 원본과 Java/TypeScript 생성 경로를 추가한다. 설정 이벤트는 기존 경계를 유지하고 구독 비밀값은 notification API에 직접 보낸다.
2. AES-GCM 암호화 구독 저장, 지원 provider 검증, 계정 소유권·10개 제한·멱등 접수·삭제를 구현하고 실제 HTTP/PostgreSQL로 검증한다.
3. 구독별 성공/실패, DB claim token·lease, 404/410 폐기, 429/5xx backoff, deadline 만료와 다음 일일 예약을 검증한다. 외부 전송은 DB 트랜잭션 밖에서 실행한다.
4. 설정의 기기 구독 버튼, 권한 거절·재시도, service worker 표시/tag/click을 연결한다.
5. 계약 drift, 영향 서비스 suite, 웹 suite/build/번들, Nginx 실제 라우팅, Compose config를 확인한다.

공개 푸시 provider에 보내는 실전 검증은 사용자 실제 브라우저 구독과 VAPID 운영 키가 있어야 한다. 서버 자동 검증은 통제된 sender를 사용하고 실제 provider 수신으로 과장하지 않는다. 외부 운동 원문 이용 권한과 실제 iPhone은 임의로 대체하지 않는다.
