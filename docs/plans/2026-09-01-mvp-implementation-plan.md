# TODOROK MVP 구현 계획

**목표:** 승인된 PRD 1.3.0을 기능 이슈 단위로 구현해 달력 중심 할 일과 운동·공부·클라이밍 기록을 한 흐름으로 제공한다.

**기준 문서:** `docs/PRD.md`

**기술:** Java 25, Spring Boot 4.1.1, Spring Data JPA, PostgreSQL 17.11, Kafka 4.3.1 KRaft, Kafka Connect·Debezium, React 19.2, TypeScript, Vite 8, TanStack Query, Docker Compose, Nginx.

## What already exists

- Java 25·Spring Boot 4.1.1 Gradle 모노레포와 세 서비스 기동 기반
- React 19.2 PWA 앱 셸과 웹·모바일 공유 패키지 경계
- 공통 이벤트 envelope와 기본 event type
- PostgreSQL 서비스별 schema·계정과 Kafka KRaft 단일 broker
- Nginx 정적 파일·planner·activity routing
- 4GB 목표의 컨테이너 메모리 상한과 health check
- Java·TypeScript·Compose·저장소 기록 정책 CI

기존 기반은 그대로 재사용한다. 인증, persistence, 계약 생성, Debezium과 실제 domain 기능만 단계적으로 추가한다.

## NOT in scope

- 오프라인 캐시·변경 queue·Background Sync·기기 간 충돌 병합: 온라인 웹앱으로 범위를 고정했다.
- 사용자별 시간대·자정 scheduler: `Asia/Seoul`과 오늘 화면 진입 시 이월만 지원한다.
- 공개 회원가입·소셜 로그인·계정 복구: 최초 사용자는 bootstrap으로 생성한다.
- 네이티브 iOS 앱: MVP 이후 별도 제품 결정으로 둔다.
- Kubernetes·Redis·ALB·NAT Gateway·Schema Registry·멀티 broker Kafka: 4GB 단일 VM 범위에 맞지 않는다.
- 서비스별 물리 DB와 고가용성: 단일 PostgreSQL과 서비스별 schema만 사용한다.
- 권리 확인 전 운동 프로그램 원문 전체 데이터 공개: private catalog로만 주입한다.
- 소셜·결제·Health 연동·실시간 공동 편집: 제품 MVP 비목표다.

## 시스템 흐름

```text
iPhone / Notebook
       │
       ▼
     Nginx
       ├── /                         React PWA
       ├── /api/planner/v1/**        planner-service
       └── /api/activity/v1/**       activity-service

planner-service                       activity-service
  planner schema                        activity schema
       │ outbox                              │ outbox
       └──────── PostgreSQL WAL ─────────────┘
                          │
                          ▼
               Kafka Connect + Debezium
                          │
                          ▼
                     Kafka KRaft
                    ├── task events ─────► activity task_reference
                    ├── activity events ─► planner task state
                    └── commands ────────► notification-service
```

## 인증 흐름

```text
로그인
  ├── access JWT 10분 ─────────► React memory
  └── refresh token 30일 ──────► HttpOnly·Secure·SameSite=Lax cookie
                                   │
refresh 요청                       ▼
  ├── 기존 token hash 폐기
  ├── 새 token·hash 발급
  └── 폐기 token 재사용 시 session chain 전체 폐기

planner: 비대칭 개인키 서명
activity·notification: 공개키로 issuer·audience·exp·nbf 검증
```

## Task 상태와 반복

```text
task_series
  └── PLANNED task occurrence 하나만 허용
          ├── complete ─────────► COMPLETED
          ├── skip ─────────────► SKIPPED
          ├── rollover ─────────► 같은 행의 scheduled_date를 오늘로 이동
          └── soft delete

COMPLETED ── reopen / ActivityVoided ──► PLANNED
SKIPPED   ── reopen ───────────────────► PLANNED(today)

반복 완료 후 다음 PLANNED occurrence 생성
지난 기록 완료 후 오늘 PLANNED occurrence 생성
```

