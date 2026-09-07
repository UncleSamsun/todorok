# 기반 재검토 보완 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 날짜·복구·상태 점검·DB 권한·계약·디자인·전체 조립에서 확인된 기반 결함을 수정한다.

**Architecture:** OpenAPI 원본과 production provisioning script를 단일 기준으로 유지한다. 단위 경계는 날짜 생성 계약, health parser, PostgreSQL role provisioning, CDC recovery fixture, Compose smoke로 나누고 각 경계를 실제 실행 결과로 검증한다.

**Tech Stack:** OpenAPI Generator 7.24.0, TypeScript 7, React 19.2, Java 25, Spring Boot 4.1.1, PostgreSQL 17.11, Kafka 4.3.1, Debezium 3.6.2.Final, Docker Compose

**Spec:** `docs/superpowers/specs/2026-09-07-foundation-remediation-design.md`

**Status:** 2026-09-07 구현·검증 완료

## Global Constraints

- date-only는 `YYYY-MM-DD` 문자열이고 timezone 변환을 금지한다.
- date-time은 기존 `Date` 생성 계약을 유지한다.
- 기존 volume의 데이터와 event ID를 보존한다.
- 서비스 역할은 자기 schema만 읽고 쓴다.
- Debezium은 두 outbox table만 SELECT한다.
- 설정·스크립트 테스트도 가능한 한 실제 실행 결과를 검증한다.
- 생성 파일을 직접 수정하지 않는다.

---

### Task 1: 날짜·인증·Task 상태 계약

**Files:**
- Modify: `contracts/generator/planner-typescript.yaml`
- Modify: `contracts/generator/activity-typescript.yaml`
- Modify: `contracts/openapi/common-v1.yaml`
- Modify: `contracts/openapi/planner-v1.yaml`
- Modify: `contracts/openapi/activity-v1.yaml`
- Modify: `packages/client-domain/src/index.ts`
- Create: `apps/web/src/api-client.contract.test.ts`
- Modify: `docs/PRD.md`
- Regenerate: `services/*/src/generated`, `packages/api-client/src/generated`

**Interfaces:**
- Produces: TypeScript `format: date` string, protected API bearer header, `TaskStatus.DELETED`.

- [ ] **Step 1: Write failing browser-package contract tests**

테스트는 `CreateTaskRequestToJSON`이 literal `2026-09-07`을 그대로 반환하고, `CalendarApi`가 access token을 `Authorization: Bearer test-token`으로 전달하는지 실제 generated runtime을 호출해 검증한다.

- [ ] **Step 2: Run tests and confirm failures**

Run: `corepack pnpm --dir apps/web test --run src/api-client.contract.test.ts`

Expected: date type 또는 bearer header assertion 실패.

- [ ] **Step 3: Update contract sources and regenerate**

TypeScript generator top-level `typeMappings`에 `Date: string`을 지정한다. common OpenAPI에 `BearerAuth`를 추가하고 planner/activity는 global security를 사용하며 login·refresh만 `security: []`로 지정한다. PRD와 client-domain에 `DELETED`를 추가한다.

- [ ] **Step 4: Verify generated behavior and drift**

Run: `node scripts/generate-contracts.mjs`

Run: `corepack pnpm --dir apps/web test --run src/api-client.contract.test.ts`

Run: `node scripts/check-contract-drift.mjs`

Expected: 모두 PASS, 생성 drift 0.

- [ ] **Step 5: Commit**

Commit: `fix(contract): 날짜와 인증 계약 정합성 보완`

---

### Task 2: 메시징 health fail-closed 처리

**Files:**
- Modify: `scripts/messaging-health.mjs`
- Modify: `scripts/messaging-health.test.mjs`
- Modify: `README.md`

**Interfaces:**
- Produces: `evaluateMessagingHealth(state)`, exact topic config parser, validated database/lag parsers.

- [ ] **Step 1: Write failing tests**

다음 literal case를 추가한다.

```js
evaluateMessagingHealth({
  connectorState: 'RUNNING', taskStates: ['RUNNING'], slotActive: true,
  retainedWalBytes: Number.NaN, consumerLag: Number.NaN,
  outboxOldestAgeSeconds: Number.NaN, inboxOldestAgeSeconds: Number.NaN,
  outboxRowCount: Number.NaN, inboxRowCount: Number.NaN,
  topicPoliciesValid: true,
})
```

Expected: `critical`, reason `measurement_invalid`.

topic parser는 `retention.ms=6048000000`을 `604800000`과 다르다고 판정한다. consumer 출력에 명시적 no-group 문구도 header도 없으면 예외를 던진다.

- [ ] **Step 2: Run and confirm failures**

