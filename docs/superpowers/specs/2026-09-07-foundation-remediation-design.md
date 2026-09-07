# 기반 재검토 보완 설계

## 목적

현재 계약·persistence·메시징 기반에서 재현된 날짜 이동, 불완전한 복구 검증, 상태 점검 오판, 운영 권한 차이와 문서 충돌을 해결한다. 완료 후에는 공통 오류·인증·오늘 할 일 기능을 구현할 수 있는 일관된 기준을 제공한다.

## 범위

- OpenAPI date-only TypeScript 값을 `YYYY-MM-DD` 문자열로 생성
- bearer 인증 계약과 `DELETED` Task 상태 통일
- Debezium slot 유실 전 outbox row의 snapshot 복구 검증
- Connect worker 실제 컨테이너 교체 후 offset 복구 검증
- 메시징 상태 점검의 계측 오류 fail-closed 처리와 건수 출력
- PostgreSQL 서비스 역할을 기존 volume에서도 조정하는 idempotent provisioning
- Debezium 역할의 조회 범위를 outbox 두 table로 제한
- 실제 운영 역할을 사용하는 schema 격리 테스트
- DESIGN의 달력·도메인 화면 충돌 제거
- 지난 기록과 반복 회차 생성 규칙 확정
- 격리 Compose 전체 조립 smoke 명령
- 기록 정책 검사의 working tree·index 포함

## 비범위

- 실제 인증 업무 로직
- Task·Activity controller와 domain entity
- 공통 runtime 오류 handler와 trace ID
- 실제 업무 endpoint의 Nginx E2E
- 월별 도메인 집계 API 구현
- 4GB 장기 soak와 실제 iPhone 검증

## 날짜 계약

OpenAPI `format: date`는 TypeScript에서 `string`으로 생성한다. 값은 `YYYY-MM-DD` 형식이며 timezone 변환을 하지 않는다. `format: date-time`은 기존처럼 `Date`를 유지한다. 생성된 request와 response 변환 함수가 `2026-09-07`을 그대로 왕복하는 테스트를 둔다.

Spring 생성 계약의 `LocalDate`는 유지한다. TypeScript 생성 설정만 `Date=string` type mapping을 적용한다.

## 인증과 Task 삭제 상태

planner와 activity API는 기본적으로 bearer access token을 요구한다. planner의 login과 refresh만 `security: []`로 공개한다. logout, 달력, Task, Activity endpoint는 보호한다. 생성 client에서 access token을 설정하면 `Authorization: Bearer <token>` header가 실제 요청에 포함되어야 한다.

Task soft delete는 lifecycle 상태 `DELETED`로 표현한다. PRD와 client-domain의 `TaskStatus`에 `DELETED`를 추가한다. 일반 달력·상세 조회는 기본적으로 삭제 Task를 제외하며 event에는 `status=DELETED`, `changeType=DELETED`를 사용한다.

## 반복 일정과 지난 기록

지난 기록의 `COMPLETED` 저장은 선택한 과거 Task를 완료한다. 다음 활성 회차는 아래 순서로 결정한다.

1. 같은 series에 이미 `PLANNED` 회차가 있으면 새 회차를 만들거나 날짜를 바꾸지 않는다.
2. series가 종료됐거나 종료 조건을 넘으면 새 회차를 만들지 않는다.
3. 다음 회차는 완료한 회차와 반복 규칙을 기준으로 계산한다.
4. 계산 날짜가 오늘보다 과거일 때만 멱등 이월 규칙으로 오늘로 옮긴다.
5. 오늘 또는 미래 날짜면 계산된 날짜를 유지한다.
6. `PARTIAL` Activity는 Task를 완료하지 않고 다음 회차를 만들지 않는다.

따라서 월요일 주간 반복의 지난 기록을 수요일에 저장하면 다음 월요일 회차를 만든다. 이미 다음 월요일 회차가 있으면 기존 회차를 유지한다.

## 메시징 복구 검증

장애 검증을 세 종류로 구분한다.