- 일반 반복은 `DAILY`, `WEEKLY`, `MONTHLY`와 간격·요일 또는 일자·시작·종료만 지원한다.
- 시리즈별 활성 회차는 partial unique index로 하나만 허용한다.
- 날짜는 `Asia/Seoul`의 `LocalDate`, 생성·이벤트 시각은 UTC `Instant`로 저장한다.

## Activity 흐름

```text
TaskScheduled / TaskChanged
          │
          ▼
activity.task_reference
          │ 소유자·유형·상태 확인
          ▼
activity_record + domain detail + outbox
          │
          ├── COMPLETED ──► ActivityCompleted ──► planner COMPLETE
          ├── PARTIAL ────► Task 유지 PLANNED
          └── VOIDED ─────► ActivityVoided ─────► planner reopen·진행 재계산
```

- 공통 Activity header와 workout·study·climbing detail table을 사용한다.
- 공부 field definition은 관계형 불변 version, record 값·snapshot은 JSONB다.
- command ID와 event ID unique 제약으로 중복 저장과 중복 결과를 차단한다.
- Task reference가 아직 없으면 `409 TASK_REFERENCE_PENDING`, `retryable=true`를 반환한다.

## 계약과 오류

- `contracts/openapi/planner-v1.yaml`, `activity-v1.yaml`이 REST 기준이다.
- OpenAPI Generator로 Spring Boot 4 interface·DTO와 TypeScript Fetch client를 생성한다.
- `contracts/events/<event>/v1.schema.json`이 이벤트 payload 기준이다.
- 파괴적 변경은 기존 version을 수정하지 않고 새 API·event version을 추가한다.
- Schema Registry는 사용하지 않고 JSON Schema·fixture 계약 테스트를 CI에서 실행한다.
- 오류는 RFC 9457 Problem Details와 `code`, `traceId`, `retryable`, `fieldErrors`를 사용한다.

## Persistence와 migration

- Spring Data JPA·Hibernate를 기본으로 사용한다.
- OSIV, 양방향 entity 관계와 entity 직접 API 응답을 금지한다.
- mutable aggregate에 optimistic version을 둔다.
- 달력 DTO projection, JSONB, partial index와 locking은 명시적 JPQL·native query로 구현한다.
- Flyway만 schema를 변경하고 Hibernate는 validate만 수행한다.
- 운영 migration은 서비스 기동 전 일회성 job으로 실행한다.
- 변경은 add → compatible code → backfill → remove 순서의 expand/contract로 나눈다.

## 운동 프로그램과 타이머

- 프로그램은 불변 catalog version·checksum과 idempotent importer로 관리한다.
- enrollment가 시작 version을 고정 참조하고 변경은 사용자의 명시적 migration만 허용한다.
- 이용 범위 확인 전 실제 표는 private catalog, 공개 테스트는 합성 fixture를 사용한다.
- 타이머는 `client-domain`의 순수 timestamp state machine이다.
- React는 표시·음향·진동·Wake Lock·visibility adapter만 담당한다.
- 여러 phase가 지나간 복귀에서는 현재 phase로 이동하고 지나간 cue를 재생하지 않는다.

## React 구조와 성능

- React Router, TanStack Query와 생성된 Fetch client를 사용한다.
- 서버 상태만 Query cache에 두고 modal·선택·타이머 표시는 component state·Context에 둔다.
- 오늘 화면은 eager, 운동·공부·클라이밍·설정은 lazy route다.
- 달력은 planner의 최대 42일 summary와 선택 날짜 detail을 병렬 조회한다.
- Activity 상세는 열 때 조회하고 긴 이력은 keyset pagination을 사용한다.
- 초기 JS gzip 150KB, lazy chunk 100KB를 CI 예산으로 둔다.
- 스티키노트는 500ms debounce PATCH로 저장하며 실패를 표시하고 수동 재시도한다.