Run: `node --test scripts/messaging-health.test.mjs`

Expected: NaN case가 healthy라서 FAIL.

- [ ] **Step 3: Implement validated parsers**

DB query는 outbox/inbox count를 추가하고 `psql -v ON_ERROR_STOP=1`을 사용한다. 모든 수치는 finite·0 이상을 검사한다. topic config는 comma-separated key/value map으로 파싱한다. child process와 fetch에는 15초 timeout을 적용한다.

- [ ] **Step 4: Verify**

Run: `node --test scripts/messaging-health.test.mjs`

Expected: PASS.

- [ ] **Step 5: Commit**

Commit: `fix(messaging): 상태 점검 계측 오류 차단`

---

### Task 3: 운영 PostgreSQL role provisioning과 격리

**Files:**
- Modify: `infra/docker/postgres/init/001-create-service-roles.sh`
- Modify: `infra/docker/postgres/replication/initialize-outbox-replication.sh`
- Modify: `infra/docker/compose.yml`
- Modify: `.env.example`
- Create: `tests/messaging-integration/src/test/java/io/todorok/messaging/integration/PostgresRoleProvisioningIntegrationTest.java`
- Create: `tests/test-resources/postgres/service-role-isolation.sql`
- Modify: `services/*/build.gradle.kts`
- Modify: `services/*/src/test/java/**/*PersistenceIntegrationTest.java`
- Delete: `services/*/src/test/resources/db/*-test-role.sql`
- Modify: `README.md`

**Interfaces:**
- Produces: idempotent role script controlled by `POSTGRES_HOST` and `DATABASE_CREDENTIAL_UPDATE`.

- [ ] **Step 1: Write failing role isolation tests**

서비스 테스트는 공통 init SQL로 세 schema·probe table을 만들고 `create-schemas=false`로 migration한다. 자기 `current_user`와 migration 성공, 다른 schema probe SELECT 실패를 검증한다. provisioning 통합 테스트는 production script를 두 번 실행해 marker 보존과 opt-in password rotation을 검증한다.

- [ ] **Step 2: Run and confirm failures**

Run: `gradlew.bat :services:planner-service:test :services:activity-service:test :services:notification-service:test :tests:messaging-integration:test --tests '*PersistenceIntegrationTest' --tests '*PostgresRoleProvisioningIntegrationTest' --no-daemon --max-workers=1`

Expected: 기존 넓은 권한 또는 production script 비멱등 때문에 FAIL.

- [ ] **Step 3: Make role script idempotent and narrow Debezium**

역할·schema는 없을 때 생성한다. 기존 비밀번호는 `DATABASE_CREDENTIAL_UPDATE=true`일 때만 ALTER한다. 모든 service/debezium 역할의 database CREATE를 회수한다. replication init은 Debezium의 전체 table/default SELECT를 회수하고 outbox 두 table만 다시 grant한다.

- [ ] **Step 4: Add postgres-provision startup gate**

Compose에 PostgreSQL health 이후 같은 role script를 실행하는 one-shot service를 추가하고 세 migration이 성공을 기다리게 한다.

- [ ] **Step 5: Verify**

Run: Task 3 Step 2 명령.

Run: `node --test scripts/persistence-compose.test.mjs scripts/postgres-messaging-config.test.mjs`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit: `fix(postgres): 기존 환경 역할과 권한 조정`

---

### Task 4: 실제 Connect 재시작과 slot snapshot 검증

**Files:**
- Modify: `tests/messaging-integration/src/test/java/io/todorok/messaging/integration/MessagingInfrastructureFixture.java`
- Modify: `tests/messaging-integration/src/test/java/io/todorok/messaging/integration/DebeziumOutboxRoundTripTest.java`
- Modify: `docs/superpowers/specs/2026-09-02-outbox-debezium-foundation-design.md`
- Modify: `README.md`

**Interfaces:**
- Produces: `restartConnectWorker()`, `dropReplicationSlot()`, `createReplicationSlot()` test fixture operations.

- [ ] **Step 1: Change recovery tests so they fail**

worker recovery test는 old Connect container를 제거하고 새 container를 시작한다. slot test는 connector stop → slot drop → outbox INSERT → slot create → connector resume 순서를 사용하고 event 수신 뒤 inbox guard 두 번 적용 결과 `true,false`를 확인한다.

- [ ] **Step 2: Run and observe current fixture failure**

Run: `gradlew.bat :tests:messaging-integration:test --tests '*DebeziumOutboxRoundTripTest' --no-daemon --max-workers=1`

Expected: 새 fixture method 부재 또는 기존 순서로 인해 FAIL.

