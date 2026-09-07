# 작업08 기록 변경 서버

범위는 Activity 수정·불변 이력·void 경합·지난 기록 반복 정책과 planner revision 동기화다. 월별 read-only 집계 API와 화면은 후속 단계이며 전체 작업08은 아직 완료되지 않았다.

## 클라이언트 연결

생성된 `activity.ActivityApi.correctActivity({activityId, correctActivityRequest})`는 PATCH `/api/activity/v1/activities/{activityId}`를 호출한다.

```ts
const saved = await api.correctActivity({
  activityId: current.activityId,
  correctActivityRequest: {
    expectedVersion: current.version,
    performedAt: new Date('2026-08-31T00:00:00+09:00'),
    note: '수정한 메모',
    detail: { study: { subject: '수학', durationMinutes: 30 } },
  },
})
```

수정 가능한 필드를 전체 교체한다. `performedAt`과 `detail`은 필수, `note`·`startedAt`·`endedAt` 생략은 해당 값을 비운다. 시간은 두 Date를 함께 보내며 수행 서울 날짜 안에서 end > start여야 한다. `{}` detail도 유효하고 유형별 선택 필드는 작업07과 동일하다. Task·activityType·completionStatus는 변경할 수 없다. VOIDED 수정은 409 ACTIVITY_VOIDED, 오래된 expectedVersion은 409 VERSION_CONFLICT, 다른 사용자의 기록은 404다. 재시도 충돌 시 GET으로 저장 revision을 확인하고 사용자가 다시 수정한다. 원본 생성 commandId/fingerprint는 유지하므로 원본 생성 요청 재전송은 현재 수정된 Activity를 반환한다.

응답의 `previousPerformedAt`은 마지막 correction 이전 수행일이다. `performedAt`과 함께 서울 날짜/월을 계산해 양쪽 캐시를 갱신한다. Task는 다른 예정일에서 이동할 수 있으므로 가장 안전한 기본 규칙은 저장 직후와 실제 sync 완료 뒤 해당 사용자의 Activity list 및 planner day/month query 전체를 invalidate하는 것이다. correction 이전 화면 snapshot을 갖고 있다면 그 날짜도 invalidate한다. ACK를 기다리는 동안 타이머로 성공 처리하지 않는다.

- COMPLETED 수정: revision 증가 및 PENDING → 실제 동일 revision ack 후 APPLIED 또는 CONFLICT.
- PARTIAL 수정: revision 증가 및 ActivityCorrected 발행, syncState NOT_REQUIRED 유지. planner는 inbox만 저장하고 완료·후속회차·ack를 생성하지 않는다.
- void: 기존 `voidActivity({activityId,voidActivityRequest:{reason,version}})` 재사용. revision과 detail 이력을 보존한다. 수정/취소 경합은 하나만 저장되며 다른 요청은 409다. 이미 VOIDED인 현재 version 재요청은 멱등, 취소 전 version 재요청은 409이므로 GET으로 확인한다.
- 활성 반복 다음 회차가 있는 완료 취소는 기존 CONFLICT/ACTIVE_OCCURRENCE_EXISTS 정책을 유지한다. 저장된 Activity와 후속 회차를 숨기거나 삭제하지 않는다. UI에 해결되지 않은 충돌을 표시해야 한다.

## 저장 구조와 반복 정책

신규 activity V4만 추가했다. 적용된 activity V1–V3/planner V1–V8과 migration·bootstrap Kafka 제외 설정을 수정하지 않았다.

`activity_revision_history(activity_id, revision, snapshot, replaced_at)`는 변경 직전 Activity header와 전체 typed detail을 JSONB snapshot으로 저장한다. PK로 revision 중복을 막고 DB trigger로 UPDATE/DELETE를 거부한다. 현재 값은 기존 관계형 detail 테이블에 보관한다. 이력 insert·현재 revision/header/detail 교체·outbox append는 동일 transaction이며 실패 시 모두 rollback된다. 이번 단계는 감사 이력 저장이며 이력을 조회하는 새 공개 endpoint는 없다.

ActivityCorrected v1은 `activityId,taskId,activityType,completionStatus,completedAt,outcome,startedAt,endedAt,previousPerformedAt` 전체 planner snapshot이다. `completedAt`은 기존 completion event와 동일하게 실제 수행일시를 뜻한다. aggregateVersion은 수정한 revision이다. completed correction이 original completion보다 먼저 도착해도 전체 snapshot으로 반영하고 낮은 revision/동일 event를 무시한다. planner Task/result/inbox/ack는 하나의 transaction이며 Activity ack update는 현재 revision과 PENDING 상태가 일치할 때만 반영한다.