## 알림 정책

- 기본 비활성화, 홈 화면 설치 후 설정 동작에서만 권한을 요청한다.
- 활성화 시 오전 8시 오늘 요약, 사용자 지정 시간 지원.
- 개별 Task 알림은 시간과 reminder를 명시한 경우에만 예약한다.
- 기본 방해 금지는 22시~07시다.
- 요약은 오전 10시, 개별 알림은 예정 시각 30분 이후 실패하면 만료 처리한다.
- notification 장애는 Task·Activity transaction에 영향을 주지 않는다.

## 테스트 gate

| 단계 | 필수 범위 | 목표 |
|---|---|---:|
| 개발 중 | 변경 함수·상태 분기 단위 테스트 | 60초 |
| 커밋 전 | 해당 모듈 전체 단위·타입·계약 drift | 2분 |
| 기능 PR | 전체 단위, 영향 서비스 Testcontainers, 계약, build, 관련 E2E | 10분 |
| develop | 전체 인프라 왕복, Chromium·WebKit 핵심 E2E | 15분 |
| release | 전체 회귀, migration, image, Safari·실기기, memory smoke | 30분 |
| 배포 직후 | health, 로그인, 오늘 조회, 합성 Task smoke, lag·오류율 | 5분 |
| 예약 | 4GB soak, fault injection, 백업 복구, 실기기 | 별도 |

- 핵심 domain branch 100%, 전체 line 85%·branch 80%를 PR·release에서 적용한다.
- repository는 PostgreSQL, messaging은 Kafka, outbox 왕복은 PostgreSQL·Kafka·Debezium Testcontainers를 사용한다.
- H2와 embedded Kafka는 사용하지 않는다.
- 장애 검사는 고정 중단 지점과 Toxiproxy를 사용하고 일반 PR에서는 영향 시나리오만 실행한다.

## Failure modes

| 경로 | 현실적인 실패 | 테스트 | 처리 | 사용자 경험 |
|---|---|---|---|---|
| 로그인 | refresh 동시 사용·재사용 | 통합·E2E | 회전·session chain 폐기 | 재로그인 안내 |
| 이월 | 같은 조회가 여러 번 실행 | 단위·DB 통합 | unique·멱등 command | 중복 없이 오늘 표시 |
| 반복 생성 | 동시 완료로 활성 Task 두 개 | 동시성 통합 | partial unique·409 | 다시 불러오기 |
| Activity 저장 | task reference 지연 | 계약·E2E | retryable 409 | 동기화 대기 후 재시도 |
| Activity 완료 | planner consumer 중단 | fault suite | inbox·재처리 | 동기화 중 표시 |
| Debezium | Connect 중단·slot 지연 | fault suite | WAL 2GB·경보·snapshot 복구 | 기록은 보존, 반영 지연 |
| Kafka | publish 후 offset 전 중단 | 중복 주입 | inbox unique | 중복 결과 없음 |
| Consumer | 영구 payload 오류 | fault suite | 제한 재시도·dead-letter·경보 | 해당 항목 동기화 오류 |
| Activity void | 다음 회차가 이미 생성 | 상태·E2E | 진행 재계산·미래 회차 교체 | 수정 결과 표시 |
| Sticky note | 병렬 PATCH 응답 역전 | component·API 통합 | optimistic version·409 | 저장 실패·수동 재시도 |
| Timer | 백그라운드에서 여러 phase 경과 | 가상 clock·WebKit | deadline 재계산 | 현재 그립으로 복귀 |
| Push | subscription 만료·provider 오류 | 통합·실기기 | 삭제·재시도·만료 | 실패 상태 조회 |
| Migration | 중간 migration 실패 | release fault | 새 배포 중단·기존 유지 | 기존 앱 계속 사용 |
| Runtime | 기본 pool·thread 과다 | 4GB soak | metric 기반 조정·8GB 승급 | 지연 또는 503 |