- 일시 단절: Connect·Kafka container pause/unpause
- worker 재시작: Connect container를 제거하고 동일 network·Kafka internal topic으로 새 worker를 시작
- slot 유실: connector를 중지하고 slot을 삭제한 뒤 outbox에 event를 넣고 새 slot을 생성한다. 이 event는 새 slot의 streaming 범위보다 앞서므로 수신되면 `when_needed` snapshot 복구가 증명된다.

slot 유실 테스트는 snapshot으로 재발행된 event ID가 유지되고, 같은 event ID를 inbox guard에 두 번 적용해 local 결과가 한 번만 바뀌는지 확인한다.

## 메시징 상태 점검

상태 점검은 알 수 없는 값을 정상으로 간주하지 않는다.

- 숫자는 finite·0 이상이어야 한다.
- connector/task/slot 정보 누락은 critical이다.
- psql은 `ON_ERROR_STOP=1`로 실행한다.
- consumer group이 없다는 명시적 출력만 lag 0으로 해석한다.
- topic config는 key/value로 파싱해 정확히 비교한다.
- outbox·inbox row count와 oldest age를 JSON에 포함한다.
- probe 실행에는 timeout을 적용한다.

계측 형식이나 값이 잘못되면 `measurement_invalid` 또는 구체적인 probe failure reason을 반환하고 exit code 1로 끝낸다.

## PostgreSQL 역할과 기존 volume

기존 `docker-entrypoint-initdb.d`는 새 volume에서만 실행된다. 같은 역할 조정 script를 one-shot `postgres-provision` 서비스에서도 실행한다.

script는 다음을 idempotent하게 수행한다.

- 네 역할이 없으면 생성
- 세 schema가 없으면 각 서비스 역할 소유로 생성
- schema owner와 CONNECT 권한 조정
- public schema CREATE 회수
- 서비스·Debezium 역할의 database CREATE 회수
- Debezium의 planner/activity 전체 table 권한 회수 후 outbox 두 table SELECT만 부여
- planner/activity default table SELECT grant 회수

기존 역할 비밀번호는 `DATABASE_CREDENTIAL_UPDATE=true`일 때만 변경한다. Debezium 비밀번호를 변경할 때는 `CONNECTOR_CONFIG_UPDATE=true`도 같은 실행에서 사용한다. README에 순서와 실패 복구를 기록한다.

서비스 migration은 `postgres-provision` 성공 뒤 실행한다. 테스트는 production provisioning script를 실제 PostgreSQL container에서 실행하고 자기 schema migration 성공, 다른 schema SELECT·INSERT·DDL 실패, Debezium의 non-outbox SELECT 실패를 확인한다.

## 디자인 기준 정리

- 주·월 모두 같은 좌우 chevron을 사용한다. 주 보기에서는 7일, 월 보기에서는 1개월 이동한다.
- 선택 날짜 배경은 라이트 `#F0F2F3`, 다크 `#303438`를 사용한다.
- 390px 이하에서는 앱·달력 가로 padding을 각각 8px, 주차·카테고리 label 열을 46px, 열 간격을 0으로 둔다. 7개 날짜 열은 `minmax(44px, 1fr)`이다.
- 도메인 화면 순서는 월별 요약·행동, 오늘 할 일·루틴, 기록 유형, 최근 기록이다.

## 전체 조립 smoke

`scripts/compose-smoke.mjs`는 고유 Compose project와 임시 env를 사용한다. host port를 공개하지 않는 smoke override로 image를 build하고 전체 서비스를 시작한다.

검증 항목은 다음과 같다.

- migration one-shot 성공
- planner·activity·notification·PostgreSQL·Kafka·Connect·Nginx health
- Nginx `/health`와 두 서비스 actuator 경로
- connector와 task RUNNING, slot active
- 기존 volume 재기동 후 marker 유지
- provisioning 재실행 성공

항상 `docker compose down --volumes --remove-orphans`로 자기 project만 정리한다.

## 완료 기준

- 날짜와 health 재현 테스트가 수정 전 실패하고 수정 후 통과한다.
- 실제 worker 교체와 slot 이전 row snapshot 복구가 통과한다.
- production 역할 script 기반 권한 격리가 통과한다.
- 계약 생성 drift가 없다.
- Compose smoke가 fresh·existing volume에서 통과한다.
- 전체 검증과 `git diff --check`, 기록 정책 검사가 통과한다.