지난 기록은 기존 createActivity의 과거 performedAt을 사용한다. 일회성은 다음 회차 없음, PARTIAL은 완료/다음 회차 없음. 완료·correction은 공통 SeriesService 정책을 사용하고 occurrenceDate는 최초 반복 규칙 날짜를 유지한다. 기존 PLANNED가 있으면 ID·날짜 그대로 유지, archived/종료 범위 초과이면 생성 없음, 과거 next는 오늘로 이월된 회차 하나, 미래 next는 규칙 날짜를 유지한다. V8 전체 이력 날짜 예약과 latestOccurrence cursor를 유지하므로 이미 완료·skip·삭제된 후속 날짜를 재생성하지 않는다. 수행일 수정은 반복 규칙 변경 명령이 아니다.

## 후속 월 집계 데이터

서비스 간 DB 직접 조회 금지. Activity 통계는 activity 서비스 로컬 DB에서, 등록수는 planner 로컬 DB에서 조회한다. 서울 월 시작을 inclusive/다음 월 시작 exclusive Instant 구간으로 바꾸고 user_id와 activity_type 필터를 함께 적용한다.

- 완료 수: activity_record.status = COMPLETED의 레코드 수. PARTIAL/VOIDED 제외.
- 시간: COMPLETED/PARTIAL만. started_at/ended_at이 있으면 구간 초를 우선 사용한다. 구간이 없으면 WORKOUT의 workout_set.duration_seconds 합, STUDY의 study_detail.duration_minutes × 60, CLIMBING의 climbing_detail.duration_seconds. null은 추정하지 않으며 header와 detail을 이중 합산하지 않는다. workout/climbing child join 때문에 Activity count가 증가하지 않도록 먼저 activity별 집계한다.
- 등록 수: planner.task의 scheduled_date가 선택 월이고 status <> DELETED인 실제 Task 수. series 정의에서 미래 회차를 계산해 더하지 않는다.

## 검증

2026-09-07 실행:

- `node scripts/generate-contracts.mjs`: 원본 OpenAPI에서 Java/TypeScript 재생성 성공. 생성물 직접 수정 없음.
- `:tests:messaging-integration:test --tests '*ActivityServiceRoundTripTest.correction*' :libs:event-contracts:test --no-daemon --no-configuration-cache`: 초기 correction 실제 왕복 3/3 성공, 2분59초.
- `.\gradlew.bat :tests:messaging-integration:test --tests '*ActivityServiceRoundTripTest.correction*' --tests '*ActivityServiceRoundTripTest.pastRecords*' --tests '*ActivityServiceRoundTripTest.finiteSeriesActivity*' :libs:event-contracts:test :services:activity-service:test --tests '*ActivityMigrationApplicationTest' --tests '*ActivitySecurityIntegrationTest' :services:planner-service:test --tests '*PlannerMigrationApplicationTest' --no-daemon --no-configuration-cache`: **실제 Kafka 왕복 6/6, EventEnvelope 4/4, EventSchemaContract 15/15, activity migration/security 3/3, planner migration 1/1 성공**, skipped 0, 3분39초.
- 실제 PostgreSQL·Kafka·Debezium·두 HTTP 서비스: 세 유형 correction/header/detail/history, 다른 owner 404, 유형·잘못된 시간 400, stale version 409, 원본 생성 command 재전송이 수정한 현재 revision 반환, 같은 version 수정 4경합 중 1승, 수정/void 경합 1승, VOIDED 후 수정 거부와 detail 보존.
- immutable history DELETE를 DB에서 거부. Activity outbox 실패 주입으로 header/detail/revision/history 전체 rollback. planner ack outbox 실패 주입으로 Task/result/inbox 전체 rollback 후 실제 listener 재개로 동일 correction 성공.
- 실제 Kafka correction 선도착, 동일 event 중복, 새 eventId의 낮은 correction/completion revision, void 뒤 오래된 correction 모두 최신 상태 보존. consumer stop 상태에서 두 revision 저장 후 restart하여 최신 revision APPLIED. PARTIAL correction event가 실제 planner inbox에 도착해도 Task PLANNED/Activity NOT_REQUIRED 유지.
- PRD10.4 매트릭스: 과거 next는 서울 오늘, 미래 next는 규칙 날짜, archived/종료조건 초과는 미생성, PARTIAL 미완료·미생성. correction 시 기존 PLANNED의 ID·예정일·occurrenceDate 불변. 일회성 correction 왕복과 V8 유한 반복 완료→void→재완료에서 기존 완료 후속 Activity 및 2개 회차 보존.
- `node --test scripts/openapi-contract.test.mjs`: 2/2 성공. `pnpm --filter @todorok/api-client build`: 성공. `node scripts/check-contract-drift.mjs`: 원본/생성물 일치.
- `.\gradlew.bat :tests:messaging-integration:test --tests '*ActivityServiceRoundTripTest.correctionPreservesTypedHistoryAndVersionRacesAndVoid' --no-daemon --no-configuration-cache`: 시간 구간 저장→다음 PATCH에서 생략하여 planner 구간도 비워지는 assertion 보강 후 해당 사례만 **1/1 성공**, skipped 0, 2분40초. 세 유형에서 실제 Kafka로 start/end 전달 및 제거 확인. `git diff --cached --check` 성공.

