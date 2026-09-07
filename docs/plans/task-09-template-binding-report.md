# 작업09B1 선택 승인·일정 연결 보고서

상태: 09B1 구현·검증 완료. 09B2 값·snapshot·legacy와 09C 화면은 범위 밖이며 단계B 전체 또는 작업09 전체 완료를 뜻하지 않는다.

중요 리뷰 라운드1의 두 지적은 아래 추가 회귀와 최소 수정으로 보완했다. 수정 diff의 부모 재검토는 별도다.

## 인터페이스와 상태 경계

- 내부 원본: `contracts/openapi/template-internal-v1.yaml`, 실제 POST `/api/activity/v1/internal/template-selections`. 생성 인터페이스/DTO: `libs/web-support/src/generated/java/io/todorok/internal/api`.
- 공개 planner 생성에는 선택적 commandId/templateSelection이 추가된다. 연결 선택에는 commandId가 필수이고 GENERAL은 거절한다. Task/series 공개 응답은 서버가 만든 TemplateLink를 반환한다.
- activity V6: 불변 template_selection_binding, task_reference 링크. planner V9: planner_creation_command와 Task/series 불변 링크. 기존 migration을 수정하지 않는다.
- planner command 등록과 안정적인 target UUID 저장을 먼저 커밋하고 외부 호출한다. 승인 뒤 Task 또는 series·첫 회차·outbox·최초 응답을 한 로컬 트랜잭션으로 저장한다. 승인만 남는 경우는 정상 복구 상태이며 다른 대상에 재사용하지 못한다.
- command가 있는 생성은 templateSelection이 없어도 동일 registry를 통과한다. 기존 연결 요청에서 selection을 제거하거나 대상 종류를 바꿔 command 재사용 검사를 우회하지 못한다. 기존 command 없는 무연결 생성은 이전 방식으로 처리한다.
- v2 TaskScheduled/TaskChanged/TaskRolledOver/SeriesChanged의 닫힌 JSON Schema와 valid/invalid fixture를 추가했다. `scripts/generate-event-v2.mjs`는 원본 payload에서 정확한 Java record를 만들며 generateContracts와 drift 검사에 포함한다. 모든 새 일정 이벤트는 envelope version2, 기존 topic을 사용한다. seriesId/templateLink는 없는 경우 명시적 null이며 이벤트 링크는 세 identity 필드만 전송한다. 표시 이름/요약은 binding의 선택 당시 version에서 복구한다.
- GET `/api/activity/v1/tasks/{taskId}/record-template`: linked=false 또는 서버 binding과 현재 전체 정의를 반환한다. projection 미도착은 409 TASK_NOT_READY/retryable=true, 타 owner는404다. GET은 쓰기를 수행하지 않는다.
- 연결된 Task의 기록 생성은 `TEMPLATE_RECORD_NOT_READY` 409/retryable=false로 명시적으로 차단한다. 09B2에서 이 경계를 typed 자유 값 검증·현재 버전 고정·서버 snapshot 저장으로 교체한다. 기존 무연결 기록의 입력·조회 계약은 이번 변경으로 이행하지 않는다.

## 설정과 배포 순서

서비스 키와 사용자 access-token 키는 분리한다. 내부 issuer/subject는 `todorok-planner`, audience는 `todorok-activity-internal`, scope는 `template:select`, POST/path는 고정이며 수명은 최대60초다. ownerId/requestId/targetType/targetId와 실제 UTF-8 요청 본문의 SHA-256을 서명 claims에 결합한다.

| 설정 | 소비 서비스 | 의미 |
| --- | --- | --- |
| TODOROK_TEMPLATE_SERVICE_PRIVATE_KEY_FILE | planner | 별도 PKCS8 private key의 Spring resource 위치 |
| TODOROK_TEMPLATE_SERVICE_PUBLIC_KEY_FILE | activity | 별도 X509 public key의 Spring resource 위치 |
| TODOROK_TEMPLATE_SERVICE_BASE_URL | planner | 기본 `http://activity-service:8082/api/activity/v1` |