테스트와 처리 없이 조용히 실패하는 경로는 허용하지 않는다.

## 구현 순서

1. 계약 생성·공통 오류·테스트 gate 기반
2. JPA·Flyway·PostgreSQL Testcontainers 기반
3. Debezium Connect·outbox schema·event contract·inbox 기반
4. 인증·bootstrap·JWT·refresh session
5. 오늘 화면·Task·series·반복·이월·reopen·메모
6. Activity header·detail·Task projection·완료·void
7. 공부 template version·snapshot
8. 프로그램 catalog·enrollment·진급·재계산
9. 푸시업·풀업 private catalog 검증·등록
10. 클라이밍 timer·부분 기록·실기기 검증
11. notification 예약·방해 금지·재시도·만료
12. iPhone 설치 경험과 온라인 오류 처리
13. AWS migration·ECR·OIDC·백업·배포
14. fault suite·4GB soak·복구 리허설·release

## Worktree parallelization

| Step | Modules touched | Depends on |
|---|---|---|
| 계약·오류 기반 | `contracts/`, `packages/api-client/`, `libs/` | 문서 PR |
| Persistence·test 기반 | `services/`, `gradle/`, test support | 문서 PR |
| Debezium 기반 | `infra/`, `contracts/events/` | Persistence |
| 인증 | `planner-service/`, `apps/web/` | 계약·Persistence |
| Planner core | `planner-service/`, `apps/web/` | 인증·계약 |
| Activity core | `activity-service/`, `apps/web/` | Planner event·Debezium |
| 공부 | `activity-service/`, `apps/web/` | Activity core |
| 운동 catalog | `activity-service/`, private catalog | Activity core |
| 클라이밍 | `client-domain/`, `activity-service/`, `apps/web/` | Activity core |
| 알림 | `notification-service/`, `apps/web/` | 인증·Planner event·Debezium |
| 배포 | `infra/`, workflows | 기반 이미지·migration |

- Lane A: 계약·오류 → 인증 → Planner core
- Lane B: Persistence·test → Debezium
- Lane C: Activity core → 공부 → 운동 catalog
- Lane D: Activity core → 클라이밍
- Lane E: 인증·Planner event·Debezium → 알림
- Lane F: 기반 이미지·migration → 배포

Lane A와 B는 병렬로 시작할 수 있다. Activity core 이후 공부·운동·클라이밍은 모듈 충돌이 커서 같은 branch에서 순차 구현한다. 알림과 배포는 선행 계약이 끝난 뒤 별도 worktree에서 병렬 진행할 수 있다.

## Implementation Tasks

- [ ] **T1 (P1)** 계약·클라이언트 생성 기반 구현
  - Files: `contracts/`, `packages/api-client/`, `gradle/`, `package.json`
  - Verify: OpenAPI 생성 drift·event JSON Schema·Java fixture 계약 검사
- [ ] **T2 (P1)** RFC 9457 공통 오류와 trace ID 구현
  - Files: `libs/`, `services/*/`
  - Verify: 서비스별 400·401·403·404·409·422·503 계약 테스트
- [ ] **T3 (P1)** Spring Data JPA·Flyway·Testcontainers 기반 구현
  - Files: `services/*/`, `infra/docker/postgres/`
  - Verify: 실제 PostgreSQL migration·validate·rollback-safe schema 테스트
- [ ] **T4 (P1)** Kafka Connect·Debezium outbox·consumer inbox 구현
  - Files: `infra/`, `contracts/events/`, `libs/event-contracts/`
  - Verify: DB commit부터 consumer 결과까지 중복·재시작 fault 테스트
