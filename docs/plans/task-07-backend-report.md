# 작업07 서버 계약과 검증

서버 구현 범위: Activity header/typed detail 저장, TaskReference projection, planner 완료·취소 처리, outbox/inbox 및 명시적 동기화 결과. 화면은 후속 작업으로 작업07 전체 완료 조건에 남는다. 서버 검증 결과는 아래 실행 결과에 기록한다.

## UI 연결 계약

`@todorok/api-client`의 `activity` namespace에서 `ActivityApi`, `Configuration`과 생성 타입을 사용한다. 기존 session의 인증된 `fetchApi`를 Configuration에 연결한다. basePath는 `/api/activity/v1`이다.

- `createActivity({createActivityRequest})` → POST `/activities`, 201 ActivityResponse.
- `getActivity({activityId})` → GET `/activities/{activityId}`, 저장 상세와 현재 syncState 조회.
- `listActivities({date, cursor, limit})` → GET `/activities`, date는 서울 수행 날짜, cursor는 응답 nextCursor를 그대로 사용한다.
- `voidActivity({activityId, voidActivityRequest:{reason,version}})` → 기록을 보존하면서 VOIDED/revision 증가. COMPLETED 취소만 planner 동기화를 기다린다. 활성 다음 회차가 있으면 planner는 CONFLICT/ACTIVE_OCCURRENCE_EXISTS를 반환하며 기존 회차를 삭제하지 않는다.

생성 예시:

```ts
const request: activity.CreateActivityRequest = {
  commandId: crypto.randomUUID(), // 폼을 연 동안 입력 및 ID를 유지한다.
  taskId,
  activityType: 'WORKOUT',
  completionStatus: 'COMPLETED', // 'PARTIAL'도 지원
  performedAt: new Date('2026-09-08T00:00:00+09:00'),
  note: '완료 기록 메모',
  detail: { workout: { sets: [{ exercise: '스쿼트', reps: 5, weightKg: 60 }] } },
}
const saved = await api.createActivity({ createActivityRequest: request })
const current = await api.getActivity({ activityId: saved.activityId })
```

유형별 detail:

- WORKOUT: `{workout?: {sets?: [{exercise?, reps?, weightKg?, durationSeconds?}]}}`
- STUDY: `{study?: {subject?, durationMinutes?, values?: Record<string,unknown>, snapshot?: Record<string,unknown>}}`
- CLIMBING: `{climbing?: {durationSeconds?, rounds?: [{grade?, attempts?, completed?}]}}`

모든 detail 필드는 선택 사항이며 `{}`도 저장된다. 다른 유형 detail, 알 수 없는 필드, 숫자 문자열/소수 정수/잘못된 타입은 거부한다. 운동 세트와 클라이밍 라운드는 관계형 행으로 저장된다. 공부 values와 snapshot만 JSONB다. 현재 공부 snapshot은 사용자가 기록한 자유 데이터이며 서버가 검증한 템플릿 정의나 프로그램 진급 근거가 아니다. 서버 소유 템플릿 snapshot/version 검증은 작업09 범위다.

`performedAt`은 수행 날짜를 정하는 timestamp이며 등록 시각은 서버의 created_at에 별도로 보존한다. UI 날짜 선택은 서울 자정으로 변환해서 보낸다. 날짜만 입력한 기록에서 performedAt의 시간을 timeblock으로 표시하지 않는다. 실제 시간 입력을 사용자가 선택한 경우만 `startedAt`·`endedAt` 두 Date를 함께 보낸다. 둘 다 수행 서울 날짜에 속하고 endedAt > startedAt이어야 한다. planner TaskResponse의 같은 두 필드가 있을 때만 기존 행에 시간 블록을 표시한다.

## 동기화 및 오류 처리

ActivityResponse의 `syncState`는 서버에서 항상 제공한다(기존 response와 호환되도록 생성 타입에서는 optional).

| 상태 | UI 동작 |
|---|---|
| PENDING | 기록은 저장됨. '일정 반영중' 표시 후 getActivity 재조회. 장기 지연 시 '기록됨 · 일정 반영 재시도' 버튼으로 같은 GET을 실행한다. |
| APPLIED | 실제 planner transaction의 ack가 도착함. 수행 서울 날짜의 planner day/task를 다시 불러와 기존 Task행의 결과를 표시한다. |
| CONFLICT | 기록은 보존됨. syncReason을 보여주고 자동 성공 처리하지 않는다. GET 재조회는 충돌을 해결하지 않는다. |
| NOT_REQUIRED | PARTIAL 또는 PARTIAL 취소. Task 완료/다음 회차 생성을 기다리지 않는다. |

`GET`은 상태 재확인일 뿐 재발행·재조정 명령이 아니다. 별도 수동 reconciliation endpoint는 없다. 영구 충돌은 사용자에게 기존 기록/Task 상태를 보여주며 새 Activity를 자동 저장하지 않는다. 일시적인 consumer 실패는 기존 retry/DLT 경로로 처리한다. DLT 재처리는 운영 워크플로우이며 UI의 GET 버튼이 DLT를 재전송하지 않는다.