선행 순서는 별도 키 파일 준비 → activity migration와 v1/v2 소비·내부 인증 배포 → planner migration와 승인 client/producer 배포 → Nginx 내부 경로 차단 확인 → 왕복 검증이다. 설정 미지정 시 일반 서비스와 migration/bootstrap 동작을 유지하면서 내부 승인은 fail-closed한다. 서비스 토큰은 공개 사용자 API에 사용할 수 없다.

선택 기능 배포에는 `infra/docker/compose.template-service.yml`을 기본 Compose와 함께 사용한다. 호스트 환경변수는 실제 파일 경로이며 override가 서비스 안의 `file:/run/secrets/...` 위치로 변환한다. private/public key의 서비스별 secret 분리와 기본 기동 호환을 보존했다. 상세는 `docs/runbooks/template-service-auth.md`다.

## 검증 기록

작업 경로: `C:\workspace\todorok-worktrees\functional`.

| 명령 | 결과 | 확인한 경계 |
| --- | --- | --- |
| `.\gradlew.bat :services:activity-service:test --tests '*TemplateFoundationHttpTest.selectionIsImmutable*' --tests '*TemplateFoundationHttpTest.internalApproval*' --console=plain` | 최초 RED 2개 실패 | 승인201 기대→401, 사용자 토큰 내부401 기대→404. 실제 PostgreSQL/HTTP |
| 내부 계약 생성 후 동일 검증을 포함한 실행 | 신규2개 GREEN | 동일 요청/보관 후 재전달, 새 선택 보관 거절, 사용자 JWT·audience·scope·만료·본문 해시 변조 |
| `.\gradlew.bat :services:activity-service:test :services:planner-service:test --tests '*TemplateFoundationHttpTest.selectionIsImmutable*' --tests '*TemplateFoundationHttpTest.internalApproval*' --tests '*TaskHttpIntegrationTest.templateSelectionRequires*' --console=plain` | activity43개 중41통과/2실패, planner1개 RED | Gradle 옵션이 마지막 task에만 적용되어 activity 전체가 실행됨. activity 실패2개는 신규 migration에 따른 개수 기대치이며 후속 수정. planner는 command 없는 연결 요청400 기대→201 |
| `.\gradlew.bat :services:activity-service:test --tests '*TemplateFoundationHttpTest.v2Projection*' :services:planner-service:test --tests '*TaskHttpIntegrationTest.templateSelectionRequires*' --console=plain` | activity1개 RED, planner1개 GREEN | v1 전용 EventJson이 v2 거절. planner command 필수·서비스 미설정503·Task 미생성 |
| `.\gradlew.bat :services:activity-service:test --tests '*TemplateFoundationHttpTest.v2Projection*' --console=plain` | 1개 GREEN | 실제 DB binding 검증·projection·owner GET·늦은 v1 후 링크 보존·다른 Task 재사용 거절 |
| `.\gradlew.bat :tests:messaging-integration:test --tests '*ActivityServiceRoundTripTest.templateBindingRecoversApprovalAndPlannerFailuresAndPropagatesSeriesThroughKafka' --tests '*ActivityServiceRoundTripTest.templateServiceOutageAndLostApprovalResponseRecoverWithTheOriginalCommand' --console=plain` | 최초 2개 중1통과/1실패 | 실제 승인 이후 planner outbox 실패·동시 command 복구까지 통과. 최초 linked Task의 projection await에서 v2 미발행 RED. 실제 서비스 연결 실패와 승인 응답 유실 복구 fixture는 통과 |
| `.\gradlew.bat :services:activity-service:test --tests '*TemplateFoundationHttpTest.v2Projection*' --console=plain` | v2 identity-only·명시적 null 변경 뒤1개 GREEN | 표시는 이벤트 payload에 복사하지 않고 binding 정의에서 조회 |
| `.\gradlew.bat :services:planner-service:test --tests '*TaskHttpIntegrationTest.templateSelectionRequires*' --console=plain` | selection 제거 우회 RED→최소 수정→GREEN 1개 | 기존 command로 selection 제거 시409 기대→201 실패를 확인. command가 있으면 무연결 요청도 registry에 대조하도록 수정 |

