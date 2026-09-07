# 작업 09A 템플릿 정의 기반 구현 보고

작성일: 2026-09-07. 기준 제품 HEAD: `d2422ba`. 이 보고서는 작업 09 전체가 아니라 상세설계 단계 A인 정의 관리 기반만 완료했음을 기록한다.

## 완료 범위

- 원본 `activity-v1.yaml`에 별도 `Template` tag와 생성·목록·상세·과거 version·새 version·archive API를 추가했다. Java `TemplateApi`와 TypeScript client/model은 원본에서 재생성했다.
- PostgreSQL V5 migration으로 `record_template`, `template_version`, `template_field_identity`, `template_field_definition`, `template_management_command`를 추가했다. 최초 version 1/revision 0, deferrable current-version FK, owner+command unique, 전역 fieldId unique, type-unit/FK/position 제약과 version/field/command 불변 trigger를 둔다.
- owner 인증 컨텍스트, owner+command advisory lock과 fingerprint/최초 응답 replay, identity 행 `FOR UPDATE`와 expected revision, append-only version/field, one-way archive를 구현했다.
- domain-kind 조합, 공백 이름, 다섯 field type, NUMBER 자유 단위, TIME 초·분·시간과 생략 시 `초`, 나머지 unit 금지, 중복/타 template/제거된 fieldId/type 변경 거부를 구현했다. `fields=[]`와 512개 field 저장을 확인했으며 field count 상한은 추가하지 않았다.
- `(created_at DESC, id DESC)` owner keyset pagination과 filter-bound opaque cursor를 구현했다. cursor에는 owner를 넣거나 신뢰하지 않는다.
- 각 TemplateResponse는 한 SQL statement snapshot에서 identity와 한 current version의 전체 fields를 읽는다. 과거 version도 한 statement로 이름·단위·순서를 읽는다.
- template controller에만 적용되는 request-body advice에서 실제 body 1 MiB를 세고 duplicate JSON key를 거부하며 identity 이외 Content-Encoding을 415로 거부한다. Nginx도 template 경로 전용 1 MiB와 공통 Problem Details 413을 반환한다. 기존 Activity controller/body 정책은 수정하지 않았다.
- 정의 저장은 `task_reference`, Activity, outbox/event를 쓰지 않는다.

## RED와 GREEN 증거

의도한 RED 전 첫 실행은 test source의 JPA sample entity 검증 때문에 endpoint까지 도달하지 못했다. test에 `spring.jpa.hibernate.ddl-auto=none`을 지정해 이 harness 간섭을 제거한 뒤 같은 명령으로 제품 RED를 다시 확인했다.

```powershell
.\gradlew.bat :services:activity-service:test --tests io.todorok.activity.template.TemplateFoundationHttpTest --no-daemon --max-workers=1
```

- RED: 1 test, 1 failed. 실제 `POST /api/activity/v1/templates` 응답이 기대 201 대신 404였다.
- 최소 구현 뒤 GREEN: 1 test, 1 passed, BUILD SUCCESSFUL. 실제 PostgreSQL migration, JWT 인증, POST/GET, 다섯 형식, TIME 기본 `초`, version 1/revision 0, Task/outbox 0건을 확인했다.
- 시나리오 확장 뒤 같은 class 전체: 7 tests, 7 passed. 이후 512 fields와 DB/command 불변 검증을 추가했다.

후속 DB 제약 변경 전 관련 suite 전체를 한 번 실행했다.

```powershell
.\gradlew.bat :services:activity-service:test --no-daemon --max-workers=1
```

- 28 tests, failures 0, errors 0, skipped 0, BUILD SUCCESSFUL.
- 그중 TemplateFoundationHttpTest는 8/8이었다. 기존 Activity HTTP/service, migration 재실행, schema 경계도 함께 통과했다.

마지막 TIME unit DB `NOT NULL` 제약 강화 뒤 기본 `초` 정규화와 실제 null 직접 삽입 rollback만 focused 재검증했다.