- [ ] **T5 (P1)** 인증·bootstrap·token rotation 구현
  - Files: `planner-service/`, `activity-service/`, `notification-service/`, `apps/web/`
  - Verify: 로그인·갱신·재사용 탐지·로그아웃·서비스 검증 E2E
- [ ] **T6 (P1)** Task series·occurrence·반복·이월 구현
  - Files: `planner-service/`, `apps/web/`, `client-domain/`
  - Verify: 날짜·동시성·지난 기록·reopen branch 100%
- [ ] **T7 (P1)** 메모와 달력 summary 구현
  - Files: `planner-service/`, `apps/web/`
  - Verify: 42일 조회 query 수·index·autosave 오류 E2E
- [ ] **T8 (P1)** Activity 공통 header·detail·projection 구현
  - Files: `activity-service/`, `planner-service/`, `apps/web/`
  - Verify: Task reference 지연·완료·부분·void·correction E2E
- [ ] **T9 (P2)** 공부 template version·snapshot 구현
  - Files: `activity-service/`, `apps/web/`, `validation/`
  - Verify: 타입·순서·선택 값·과거 snapshot 테스트
- [ ] **T10 (P2)** 프로그램 catalog·enrollment·진급 구현
  - Files: `activity-service/`, catalog importer
  - Verify: schema·checksum·version 고정·성공·실패·void 재계산 테스트
- [ ] **T11 (P2)** 클라이밍 timer·부분 기록 구현
  - Files: `client-domain/`, `activity-service/`, `apps/web/`
  - Verify: 가상 clock 모든 경계·WebKit·실제 iPhone 체크
- [ ] **T12 (P2)** 웹 푸시·방해 금지·만료 구현
  - Files: `notification-service/`, `apps/web/`
  - Verify: 장애 격리·중복·만료·실기기 push
- [ ] **T13 (P2)** React query·routing·bundle budget 적용
  - Files: `apps/web/`, shared packages
  - Verify: 초기 gzip 150KB·request waterfall·관련 E2E
- [ ] **T14 (P2)** AWS migration·배포·백업 복구 구현
  - Files: `infra/`, workflows, runbooks
  - Verify: migration 실패 중단·HTTPS·OIDC·S3 restore
- [ ] **T15 (P2)** 4GB soak·fault suite와 release 판정
  - Files: test support, runbooks
  - Verify: 7일 운영·OOM·swap·p95·consumer lag·WAL·복구

## Completion Summary

- Step 0 Scope Challenge: 전체 MVP 범위 유지
- Architecture Review: 9개 이슈 결정
- Code Quality Review: 8개 이슈 결정
- Test Review: 전체 경로 diagram 작성, 4개 test infrastructure gap 해결
- Performance Review: 5개 이슈 결정
- NOT in scope: 작성 완료
- What already exists: 작성 완료
- TODOS.md updates: 2개 제안, 사용자 결정으로 모두 미추적
- Failure modes: 0개 silent critical gap
- Outside voice: 사용자 요청이 없어 생략
- Parallelization: 6개 lane, 초기 2개 병렬·domain 내부 순차
- Lake Score: 24/27 결정에서 완전한 경로 선택

## GSTACK REVIEW REPORT

| Review | Trigger | Why | Runs | Status | Findings |
|---|---|---|---:|---|---|
| Product Review | 선택 | Scope & strategy | 0 | — | 기존 승인 PRD 사용 |
| External Review | 선택 | Independent second opinion | 0 | — | 실행하지 않음 |
| Eng Review | 전체 기술 리뷰 | Architecture & tests | 1 | CLEAR | 26개 이슈 결정, critical gap 0 |
| Design Review | 선택 | UI/UX gaps | 0 | — | 구현 전 별도 수행 가능 |
| DX Review | 선택 | Developer experience | 0 | — | 내부 개인 프로젝트로 생략 |

**VERDICT:** ENG CLEARED — PRD 1.3.0을 기준으로 구현 가능

NO UNRESOLVED DECISIONS
