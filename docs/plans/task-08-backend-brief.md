# 작업08 기록 변경 서버 단계

`docs/plans/task-08-brief.md`의 전체 요구에서 correction·void·지난 기록·revision/event 반영 서버를 먼저 완료한다. 월별 read-only 집계와 UI는 별도 하위 단계에서 이어 구현하며 전체08 완료조건에 남는다.

- 기존 생성/void/ack와 TaskReference를 재사용한다. Activity PATCH는 expectedVersion, typed detail·note·optional 시간·수행일을 검증하고 수정 이력을 보존한다.
- ActivityCorrected 원본 schema/Java record/fixture와 planner consumer를 연결한다. 같은 transaction에서 revision/history/detail/outbox를 저장하고 ack가 확인하는 revision을 일치시킨다.
- 수정 전후 날짜를 보존하여 UI가 양쪽 날짜/월을 갱신할 수 있는 응답 또는 명확한 캐시 규칙을 제공한다. count/time read 모델은 다음 단계가 실제 저장구조에서 조회할 수 있도록 문서화한다.
- void/recompletion의 V8 발생일 중복 방지와 이미 수행된 후속회차 보존을 유지한다. PLANNED 충돌은 숨기거나 이미 수행된 이력을 삭제해서 해결하지 않는다.
- 지난기록은 생성 API의 실제수행일과 기존 series 정책을 연결하여 PRD10.4 네분기/일회성/PARTIAL을 실제검증한다. UI용 새 명령이 필요하면 원본계약에 먼저 명시한다.
- 다른owner·version충돌·중복/역순correction·void후oldcompletion·consumer중단복구·실제event원자성을 검증한다. 필요한경로는 실제Kafka/PG/CDC로 테스트한다.
- rollback/권한/migration의 기존 보장을 낮추지 않는다. 이미 적용한 migration을 수정하지 않는다.

현재workspace `C:\workspace\todorok-worktrees\functional`, 서버기준 `88fa718`, UI포함 HEAD `77bf83b`. 실제파일/계약을먼저확인한다. 적용 후 `docs/plans/task-08-backend-report.md`에 exact client method/payload/state·검증명령/결과·월집계에 사용할 데이터와 시간우선순위를 기록한다. 시간은 header 구간 우선, 없으면 WORKOUT세트초합/STUDY분×60/CLIMBING초이며 중복합산하지 않는 후속집계안을 따른다.

부모progress/brief와 기존UI는 수정하지 않는다. 하위작업자생성·원격push/merge금지. apply_patch/PowerShell사용. 변경파일만 한글commit하고 서버하위단계와 전체08완료를 구분한다.