동일 `(userId, commandId)`와 동일 요청은 reference가 COMPLETED/DELETED로 바뀐 후에도 기존 Activity를 반환한다. 다른 payload로 같은 commandId를 사용하면 409 COMMAND_CONFLICT. 네트워크 응답 유실 시 원래 요청/commandId를 보존해서 재시도한다. 새로운 commandId로 복제하지 않는다. TASK_REFERENCE_PENDING은 retryable 409이며 나머지 상태/유형 충돌은 재저장 전에 확인이 필요하다. 다른 사용자의 Task/Activity는 404.

planner `TaskResponse` 추가 필드: `activityId`, `performedAt`, `startedAt`, `endedAt`, `completionSummary`. 새 Task를 복제하지 않고 기존 Task가 COMPLETED가 되며 scheduledDate는 수행 서울 날짜로 이동한다. 원래 series occurrenceDate는 유지하고 작업05의 다음 회차 정책을 호출한다.

## 메시지와 실행 설정

- 기존 ACTIVITY_COMPLETED v1은 optional startedAt/endedAt을 추가했다. 기존 필수 필드는 유지한다.
- 신규 ACTIVITY_SYNC_RESULT v1: `{activityId, taskId, state:'APPLIED'|'CONFLICT', reason}`. envelope aggregateVersion은 결과가 확인하는 Activity revision이다. planner task outbox/topic으로 전송된다.
- TaskScheduled/TaskChanged/TaskRolledOver는 activity-reference-v1 그룹이 `todorok.task.v1`에서 수신한다. 삭제 tombstone은 이후 이벤트로 복구하지 않는다.
- planner-activity-v1 그룹은 `todorok.activity.v1`에서 완료/취소를 수신한다. inbox, 완료 결과, Task/series 변경, ack outbox를 같은 transaction에 저장한다.
- Compose의 planner/activity는 `MESSAGING_ENABLED=true`를 설정했다. 기본 false는 Kafka 없는 테스트/로컬 비동기 비활성 기동용이다. 실제 UI 연결용 서버를 직접 기동하면 두 서비스에 MESSAGING_ENABLED=true, KAFKA_BOOTSTRAP_SERVERS와 각 schema datasource를 함께 설정해야 한다. DB migration을 먼저 적용하고 기존 Debezium connector가 RUNNING이어야 한다.
- migration: activity V3, planner V8까지 적용한다(V7 완료 연결, V8 이력 회차 예약). 개인/운영계정이나 외부 schema join을 사용하지 않는다.

새 UI fixture는 기존 `docs/authentication.md`의 키 생성/최초 계정 절차와 `infra/docker/compose.yml`을 사용하되 기존 운영 Compose project를 재사용하지 않는다. 테스트 전용 env 파일·RSA 키·계정·volume과 `-p todorok-task07` project를 사용하고 HTTP Origin/포트를 일치시킨다. 기존 작업06 image를 재사용하면 V3/V7과 listener가 없으므로 **현재 source의 planner/activity image를 새로 build**한다. 기동 순서는 PG/Kafka → migration → 두 HTTP 서비스 → Debezium Connect RUNNING → bootstrap 계정 → 프런트다. 새 Compose에는 두 서비스의 MESSAGING_ENABLED=true가 포함돼 있다. 브라우저에 연결하기 전에 Task 생성 후 Activity 저장이 TASK_REFERENCE_PENDING을 벗어나는지, GET syncState가 실제 APPLIED가 되는지 확인한다. 테스트가 끝나면 그 전용 project만 정리한다.

## 실행 결과

2026-09-07 실행:

- `:tests:messaging-integration:test --tests '*ActivityServiceRoundTripTest'`: **8/8 통과**, skipped 0, 실제 시나리오 116초. PostgreSQL 17.11, Kafka 4.3.1, Debezium Connect 3.6.2 실제 컨테이너와 두 Spring HTTP 서비스 사용.
- 세 유형 HTTP 기록→각 로컬 DB outbox→Debezium→Kafka→실제 listener→planner 기존 Task 완료→ack→Activity GET APPLIED. 실제 서울 날짜, 시간 구간, 완료 메모 보존. 완료/삭제 projection 뒤 동일 command 재시도와 다른 payload 409.
- 같은 command 8개 동시요청 모두 동일 ID, 다른 command 6개 경합은 생성 1개. PARTIAL은 outbox 없음/Task PLANNED/series 다음 회차 없음. COMPLETED는 series 다음 회차 하나.
- 소비자 stop→Activity PENDING 보존→Task 삭제 또는 skip→소비자 restart→조회 가능한 CONFLICT. 기록 삭제/삭제 Task 복구 없음.
- 실제 Kafka duplicate 및 새 eventId의 낮은 revision 재전달, VOIDED 이후 이전 completion 거부. 삭제 TaskReference의 낮은/높은 version 복원시도 모두 tombstone 유지.
- header/detail/outbox 실패 주입 시 기록 전체 rollback. planner inbox/Task/result/ack 실패 원자성 및 재개 후 실제 Kafka 처리. 원자성 실패 주입 2곳은 Spring transaction proxy를 직접 호출해 rollback을 검사했고, 서비스간 왕복과 재개 성공 판정은 실제 Kafka listener로 했다.
- `node --test scripts/openapi-contract.test.mjs`: 2/2 통과. EventEnvelope 4개와 EventSchemaContract 13개 통과. planner TaskHttp 17개/Persistence 4개, activity Persistence 4개/Security 2개 통과.
- migration 수정 후 activity/planner MigrationApplication 테스트 각 1개 통과. typed detail GET의 reps/weight/Study values·snapshot/Climbing duration·rounds 값을 확인하는 assertion 보강 후 해당 실제 Kafka 왕복 1개만 추가 실행하여 통과.
- BootstrapIsolationTest 1개 통과: 연결 불가능한 테스트 DB URL을 사용한 context 기동에서도 UserAccount bean만 구성되고 KafkaAdmin/KafkaTemplate/JPA가 생기지 않음을 확인. `pnpm --filter @todorok/api-client build` TypeScript 생성 client 빌드 통과. `git diff --check` 통과.

초기 실행 실패: 테스트 의존성/Boot 클래스 import 컴파일 2건과 fixture Origin allowlist 누락은 행동검증 전 초기화 문제였다. 이후 실제 기동에서 기존 plain spring-kafka 의존성으로 Boot4 Kafka 자동설정이 없는 것을 확인하여 두 서비스를 starter-kafka로 교체했다. 이에 따라 migration에서 KafkaAdmin이 생긴 영향 회귀를 발견해 두 migration entrypoint에서 KafkaAutoConfiguration을 명시 제외했고 재검증했다. 같은 auto-configuration을 사용하는 BootstrapCommand도 제외를 추가했으며 단독 격리 테스트를 통과했다.

미검증: UI/브라우저 연결은 후속 단계다. 이번 왕복 테스트의 restart는 실제 listener stop/start이며 서비스 JVM 교체·DLT 운영 재처리·broker/Connect 장애 복구를 새로 반복하지 않았다(기존 messaging fixture 범위). 완료 취소 시 활성 다음회차 충돌은 구현되어 있지만 전용 UI 조정 기능은 없다. correction/template/program 진행은 이번 서버 단계의 생성·완료 동기화 범위에 추가하지 않았다.

## P2 리뷰 수정: 과거 회차 날짜 예약

유한 반복의 첫 회차를 재완료하면 원래 occurrenceDate부터 다음 회차를 찾으므로 이미 COMPLETED/SKIPPED/DELETED인 후속 회차를 다시 만드는 문제가 있었다. 기존 `hasActive` 검사로는 terminal 이력을 보호하지 못했다.

- 수정 전 실제 PostgreSQL/HTTP 회귀 2개 실패: 유한 2회 반복의 Task 수가 기대 2/실제 3, 삭제된 후속 회차 뒤 재개 날짜가 기대 2026-09-11/실제 2026-09-09.
- 기존 series lock 안에서 생성 cursor를 `max(original occurrenceDate, latest persisted occurrenceDate)`로 정한다. latest 조회는 상태를 필터링하지 않으므로 삭제된 회차도 예약한다. Activity 완료와 GENERAL 재완료가 사용하는 공통 `SeriesService.generate`를 수정했으며 활성 회차·archive·endDate 정책은 유지한다.
- 신규 planner **V8**은 `(series_id, occurrence_date)` unique index를 series_id가 있는 전체 이력에 적용한다. 기존 중복을 발견하면 명확한 예외로 migration을 중단한다. 기존 기록을 자동 삭제·수정하지 않으므로 해당 환경은 명시적으로 이력을 조정한 뒤 migration을 다시 실행해야 한다.
- 집중 검증: GENERAL 유한 반복의 완료/skip/삭제 후속 회차 예약, 무기한 반복에서 삭제된 최신 회차 다음 날짜로 재개, 삭제 이력 중복 DB insert 거부, 실제 PostgreSQL→Debezium→Kafka로 유한 2회 모두 완료→첫 Activity VOIDED→첫 회차 새 기록 완료 후 Task 수 2 유지 및 기존 후속 Activity 연결 보존.

GREEN: `:services:planner-service:test`에서 새 HTTP/DB 회귀 2개만 선택하고 `:tests:messaging-integration:test`에서 새 finite Activity 왕복 1개만 선택하여 **3/3 통과, skipped 0**. 2026-09-07 실행 2분31초. V8을 실제 PostgreSQL에 적용한 상태에서 검사했으며 `git diff --check`도 통과했다. 앞 절의 전체 서버 검증은 반복하지 않았다.