최초 생성기 연결 중 jakarta.annotation 의존성 누락과 외부 schema ref의 inline 이름 생성으로 compile 실패가 있었다. 생성 의존성과 공통 schema의 명명된 local ref를 보완했다. 이를 기능 RED로 계산하지 않는다.

v2 발행 뒤 최초 왕복 재검증에서도 projection await가 실패했다. 경계를 나눠 확인한 결과 planner outbox1건/version2/정확한 bindingId와 독립 Kafka consumer의 같은 eventId 수신은 통과했다. 다음 JSON 동등 비교에서 DB payload의 `seriesId:null`만 Kafka payload에서 누락된 것을 확인했다. 이는 Debezium JSON 확장의 기본 null 동작인 ignore와 일치한다. [공식 Outbox Event Router 설정](https://debezium.io/documentation/reference/3.1/transformations/outbox-event-router.html)에서 optional_bytes가 null을 보존하는 동작을 확인했다.

해결은 `infra/docker/connect/connector-template.json`에서 원본 planner/activity outbox topic에 각각 명시적인 TopicNameMatches predicate를 적용한 두 SMT다. plannerOutbox만 optional_bytes를 사용하고 기존 activity outbox는 ignore를 유지한다. 첫 SMT가 topic을 바꾼 뒤 두 번째 SMT가 메시지를 다시 변환하지 않도록 negate predicate를 사용하지 않는다. v2의 명시적 null 원본 계약과 소비 검증은 유지했으며 기존 Activity v1의 생략 형식을 전역으로 변경하지 않았다. 원본 DB JSON↔실제 Kafka JSON 동등 검사를 영구 회귀로 남겼다.

SMT 수정 뒤 `.\gradlew.bat :tests:messaging-integration:test --tests '*ActivityServiceRoundTripTest' --console=plain`을 실행했다. 16개 중 신규2개와 기존13개가 통과했다. 유일한 실패는 기존 rollback 테스트의 전역 climbing_round 건수<=1 가정이었다. 해당 사용자에게 성공한 CLIMBING 기록이 없는 시나리오이므로 owner 범위0으로 바꿔 롤백 검증을 강화했다. 후속 `.\gradlew.bat :tests:messaging-integration:test --tests '*ActivityServiceRoundTripTest.concurrentCommandsPartialAndAtomicRollback' --console=plain`은1개 통과·exit0이며 전체16개를 반복하지 않았다. 신규 왕복은 실제 승인·planner 최종 저장 실패·동시 Task/series 명령·한 번만 생성된 첫 회차·정의 갱신·archive·skip·이월·Kafka 중복/역순·잘못된 binding DLT·응답 유실 복구를 통과했다.

마지막 서비스 검증 명령은 `.\gradlew.bat :services:activity-service:test :services:planner-service:check :libs:event-contracts:test :libs:messaging-support:test :libs:web-support:test :services:activity-service:assemble :services:planner-service:assemble --max-workers=2 --console=plain`이다. planner46개 전부 통과, activity47개 중46개 통과했다. activity의 유일한 실패도 기존 정의 생성 테스트의 전역 task_reference0 가정이었다. 신규 projection 테스트의 타 owner 행과 분리하여 해당 owner의 TaskReference/outbox가0인지 확인하도록 수정했다.

후속 `.\gradlew.bat :services:activity-service:test --tests '*TemplateFoundationHttpTest.createsAndReadsACompleteFiveTypeDefinitionWithoutCreatingTasksOrEvents' :services:planner-service:check :libs:event-contracts:test :libs:messaging-support:test :libs:web-support:test :services:activity-service:assemble :services:planner-service:assemble --max-workers=2 --console=plain`은 exit0이다. activity 수정1개, event-contracts23개, messaging-support9개, web-support14개 통과. planner46개 통과 결과로 Jacoco check를 수행했고 두 서비스 main/migration 및 planner bootstrap JAR 빌드가 성공했다. row lock으로 승인→archive/ archive→승인 양방향을 강제한 DB 검증과 내부 JWT claim/owner/domain/version/동시 request 검증도 마지막 activity 전체 실행에서 통과했다.

`node --test scripts/template-contract-roundtrip.test.mjs scripts/openapi-contract.test.mjs scripts/check-contract-drift.test.mjs scripts/connect-config.test.mjs` 9개 통과. `pnpm --filter @todorok/api-client build` exit0. `node --test scripts/template-nginx.integration.test.mjs` 1개 통과(내부 root·승인·하위경로4개404, 공개 Activity proxy200, 실제1MiB 및 chunked413).

`docker compose --env-file .env.example -f infra/docker/compose.yml -f infra/docker/compose.template-service.yml config --format json`에 두 서비스용 host key 파일 환경변수를 지정해 파싱했다. planner private/ activity public secret만 추가되고 기존 user auth secrets가 유지되는 것, URL8082와 container resource 위치를 확인했다. 이 명령은 구성 검증이며, 실제 파일 마운트와 기동 검증은 아래 별도 Compose smoke에서 수행했다. 앞선 두 서비스 HTTP suite는 독립 키 쌍을 메모리에서 생성해 사용했다.

추가 gate: `node scripts/check-contract-drift.mjs` 원본 일치, `node --test scripts/contracts-generation.test.mjs` 1개 통과, `node --test scripts/connect-registration.integration.test.mjs` 5개 통과. `.\gradlew.bat :libs:web-support:test --tests '*TemplateServiceTokensTest' --console=plain` 2개 통과로 임시 PEM private/public 파일을 실제로 읽어 서비스 토큰을 서명·검증하고 미설정·사용자 키 재사용 거절을 확인했다. 앞서 실행한 web-support14개와 합해16개다.

별도 Compose smoke의 최초 이미지 빌드에서는 새 event generator의 Node 실행 파일이 JDK builder에 없어 실패했다. 세 서비스가 공통 event-contracts를 빌드하므로 planner/activity/notification Dockerfile에 Node24 bookworm-slim 단계의 실행 파일을 builder에만 공급했다. generation을 생략하지 않았으며 최종 JRE 단계에는 Node를 복사하지 않는다. 보완 뒤 세 서비스와 connect-init 이미지 빌드·health·bootstrap이 통과했다.

파일 키를 실제로 마운트한 smoke에서 서비스 JWT 양성 요청201, aud 누락401, 선택 승인→linked Task→같은 command 재전달→Kafka projection→archive 정책→연결 기록409가 통과했다. 추가 공개 API 입력 probe는 expectedTemplateVersion 소수1.5와 문자열"1"이 정수로 변환되어201을 반환하는 문제를 발견했다. `.\gradlew.bat :services:planner-service:test --tests '*TaskHttpIntegrationTest.templateVersionMust*' --console=plain`에서 Task/series2개 RED를 확인하고, 해당 두 생성 요청에만 TemplateCreationBodyAdvice를 적용하여 역직렬화 전 정수 JSON 노드를 검사했다. 전역 mapper와 다른 planner API의 입력 방식은 바꾸지 않았다.

후속 `.\gradlew.bat :services:planner-service:test --tests '*TaskHttpIntegrationTest' :services:planner-service:assemble --max-workers=2 --console=plain`은22개 통과·JAR3개 빌드 성공이다. 두 생성 API 각각 소수/문자열400·command/Task/series/outbox0과 정수1 양성 대조를 확인했고 기존HTTP20개도 통과했다. 앞서 planner 전체46개에서 늘어난 두 case를 합해48개이며 마지막 입력 경계 변경은 이22개와 실제 Compose 재검증으로 확인한다.

실제 Compose smoke는 별도 project `todorok-task09b-smoke`, 공개 `localhost:5189`, `compose.template-service.yml` override와 사용자/서비스용 서로 다른 파일 키2쌍으로 실행했다. 재현 helper는 비밀값을 포함하지 않는 `.local/task09b-runtime/runtime.mjs`이며 아래 순서로 실행했다. helper·임시 runtime 자료는 commit에 포함하지 않는다.

```powershell
node .local/task09b-runtime/runtime.mjs prepare
node .local/task09b-runtime/runtime.mjs build
node .local/task09b-runtime/runtime.mjs up
node .local/task09b-runtime/probe.mjs
node .local/task09b-runtime/audience-probe.mjs
node .local/task09b-runtime/runtime.mjs build-planner
node .local/task09b-runtime/runtime.mjs up
node .local/task09b-runtime/probe.mjs
node .local/task09b-runtime/runtime.mjs status
node .local/task09b-runtime/runtime.mjs down
```

첫 probe의 소수/문자열201 RED 뒤 planner를 재빌드해 교체한 최종 probe는 exit0이다. Task/series 각각 잘못된 두 타입400, 정수1 생성·같은 command 재전달, 파일 키 내부 승인, 실제 Kafka projection, archive 신규 선택 차단/기존 연결 유지, 연결 기록409를 확인했다. 같은 Activity 이미지의 audience-probe는 유효 서명201·aud 누락401 UNAUTHORIZED로 exit0이었다. 최초 `up --wait`가 종료된 일회성 connect-init을 실패로 처리한 helper 문제는 앱 기동과 `run --rm connect-init`을 분리하여 해결했으며 마지막 up/status는 전체 앱 healthy·등록 job exit0이었다.

마지막 planner 이미지 ID는 `sha256:ebb71b3f64edfe05fa007a3220da35b06b4f5ecaabce33289cd644e05dfe2892`다. down은 exit0, 전용 project 컨테이너·volume·network는 모두0개이고 임시 key4파일·runtime.env·state.json6파일은 제거됐음을 확인했다. 테스트 외부의 기존 runtime은 이 정리 대상이 아니다.

검증 수는 서로 다른 코드 시점을 구분한다: activity47개(46통과 후 격리 수정1개 통과), planner48개(기존46개 후 입력 경계 관련22개 재검증으로 새2개 포함), event-contracts23개, messaging-support9개, web-support16개(14+PEM2), 실제 서비스 왕복16개(15통과 후 격리 수정1개 통과), Node 계약·등록·Nginx16개(9+1+5+1). 실제 Compose probe와 구성 검증, 생성 drift, JAR/API-client 빌드는 별도다. 키·토큰 값은 보고서나 commit에 저장하지 않았다.

과거 완료·correction 후 반복 identity 유지 검증은 crafted ActivityCompleted/Corrected를 실제 Kafka에 전달하여 기존 planner 완료 소비 경계를 실행한다. 이 결과를 자유 템플릿 값의 사용자 기록 저장 E2E로 해석하지 않는다. 해당 생성 API는 의도적으로09B2 전까지409이며 값 저장·snapshot·사용자 전체 E2E는 후속 범위다.

## 남은 작업

09B1의 구현·검증 gate는 완료했다. 기록 쓰기의 명시적 준비 상태 경계는 아래09B2에서 교체한다.

09B2: 자유 값 다섯 타입·관계형 운동/클라이밍 값·snapshot·최초 저장 시 현재 버전 비교·correction 기존 snapshot 보존·legacy 보존/새 무검증 입력 거절·시간 집계 회귀. 09C: 관리·선택·기록 화면과 전체 사용자 E2E.

## 중요 리뷰 라운드1 — 2026-09-08

제품 기준은 `8ed2d4a`다. 작업 중 부모가 보존한 문서 commit `22e3d50`은 제품 기준을 바꾸지 않았다. 대상은 `task-09-template-binding-review.md`의 Important2건이며 위 최초 B1 검증 수와 이번 검증 수를 구분한다.

첫 결함은 중복 `templateSelection`의 앞 객체와 뒤쪽 null을 tree와 generated DTO가 다르게 처리하는 것이었다. 소수1.5 또는 문자열"1"을 앞 객체에 넣으면 tree 검사가 건너뛰어지고 DTO는 정수1로 변환한 앞 객체를 유지했다. 공개 Task/series 실제HTTP에서 네 조합 모두400 기대에503을 반환해 서비스 승인 처리에 진입하는 것을 RED로 확인했다. 테스트 서비스 키가 미설정된 경계의503이며, 올바른 입력으로 허용돼 command 등록 이후로 진행했다는 증거다.

수정은 두 생성 요청에만 적용되는 TemplateCreationBodyAdvice의 전용 JsonMapper에 STRICT_DUPLICATE_DETECTION을 켠 것이다. 생성 모델의 null 처리나 전역 mapper는 바꾸지 않았다. 네 공격 요청은 MALFORMED_JSON400이며 owner의 command/Task/series/outbox가 모두0이다. 중복 없는 정수1은 정상 승인 경계까지 진행하고, 기존 무연결 Task/series 생성은201을 반환한다.

두 번째 결함은 이미 TASK binding이 있고 series_id가null인 reference에 `templateLink:null`과 임의seriesId를 담은 v2를 보낼 때 identity가 변경되는 것이었다. 현재 aggregateVersion1에 대해0·1·2를 각각 보내는 실제DB 회귀3개 모두 예외 없이 허용되는 것을 RED로 확인했다. 기존 binding이 있으면null도 series identity에 포함해 비교하도록 조건만 보완했다. 수정 뒤 세 이벤트 모두 거절되며 전체 reference 행과 inbox가 불변이고 정상 후속 이벤트·record-template GET이 계속 성공한다.

정상 대조에는 더 높은 version의 v1 reference에 낮은 version의 검증된 TASK/SERIES binding을 복구하는 두 경우, binding 없는 v1 reference에 정상 v2 series metadata를 추가하는 경우, 기존 늦은v1/정상v2 연결 보존을 포함한다. 이 복구는 state/version을 낮추지 않으며 새 identity 검사 때문에 차단되지 않는다.

RED 명령:

```powershell
.\gradlew.bat :services:planner-service:test --tests '*TaskHttpIntegrationTest.duplicateSelectionCannot*' :services:activity-service:test --tests '*TemplateFoundationHttpTest.nullLinkCannot*' --max-workers=2 --console=plain
```

실제HTTP4개 실패 + 실제DB3개 실패, exit1을 확인한 뒤 제품 두 파일만 최소 수정했다.

GREEN 및 빌드 명령:

```powershell
.\gradlew.bat :services:planner-service:test --tests '*TaskHttpIntegrationTest' :services:activity-service:test --tests '*TemplateFoundationHttpTest.nullLinkCannot*' --tests '*TemplateFoundationHttpTest.verifiedBindingCanEnrich*' --tests '*TemplateFoundationHttpTest.unboundV1Reference*' --tests '*TemplateFoundationHttpTest.v2Projection*' :services:planner-service:assemble :services:activity-service:assemble --max-workers=2 --console=plain
```

결과는 exit0이다. XML에서 planner HTTP26개(기존22+신규4), activity DB7개(악성3+검증된 binding 복구2+unbound 복구1+기존v1/v2회귀1), failure/error/skipped 모두0을 확인했다. 두 서비스 main/migration 및 planner bootstrap JAR 빌드가 통과했다.

이번 라운드는 소비자의 실제 PostgreSQL 트랜잭션·inbox 롤백을 직접 검증했다. Kafka 전송·재시도/격리 구성과 원본·생성 계약은 수정하지 않아 이전 전체 Kafka16개나 생성 drift를 반복 실행하지 않았다. 이전 실증 결과는 위 해당 코드 시점의 기록으로 유지한다. 부모 ledger/vault·B2 문서·임시 runtime은 수정하지 않았다.
