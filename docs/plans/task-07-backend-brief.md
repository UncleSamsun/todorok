# 작업07 서버 구현 범위

`docs/plans/task-07-brief.md`가 전체 요구사항이다. 이번 하위 단계는 Activity 저장·TaskReference·완료 동기화 서버와 계약·실제DB/Kafka 테스트를 완료한다. 사용자 폼과 React 변경은 다음 하위 단계에서 같은 계약에 연결하며 작업07 전체 완료 조건에서 제외하지 않는다.

- 원본 OpenAPI/event와 typed detail·idempotency·revision·동기화 조회 응답을 먼저 확정한다.
- TaskReference의 owner/type/status/version, 완료기록 제약, header/detail/outbox 원자성, inbox와 결과 원자성을 검증한다.
- ActivityCompletion과 planner 반영의 성공/충돌을 외부에서 확인할 수 있게 한다. deleted/skip 경합을 단순 성공으로 덮거나 기존 기록을 삭제하지 않는다.
- 실제 PostgreSQL·Kafka·Debezium 또는 기존 동등한 integration fixture로 DB commit부터 planner 반영까지 검증한다. mock listener 호출만으로 서비스 간 왕복을 주장하지 않는다.
- 일반 완료 기록 메모도 Activity header에 저장한다. PARTIAL은 Task완료/회차생성을 일으키지 않는다.
- UI 후속 담당자가 사용할 endpoint, generated client 호출, payload 예시, sync 조회/재시도 규칙을 `docs/plans/task-07-backend-report.md`에 명확히 남긴다.
- schema 버전·기존 contract 호환성을 확인하고 생성물은 생성기로만 변경한다. planner는 V6 이후, activity는 실제 최신 migration 이후 번호를 사용한다.

진행 중 판정은 서버 단계 기준으로 보고하되, 작업07 전체 완료는 화면·브라우저 연결 이후다. 원격push/merge·하위작업자생성 금지. apply_patch/PowerShell 사용. 현재workspace source 밖의 개인데이터·운영계정 사용 금지. 의미있는 회귀 테스트·보고·scoped 한글commit을 남긴다.