위 실패 주입의 rollback 검사는 Spring transaction proxy 직접 호출을 포함하며 성공/복구는 실제 Kafka listener 경로다. 새 correction 계약의 서비스 JVM 교체, broker/Connect 장애, DLT 운영 재전송은 이번에 반복하지 않았다. UI/월별 read-only API/프로그램 진급은 이 서버 단계 검증 범위 밖이다.

## P2 리뷰 수정: 조회 revision 일관성

기존 GET은 header SQL의 row mapping 중 별도 detail SQL을 실행해 READ_COMMITTED에서 concurrent PATCH가 끼면 revision N header와 N+1 detail을 함께 반환할 수 있었다. 목록의 여러 GET 호출과 생성 command 재전송의 내부 GET도 같은 경로였다.

공통 SELECT 한 문장에서 header와 유형별 관계형 detail의 JSON aggregate를 가져오고 row mapper는 추가 SQL 없이 변환한다. GET은 한 기록, list는 날짜·cursor·limit 필터까지 같은 statement snapshot을 사용한다. PostgreSQL statement snapshot으로 해결했으며 READ_COMMITTED·생성 command advisory lock·쓰기 잠금 순서는 변경하지 않았다. 따라서 advisory lock 대기 전에 REPEATABLE_READ snapshot을 고정하는 문제를 만들지 않는다. 생성 원본 command 재전송도 같은 snapshot mapper를 사용한다.

실제 PostgreSQL 테스트는 ResultSet을 얻은 뒤 row mapping 직전에 latch로 조회를 멈추고, 다른 transaction에서 PATCH를 commit한 다음 조회를 재개한다. 세 유형 × GET/list/원본 command replay 9조합에서 이전 revision의 header·메모·수행일·detail을 함께 반환하고 다음 GET은 새 revision을 반환하는지 검사한다. 동일 command 8개를 동시에 시작해 모두 같은 Activity ID를 받으며 저장 행은 하나인지 별도 확인한다. 기존 persistence migration 수 기대값은 신규 V4와 테스트 V9000을 포함한 5개로 수정했다.

첫 두 DB 실행은 새 서비스 호출 fixture의 search_path 설정 문제로 `task_reference`를 찾지 못했다(각 실행에서 기존 4개 통과, 새 2개 초기화 실패). 기존 fixture는 schema-qualified 조회만 했고, 첫 수정에서는 Testcontainers URL의 기존 `?loggerLevel=OFF`를 놓쳤다. URL query 유무에 따라 `&`/`?`를 구분해 실제 서비스와 동일한 `currentSchema=activity`를 명시했다.

다음 실행은 기존 4개와 동시 command 8개 검증이 통과했다. barrier fixture의 `List.of`가 기존 validation의 `contains(null)` 호출에서 예외를 내는 문제도 드러났다. barrier는 generated mutable-list builder로 분리해 조회 문제를 검사했고, 유효한 immutable list도 허용해야 하므로 기존 validator 자체를 `stream().anyMatch(Objects::isNull)`로 수정했다. 별도 단위 테스트에서 immutable sets/rounds 허용과 null set/round 거부를 검증한다.

- `.\gradlew.bat :services:activity-service:test --tests '*ActivityPersistenceIntegrationTest' --no-daemon --no-configuration-cache`: URL 보정 뒤 기존 persistence/migration 4개와 8개 동시 command 멱등성 1개 통과. 위 validation 문제로 barrier fixture만 실패했으며 제품 조회 assertion 실패는 없었다.
- `.\gradlew.bat :services:activity-service:test --tests '*ActivityPersistenceIntegrationTest.readsOneRevisionWhenCorrectionCommitsBetweenResultSetAndMapping' --no-daemon --no-configuration-cache`: 실제 PG barrier **1/1 성공(내부 9조합), skipped 0**, 58초. query 결과를 얻은 후 PATCH가 실제 commit되어도 header/detail은 동일 revision이며 다음 GET은 새 revision이다.
- `.\gradlew.bat :services:activity-service:test --tests '*ActivityDetailValidationTest' --no-daemon --no-configuration-cache`: immutable DTO list와 null item 단위 **3/3 성공**, skipped 0, 42초. `git diff --check` 성공.

공개 계약·Kafka 처리·쓰기 격리는 변경하지 않아 생성 drift나 전체 Kafka gate는 다시 실행하지 않았다.