- [ ] **Step 3: Implement worker replacement and split slot operations**

Connect factory method를 추출하고 field를 교체 가능하게 만든다. old container를 stop한 뒤 같은 network·worker group·internal topics로 새 container를 시작한다. slot drop/create를 별도 method로 제공한다.

- [ ] **Step 4: Verify recovery suite**

Run: Step 2 명령.

Expected: 실제 worker 교체와 slot 이전 row 복구 PASS.

- [ ] **Step 5: Commit**

Commit: `test(messaging): 실제 재시작과 snapshot 복구 검증`

---

### Task 5: DESIGN과 반복 규칙 정합성

**Files:**
- Modify: `DESIGN.md`
- Modify: `docs/PRD.md`
- Modify: `docs/plans/2026-09-01-mvp-implementation-plan.md`
- Modify: `docs/superpowers/specs/2026-09-02-outbox-debezium-foundation-design.md`

**Interfaces:**
- Produces: 주·월 navigation, theme selection, 44px mobile grid, past-record recurrence decision table.

- [ ] **Step 1: Apply the approved design rules**

주·월 chevron 동작, theme별 selection 색, 390px 44px 열 배치, 도메인 화면 순서를 설계 문서와 일치시킨다. PRD에 기존 PLANNED·종료 series·과거 계산일·PARTIAL 조합을 표로 기록한다.

- [ ] **Step 2: Scan for contradictions**

Run: `rg -n "비활성화|차콜 배경|약 40px|오늘 날짜의 새 활성" DESIGN.md docs/PRD.md docs/plans/2026-09-01-mvp-implementation-plan.md`

Expected: 폐기한 모순 문구 0개.

- [ ] **Step 3: Commit**

Commit: `docs(product): 달력과 반복 일정 규칙 명확화`

---

### Task 6: 실제 connector 등록과 Compose 조립 smoke

**Files:**
- Create: `infra/docker/compose.smoke.yml`
- Create: `scripts/compose-smoke.mjs`
- Create: `scripts/compose-smoke.test.mjs`
- Create: `scripts/connect-registration.integration.test.mjs`
- Modify: `infra/docker/connect/register-connector.sh`
- Modify: `README.md`
- Modify: `scripts/verify-all.mjs`

**Interfaces:**
- Produces: `node scripts/compose-smoke.mjs`, executable connector registration scenarios.

- [ ] **Step 1: Write failing script tests**

connector test는 임시 HTTP server로 404 create, config drift refusal, explicit PUT을 실행하고 password에 quote·backslash가 있어도 JSON roundtrip이 같은지 확인한다. smoke test의 dry-run은 고유 project name, override, cleanup command를 반환하는지 검증한다.

- [ ] **Step 2: Run and confirm failures**

Run: `node --test scripts/connect-registration.integration.test.mjs scripts/compose-smoke.test.mjs`

Expected: smoke module 또는 executable connector harness 부재로 FAIL.

- [ ] **Step 3: Implement smoke runner**

runner는 build/up, migration exit 확인, Nginx·service health, connector/task/slot, marker 보존 재기동을 수행한다. finally에서 자기 project의 volume과 orphan만 삭제한다.

- [ ] **Step 4: Run full assembly**

Run: `node scripts/compose-smoke.mjs`

Expected: fresh·existing volume PASS and cleanup complete.

- [ ] **Step 5: Commit**

Commit: `test(infra): 기존 환경과 전체 조립 smoke 추가`

---

### Task 7: 기록 정책과 전체 검증

**Files:**
- Modify: `scripts/check-record-policy.mjs`
- Modify: `scripts/verify-all.mjs`
- Modify: `README.md`
- Modify: `docs/ISSUE_ROADMAP.md`
- Modify: Obsidian project MOC and review log outside repository

**Interfaces:**
- Produces: HEAD·index·working tree·untracked file policy scan and final verification evidence.

- [ ] **Step 1: Write failing policy test**

임시 repository에서 금지 문자열을 untracked file과 staged diff에 넣었을 때 검사 exit code 1을 확인한다.

- [ ] **Step 2: Implement complete scan**

tracked HEAD와 commit log 외에 `git diff`, `git diff --cached`, `git ls-files --others --exclude-standard`의 text file도 검사한다.

- [ ] **Step 3: Run all verification**

Run: `node scripts/verify-all.mjs`

Run: `node scripts/compose-smoke.mjs`

Run: `git diff --check`

Expected: 모두 PASS.

- [ ] **Step 4: Update project knowledge and commit**

MOC에 실제 통과 범위와 남은 T2·인증·domain 구현을 기록한다.

Commit: `chore(review): 기반 재검토 보완 완료`