```powershell
.\gradlew.bat :services:activity-service:test `
  --tests io.todorok.activity.template.TemplateFoundationHttpTest.createsAndReadsACompleteFiveTypeDefinitionWithoutCreatingTasksOrEvents `
  --tests io.todorok.activity.template.TemplateFoundationHttpTest.databaseRejectsMutationOrDeletionOfEveryHistoricalDefinitionTable `
  --no-daemon --max-workers=1
```

- 2 tests, 2 passed, BUILD SUCCESSFUL. null TIME definition과 함께 넣은 field identity도 같은 transaction에서 rollback되어 0건임을 확인했다.

## HTTP·경쟁·DB 검증 내용

- 실제 HTTP: 다섯 형식, 빈 fields, 512 fields와 position 순서, 이름 변경과 과거 version 이름/필드 보존.
- 검증/rollback: 잘못된 domain-kind, 공백 이름, 부적절 unit, 중복 fieldId, 제거된 fieldId, 타 template fieldId, type 변경, unknown property, duplicate JSON key, oversized/chunked, Content-Encoding. 실패 뒤 template/version/command 중간 저장이 없음을 확인했다.
- owner: 타 owner의 상세와 과거 version은 동일한 404이며 목록에도 노출되지 않는다.
- command: 동일 create/version/archive command와 동일 payload는 최초 body를 반환한다. 다른 payload 재사용은 `COMMAND_REUSE` 409다. 동일 create command 두 동시 HTTP 요청도 한 identity/같은 응답만 남겼다.
- 경쟁: expected revision 0의 두 version 요청 중 201 한 건/409 한 건만 남고 version은 총 2개다. version과 archive 경쟁도 성공 한 건/409 한 건, 최종 revision 1만 남는다.
- archive: 기본 목록에서 제외, `includeArchived=true`에서 포함, 새 version 409, owner의 과거 version GET 허용, 동일 archive command 최초 응답 replay.
- DB: historical version/field identity/field definition update·delete, identity/owner 변경, 비연속 revision, record delete, command update·delete를 실제 PostgreSQL에서 거부했다. current-version FK와 전역 field identity unique는 정상 관리 흐름과 타-template 거부로 확인했다. Flyway 재실행은 0 migrations executed였다.
- 일관성: create/GET body가 같고, current/historical version은 fields 전체를 단일 query snapshot으로 반환한다.

## 프록시·계약·생성·빌드

```powershell
node --test scripts/api-routing.test.mjs scripts/template-nginx.integration.test.mjs
```

- 4/4 통과. 격리 Docker network의 실제 Nginx→stub upstream에서 정확히 1,048,576 bytes는 200과 수신 bytes 1,048,576을 반환했고, Content-Length 없는 chunked 1,048,577 bytes는 upstream에 전달되지 않고 공통 `application/problem+json` 413/code/trace를 반환했다.

```powershell
corepack pnpm test:contracts
node scripts/check-contract-drift.mjs
corepack pnpm --filter @todorok/api-client test
corepack pnpm --filter @todorok/api-client build
.\gradlew.bat :services:activity-service:assemble --no-daemon --max-workers=1
```

- contract tests 6/6. 생성 TypeScript의 TemplateResponse version+fields 직렬화 왕복 포함.
- drift: `생성 계약이 원본과 일치합니다.`
- api-client tests 5/5, TypeScript noEmit/build 성공.
- activity-service app jar와 migration jar assemble 성공.

## 남은 범위와 우려

- 작업 09B의 selection binding, planner Task/series, 이벤트, Activity 기록 값/snapshot/correction 연결은 의도적으로 구현하지 않았다. 작업 09C의 관리·추가·기록 UI도 미구현이다. 따라서 전체 작업 09와 M3는 완료가 아니다.
- 전체 저장소 `verify-all.mjs`는 실행하지 않았다. 이번 변경의 관련 activity suite, 실제 proxy, 계약 생성/roundtrip/drift와 두 build를 각각 실행했다. 새 proxy/roundtrip test는 이후 `verify-all.mjs` 목록에 포함했다.
- 기존 migration V1–V4, ActivityService/ActivityDetailStore, StudyDetail 입력 계약, Task/series/event/legacy 데이터는 수정하지 않았다.
