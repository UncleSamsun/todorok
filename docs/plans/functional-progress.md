# 기능 구현 진행 기록

계획: `docs/superpowers/plans/2026-09-07-mvp-functional-implementation.md`
기준: develop `5eea3c16a309c1828483d8c0bd627d4585d4b145`.
작업 위치: `C:\workspace\todorok-worktrees\functional`.

## 현재 기준 — 사용자 흐름 중심

이 문서가 기능 완료 상태와 검증 증거의 단일 기준이다. 과거 보고서는 증거 원본으로만 보존하고, 이후 상태 보고는 이 문서에 모은다.

| 사용자 흐름 | 상태 | 검증 근거·다음 작업 |
| --- | --- | --- |
| 로그인·세션, 달력·일정·반복·메모 | 구현·관련 검증 완료 | 아래 작업01–06의 커밋·회귀·실제 브라우저 증거 |
| 기본 활동 기록·수정·취소·지난 기록·월 집계 | 구현·관련 검증 완료 | 작업07–08의 실제 서비스 왕복·브라우저·회귀 및 중요 리뷰 |
| 사용자 정의 공부 기록 | 백엔드 연결·검증 완료, UI 진행 예정 | 실제 값 저장·과거 버전/legacy 보존·Kafka APPLIED·다음 반복 회차 연결 완료. 다음은 관리→일정 선택→기록→과거 수정 UI 흐름 |
| 사용자 정의 자유 운동·클라이밍 기록 | 남음 | 공부 흐름에서 검증한 공통 부분을 재사용해 각 세트·라운드와 연결 |
| 운동 프로그램·실제 catalog | 남음 | 원래 작업10–11. 외부 자료의 이용 범위 확인도 유지 |
| 클라이밍 타이머·알림·PWA/설정 | 남음 | 원래 작업12–14. 기존 PWA 업데이트 문제 포함 |
| 최종 통합·운영 준비·실기기 검증 | 남음 | 원래 작업15–16의 필수 기준 유지. 원격 push·merge·배포는 별도 지시 없이 하지 않음 |

### 공부 기록 흐름

공부의 백엔드와 UI 흐름을 연결했다. 카테고리 관리 → 일정 선택 → 다섯 형식 기록 → 정의 변경·보관 → 원래 정의로 과거 수정·취소, HTTP → outbox/CDC/Kafka → APPLIED → 같은 binding의 다음 반복 회차까지 구현했다. **공부 사용자 기능은 완료했지만 작업 09 전체 완료는 아니다.** 자유 운동·클라이밍의 custom 값은 다음 사용자 흐름이다. 이 두 도메인의 기존 typed 세트·라운드는 유지하고 연결 custom 기록 경계는 아직 `TEMPLATE_RECORD_NOT_READY`다.

저장·보존 범위:

- 입력 `ActivityDetail`/`StudyDetail`과 응답 `ActivityDetailResponse`/`StudyDetailResponse`를 분리했다. 생성 시 서버 TaskReference에서 template identity를 정하고 current 행 잠금 아래 version을 비교한다. 모든 필드 정의를 `ActivityTemplateSnapshot`으로 고정하고 입력한 값만 scalar JSONB map에 저장한다.
- `V7__study_template_records.sql`이 기존 Activity를 LEGACY로 분류하며 원본 Study JSON·typed detail·기존 history를 수정하지 않는다. 신규 미연결 기록은 STANDARD, 공부 연결 기록은 TEMPLATE다. template identity/version/snapshot/detailFormat 변경은 DB trigger로 차단한다. correction 전에 전체 revision을 이력에 넣으며 legacy correction은 subject/duration 등 검증 가능한 컬럼만 갱신한다.
- 생성 잠금 순서는 owner/command advisory → 성공 command replay → TaskReference → template current 행이다. 관리 command는 management advisory → template → field identity advisory, 선택 승인은 binding advisory → target advisory → template 순서다. 관리·승인은 TaskReference/Activity를 잠그지 않고 projection은 template에 쓰기 잠금을 잡지 않는다. correction/void는 Activity 행만 잠그고 저장된 원래 snapshot을 사용하므로 역순 template 잠금이 없다. 저장 트랜잭션에 타 서비스 호출·DB join은 없다.
- 상세/목록/replay는 기존 단일 SQL statement에서 header·typed detail·snapshot·값을 함께 읽는다. legacy의 긴 소수는 일반 floating-point tree를 경유할 때 history에 반올림되는 실패를 재현해, 정확한 decimal tree와 원본 JSON node로 보존하도록 보완했다. 현재 JSONB는 원래부터 보존됐으며 손실 지점은 응답을 통한 새 revision history 저장이었다.

UI API 인계 — 원본은 `contracts/openapi/activity-v1.yaml`, 생성 모델은 `@todorok/api-client`의 `activity` namespace다.

| 흐름 | API와 필수 사용법 |
| --- | --- |
| 관리 | `TemplateApi.createTemplate`, `createTemplateVersion`, `archiveTemplate`, `listTemplates({domain:'STUDY',kind:'STUDY_CATEGORY'})`. 정의 version은 1부터, 관리 revision은 0부터다. 정의 저장은 Task 생성이 아니다. |
| 일정 선택 | 기존 planner `createTask`/`createSeries` 요청에 `commandId`와 `templateSelection:{templateId,expectedTemplateVersion}`을 함께 보낸다. 일정 응답의 `templateLink`는 선택 당시 version을 유지한다. |
| 기록 폼 | `TemplateApi.getTaskRecordTemplate({taskId})` → `{linked,templateLink?,template?}`. `template.currentVersion`의 `templateVersion,name,fields`를 사용한다. linked=false는 기본 typed 폼이고, projection 미도착은 `409 TASK_NOT_READY`다. |
| 생성 | `ActivityApi.createActivity({createActivityRequest})` → 201 `ActivityResponse`. 연결 공부에는 현재 폼의 `expectedTemplateVersion` 필수, 미연결에는 금지. templateId/snapshot/legacy JSON은 요청에 넣지 않는다. |
| 상세·수정 | `getEditableActivity(api,id)`로 원본 timestamp를 보관한다. 폼 정의는 응답 `templateSnapshot.fields`, 값은 `detail.study.fields`. `correctEditableActivity(api,id,{expectedVersion,performedAt,startedAt?,endedAt?,note?,detail})`는 Activity revision을 비교하며 expectedTemplateVersion을 받지 않는다. |
| 취소·동기화 | 기존 `voidEditableActivity`/`ActivityApi.voidActivity`, `syncState` PENDING/APPLIED/CONFLICT, 이전·새 수행 날짜/월 캐시 갱신 규칙을 그대로 사용한다. |

생성 요청 예시(공통 실제 시간·note는 선택):

```json
{
  "commandId": "10000000-0000-0000-0000-000000000001",
  "taskId": "10000000-0000-0000-0000-000000000002",
  "activityType": "STUDY",
  "completionStatus": "COMPLETED",
  "performedAt": "2026-09-07T10:00:00+09:00",
  "expectedTemplateVersion": 1,
  "detail": {"study": {"fields": [
    {"fieldId":"20000000-0000-0000-0000-000000000001","type":"NUMBER","numberValue":0},
    {"fieldId":"20000000-0000-0000-0000-000000000002","type":"TIME","timeSeconds":120},
    {"fieldId":"20000000-0000-0000-0000-000000000003","type":"SHORT_TEXT","textValue":"BFS"},
    {"fieldId":"20000000-0000-0000-0000-000000000004","type":"CHECK","checked":false},
    {"fieldId":"20000000-0000-0000-0000-000000000005","type":"MEMO","memoValue":"첫 줄\n둘째 줄"}
  ]}}
}
```

각 fieldId는 실제 선택 정의에서 가져온다. 각 원소는 fieldId/type/맞는 값 하나만 가지며 null·혼합 멤버·중복 ID·다른 정의 ID·unknown 속성·중복 JSON 키·숫자/문자열 coercion은 400이다. 빈 배열과 미입력 생략은 유효하고 0/false는 보존한다. TIME은 표시 unit과 무관하게 정수 초(0–9007199254740991), SHORT_TEXT는 120자, MEMO는 20,000자다. 공백뿐인 문자열은 미입력으로 정규화하고 내용이 있으면 공백·줄바꿈을 보존한다. 필드 개수 상한은 없고 전송 본문은 프록시/앱 모두 1 MiB, 초과는 413 `PAYLOAD_TOO_LARGE`, 압축은 415다.

응답은 공통 Activity 헤더와 함께 `detailFormat:'TEMPLATE'`, `detail.study.fields:FieldInput[]`, `templateSnapshot:{schemaVersion:1,templateId,templateVersion,name,domain:'STUDY',kind:'STUDY_CATEGORY',fields:FieldDefinition[]}`를 반환한다. snapshot의 모든 필드에는 `fieldId,name,type,unit?,position`이 있으며 미입력 항목도 남는다. TEMPLATE의 subject/durationMinutes는 기존 선택 typed 값으로 함께 사용할 수 있다. LEGACY에는 `legacyStudyPayload:{provenance:'UNVERIFIED_LEGACY',values?,snapshot?}`만 원문 읽기 영역으로 제공한다. 이 원문을 요청으로 재전송하거나 필드 정의로 추정하지 않는다. STANDARD/LEGACY 응답에는 templateSnapshot이 없다.

`409 TEMPLATE_VERSION_CONFLICT`에서는 원래 입력을 보존하고 최신 정의를 재조회한다. 성공했던 동일 command/payload는 정의 변경·보관 후에도 **현재 Activity revision**을 201로 다시 반환한다. 내용이 바뀐 command 재사용은 기존 `COMMAND_CONFLICT`다. correction은 원래 snapshot으로만 값을 검증한다. 기존 시간 전체교체 계약을 유지하므로, 미수정 시간은 `getEditableActivity`가 보관한 wire timestamp를 보내 마이크로초까지 보존하고 시간 쌍을 생략하면 구간을 삭제한다. 자유 TIME은 월 시간 합계에 더하지 않으며 실제 구간 우선, 없으면 기존 durationMinutes fallback을 유지한다.

실행 증거:

- `./gradlew.bat :services:activity-service:test --tests '*StudyTemplateRecordHttpTest' --no-daemon --no-configuration-cache`: 입력 계약 부재/무검증 쓰기 허용 RED 후 최초 HTTP 4개 GREEN. 이후 current template lock 양방향, GET/LIST/REPLAY 수정 경합 2개 focused GREEN.
- `./gradlew.bat :services:activity-service:test --tests '*record.*' --tests '*ActivityPersistenceIntegrationTest' --tests '*ActivityMigrationApplicationTest' :services:activity-service:assemble :services:planner-service:assemble --no-daemon --no-configuration-cache`: 영향 25개 중 16개 통과. V7 추가로 migration 개수 fixture를 8로 갱신했다. 병렬 재생성 중의 HTTP 404는 생성/컴파일과 테스트를 분리한 실행에서 제품 변경 없이 해소했다.
- `./gradlew.bat :services:activity-service:test --tests '*StudyTemplateRecordHttpTest' --tests '*ActivityPersistenceIntegrationTest.migratesOnlyActivitySchema' --tests '*ActivityPersistenceIntegrationTest.rerunningFlywayMakesNoChanges' :services:activity-service:assemble --no-daemon --no-configuration-cache`: 나머지 9개 통과. 합계 영향 25개를 확인했고 이미 통과한 B1 foundation/선택 검사는 반복하지 않았다.
- `./gradlew.bat :tests:messaging-integration:test --tests '*ActivityServiceRoundTripTest.templateBindingRecoversApprovalAndPlannerFailuresAndPropagatesSeriesThroughKafka' --no-daemon --no-configuration-cache`: 1개 통과. 기존 기록409/crafted 완료 fixture를 실제 값 저장·APPLIED·다음 반복 회차·보관 뒤 기록/수정으로 교체했다.
- `./gradlew.bat :tests:messaging-integration:test --tests '*ActivityServiceRoundTripTest.typedRoundTrip*' --tests '*ActivityServiceRoundTripTest.correction*' --tests '*ActivityServiceRoundTripTest.validatesOwners*' --tests '*ActivityServiceRoundTripTest.concurrentCommands*' --tests '*ActivityServiceRoundTripTest.pastRecords*' --no-daemon --no-configuration-cache`: 기존 세 도메인 typed 기록·시간·correction/void·history·경합·rollback·과거 반복 등 영향 8개 통과.
- `node --test --test-name-pattern='공부 기록' scripts/template-nginx.integration.test.mjs`: 실제 proxy의 413 HTML 실패를 공통 Problem JSON으로 수정 후 1개 통과. `node --test scripts/template-contract-roundtrip.test.mjs scripts/openapi-contract.test.mjs`와 TypeScript 신규 왕복 fixture 수정 후 focused 재실행으로 4개 모두 확인. 다섯 타입·TEMPLATE/LEGACY 응답 및 입력/응답 분리 직렬화를 확인했다.
- `pnpm run contracts:check`, `pnpm run build:packages`, `pnpm run build:web` 통과. 생성 Java는 실제 HTTP/서비스에서 사용하며 TypeScript 생성기는 왕복 테스트를 통과했다.
- 긴 소수 원문 보존 추가 fixture는 SQL `history.legacyStudyPayload.values = study_detail.values_json` 비교에서 RED였다. 최소 읽기 수정 뒤 `./gradlew.bat :services:activity-service:test --tests '*StudyTemplateRecordHttpTest.migrationAndLegacyCorrectionPreserveArbitraryJsonAndHistory' --tests '*StudyTemplateRecordHttpTest.fiveTypesRoundTripWithOriginalDefinitionAfterEditArchiveCorrectionAndVoid' :services:activity-service:assemble --no-daemon --no-configuration-cache`로 영향 HTTP 2개와 activity assemble 최종 통과를 확인했다.
- UI는 `study-template-ui.test.tsx`와 영향 `recurrence.test.tsx`의 최종 집중 8/8, 앞선 관리·기록·기존 도메인 영향 묶음 14/14, `pnpm --filter @todorok/web build`를 통과했다. 실제 브라우저에서 관리 저장 전후 등록 0개 유지 → 다섯 형식 카테고리 선택 반복 일정 → 일부 항목과 NUMBER 0/CHECK false 저장 → APPLIED → 이름·단위·순서 변경 → 과거 snapshot 이름·단위 유지 수정 → 보관 후 새 선택 제외 → 기존 다음 회차 기록과 390px/44px를 확인했다. 후속 짧은 브라우저 왕복에서 QuickAdd 제목·선택을 유지한 채 카테고리를 생성하고 돌아오는 흐름도 확인했다.
- UI 중요 검토에서 QuickAdd의 관리 왕복 진입점 누락과 관리 중 선택 항목이 사라질 때 stale selection이 남는 경계를 발견했다. 폼을 mount한 채 관리 화면을 열고 활성 목록 갱신 시 사라진 선택만 해제하도록 보완했으며, 일정 제목·날짜·반복 초안은 유지한다. 다른 중요 결함은 발견하지 않았고 비차단 목록 전체 pagination 개선은 후속 정리로 남긴다.

남은 기능은 자유 운동·클라이밍 custom 관계형 값 저장/수정 및 UI, 그리고 원래 작업10–16이다. 숫자 정밀도가 중요한 legacy 원문은 정확한 raw JSON 구간을 읽기 전용으로 표시하며 편집 가능한 새 입력으로 변환하지 않는다. 비차단 구조 정리(템플릿 목록 전체 pagination, 기존 미사용 detail read helper 등)는 이번 범위에 넣지 않았다.

### 실행 규칙 (2026-09-08)

- 사용자 흐름 단위로 구현 → 영향 테스트 → 리뷰한다. 기반·계약만 완료된 상태는 사용자 기능 완료가 아니다.
- 같은 코드에서 통과한 검사는 반복하지 않는다. 전체 검사는 주요 통합 시점과 최종 완료 전에 실행한다.
- 지적은 코드와 재현으로 확인한다. 오탐은 근거로 닫고 재리뷰는 원 지적과 수정으로 생긴 문제만 확인한다.
- 데이터 손실·보안·동시성·핵심 오류는 해결한다. 비차단 구조·미관 정리는 별도 목록으로 유지하며 범위를 늘리지 않는다.
- 조율 역할은 작업자의 동일 테스트·리뷰를 중복하지 않는다. 진행 보고는 완료 기능·다음 기능·실제 장애만 간결히 전달한다.
- 옵시디언은 결정·완료·중요 장애가 바뀔 때 갱신한다. 변경·커밋·데이터를 보존한다.

### 마지막 검증 결정

`241f9d9`에서 현재 중요 결함 2건을 보완했고 실제 HTTP26개·DB7개·두 서비스 assemble을 통과했다. 수정 범위 재리뷰에서 두 건 모두 해결됐고 새 중요 문제가 없음을 확인했다. 같은 검사는 반복하지 않고 사용자 정의 공부 기록 흐름으로 진행한다.

## 과거 진행 이력·증거

- 01 공통 오류·trace ID: 완료. `a198c7d`→`4aef39c`→`548049e`→`8a15c84`, 보완 3차 리뷰 통과. HTTP 14개 전체 통과 후 Callable 강화 focused 통과, API client 5개·tsc 통과, planner 연결 smoke·activity/notification compile 통과. 검증 보고는 `docs/plans/task-01-report.md`.
- 01 리뷰 이력: async dispatch → Callable worker MDC 전파·정리, common-v1 추가 속성·URI ASCII 검증 순서로 보완해 모두 해소했다. 원본 계약의 title은 빈 문자열을 허용하므로 임의 nonempty 제약을 추가하지 않았다.
- 02 인증 backend: 구현·리뷰·통합 검증 완료. `d00d84d`·`3c7a77b` 서비스30/Nginx429/공통오류14/client build·보안 리뷰 통과. 웹 TS5097 수정 `4ba0120` build:web/API5/리뷰 통과. Compose `51301` exit0: 새 이미지·fresh DB·health·Connect·marker 보존·동일 volume 재기동·정리 통과. 커버리지 비율 계측은 최종 gate에 남아 있다.
- 03 로그인·session·routing: 완료. `63e7591` 구현·실제 브라우저 로그인/갱신/두탭logout/재접속/token storage 비어있음 검증. `4a3e35a` late refresh 경쟁 보완 후 웹20/build·리뷰 통과. 브라우저 session54304 exit0, 자원정리 완료.
- 03 리뷰1: Web Locks 미지원 환경의 늦은 refresh 성공/실패 race를 generation 재확인과 두탭 결정론적 테스트로 해결했다.
- 04 달력·Task·오늘 화면: 완료. `fbfe2cf` planner24·Task최종5·웹22+진행률2·build/drift/package·실제브라우저CRUD/16view조합 통과. 편집경합 보완 `c4e5772` 관련9개/build·리뷰 통과. 보고 `docs/plans/task-04-report.md`.
- 04 리뷰1: 같은 Task 재조회에서 초안·version이 섞이는 경합은 snapshot고정·sameTask재클릭방지와409초안보존/닫기재열기 테스트로 해결했다.
- 05 반복·이월·skip: `f9c5629` 구현·`40e2bb2` 보고. HTTP14/정책6/event16/web27·정책분기27+24 모두통과, 실제브라우저47377 exit0. 독립리뷰 중. canonical생성물 EOF빈줄16건은 생성단계 정규화로 보완 중. 06–16 미착수.
- 05 리뷰1: range/detail 수동refetch가 ready=false를 우회하는 경로 보완 필요. 이월대기/실패에서 retry버튼/handler를 guard하고 cached queryerror 경계를 테스트한다. 반복·잠금·상태 핵심 검토는 통과했다.
- 05 최종: 완료. `4e293a8` 생성EOF정규화·실제재생성검증, `78f84ec` 이월중수동조회guard·3개회귀/build·보완리뷰 통과.
- 06 메모: `78f84ec` 기준 구현 착수. 07–16은 미착수이며 전체 목표 유지.
- 06 구현 `887a825`: PostgreSQLHTTP17·웹전체39·최종메모12·build/contracts/drift 통과. 실제메모/충돌/오프라인/다음회차와 별도 실제pointer·390/1440화면 검증 후 중요리뷰 중이다.
- 06 최종: 리뷰 통과로 완료. 저장·초안·세션경계에 중요결함 없음.
- 07 Activity: 서버연동 단계 착수(`887a825` 기준). 서버 계약/DB/Kafka를 확정 후 기록UI를 별도 하위 단계로 연결한다. 전체07완료는 실제브라우저까지 포함한다. 08–16 미착수.
- 07 서버 중간: 유형별detail·command멱등·sync상태·실제입력시간 계약과 ActivityV3/plannerV7 구현. 실제 HTTP/CDC/Kafka 왕복 테스트 session45094 실행 중(완료판정 아님). runtime Compose messaging 활성화, test/migration 격리 설정 반영.
- 07 검증 후속: 초기 compile/fixture Origin 오류 뒤 실제 Boot4 Kafka자동설정 누락을 발견해 starter와 retry/DLT listener factory를 연결했다. 현재 재실행23981 진행 중이며 앞선 초기화 실패를 동작테스트 성공으로 계산하지 않는다.
- 07 서버 `aa65744`: 실제왕복8개 및 typed추가1개·plannerHTTP17·persistence8·security2·event17·migration2·bootstrap1 통과. 중요리뷰 중. UI단계는 아직미착수이며 전체07완료 아님.
- 07 서버리뷰1: 두회차완료→첫Activity취소→재기록에서 종료된후속회차가중복생성될수있는경계 발견. 기존발생일(삭제포함)을재사용하지않고최신보존발생일이후로계산·DB발생일unique제약·실제재완료회귀로보완한다. 기존데이터삭제로해결하지않는다.
- 07 서버최종: `88fa718` maxcursor/V8unique 보완, DBHTTP2·실제Kafka1·재리뷰 통과. 서버단계 완료.
- 07 UI: `88fa718` 기준 기록폼·동기화표시·실제브라우저 연결 착수. 전체07완료는 이 단계 검증 후다.
- 07 UI 검증 준비: 최신 planner/activity runtime+migration jar가 포함된 Docker이미지를 session16737에서 빌드 중이다. UI담당과 중복빌드하지 않도록 분리했다. 완료시 imageID를 기록해 실제브라우저 fixture에 전달한다.
- 07 UI 서버이미지 준비완료: 16737 exit0. planner `sha256:0762771324927cd737dd04fcf02fdd4f41f687157fb67c22c57d5c73ab864d65`, activity `sha256:98b3e1da3c72a72586b9a4d5c4f3b8c67fde43dc7df567907995a872d6424c82`. `88fa718` 서버소스의 V8/V3와 migration/bootstrap 포함. UI담당에전달.
- 07 UI 전용환경: project `todorok-task07-ui`, URL `http://localhost:5187`. `.local/task07-runtime/runtime.mjs up` session39236 exit0; `probe.mjs` 실제Task→Activity→Kafka→plannerCOMPLETED/APPLIED 확인 exit0. UI검증을 위해 유지 중이며 부모가 마지막에 down으로 전용자원만 정리한다. 비밀값은 local파일에만 있음.
- 07 UI `34f36d5`: 웹43/build·실제3유형저장/APPLIED/기존행완료/취소DB0·390px검증 통과. StrictMode effect replay·이탈후늦은응답·자동APPLIED갱신 전용회귀는 미완료여서 별도 집중보강 중이다. 프로덕션브라우저를 StrictMode 개발재실행 증거로 간주하지 않는다.
- 07 UI 집중검증: Task queryparam 구독 누락과 자동APPLIED 갱신중복/기록전환 경계를 재현해 보완 중이다. 전용runtime down22444 exit0, 컨테이너/volume/network와 임시키·credential4파일 제거. screenshot/harness 보존, liveAPI없이 회귀 진행.
- 07 UI `4bbf1de`: queryparam구독·동기화갱신/전환·StrictMode 보완, 집중17/build통과. 최종리뷰3건(HTTP5xx불확실요청보존, 이탈후세션내초안/요청복원, 부분시간입력거부)을 집중보완 중이며 전체07미완료.
- 07 최종완료: `77bf83b` 세션초안/불확실HTTP/부분시간 보완·집중30/build/diff·재리뷰 통과. 서버실제왕복과 UI실제3유형브라우저 증거를 합쳐 전체07 완료.
- 08 기록변경 서버단계 착수: correction/void/지난기록 반영 후 월별조회·UI를 후속하위단계로 연결한다. 09–16 미착수.
- 08 서버 `65aa6b8`: 실제Kafka6+시간보강1/contracts19/migration-security4/drift/build통과. 리뷰에서동시PATCH중별도header/detail SQL조회가다른revision을섞을수있는P2를발견해 일관snapshot조회·list/replay 회귀보완 중이다.
- 08 서버완료: `a3a58ff` 단일SQLsnapshot·PGbarrier9조합·samecommand8경합·validator3개·재리뷰 통과. 월별조회API/기록관리UI 하위단계 착수. 전체08미완료.
- 08 UI환경: backendbuild85961 exit0, planner `78e7f4ca90890651d0c80c49f18c2bb7e71e944e24c621044216b4ebd2a80166`, activity `f2a41f32bd694d81347dd076af285135ecd8f7baff5cf30129d31af9877633c9`. runtime8767 exit0 project`todorok-task08-ui` localhost5188. probe40915 exit0: 실제badmonth400/correction+void APPLIED/month합계확인. UI브라우저 검증용유지중.
- 08 UI `53c031a`: 월API DB/Clock/실제HTTP·신규UI3개/build/drift통과. 기존StrictMode조회대기와 실제과거Task선택timeout 미해결로 집중인계. 별도허용된브라우저CLI0.36 정상실행·부모로그인PASS확인(차단된실행파일과구분). 전체08미완료.
- 08 실제UI 후속: 과거Task 선택·저장·APPLIED는 native 조작으로 확인했다. 저장 후 이미 방문했던 도메인의 월요약/목록이25초 동안 이전값을 유지하는 실제 캐시갱신 누락을 발견했다. 기존 생성/APPLIED 경로가 calendar만 무효화하고 새 Activity조회키를 빠뜨린 원인으로 집중회귀·수정 중이다. StrictMode조회는1,111ms에성공하여 초기render부하와 구분했고 timeout상향 없이 mount완료대기를검증한다.
- 08 검증계속: async act 초기mount와단일worker로 기존69/69통과, 월요약stale 신규회귀RED확인 후 생성/APPLIED경로수정 중이다. 월경계·윤년·세유형·조회불변성 직접DB증거의 누락을 `task-08-summary-boundary-brief.md`에 명시해 UI수정이후 순차보강한다. 현재08완료아님.
- 08 UI최신검증: 수정후70/70·build통과와실제과거저장/APPLIED→수정45분→합계75분→VOIDED→합계30분·탭간월공유를확인했다. 부모모바일캡처검토에서390px추가버튼글자줄바꿈을발견해배치보완요청했다. 빌드전환중단발성blank원인도증거확인중이며 최종리뷰/DB누락보강전완료처리하지않는다.
- 08 UI보완확정 `3e8fe6d`:70/70/build/실제저장수정취소·390px버튼행보완통과,부모최종캡처확인. `a3a58ff..3e8fe6d` frozen diff중요리뷰와별도DB경계검증보강착수. PWA전환blank는아래14필수항목에별도추적한다.
- 08 중요리뷰: 요구미충족/수정필요. 편집version혼합·수정/void동기화미연결·완료Task신규폼·400옛snapshot재전송·불확실응답시간비교누락·등록캐시미갱신·메모수정시간초손실7건,재시도44px1건을 `task-08-summary-review.md`에기록했다. DB검증작업완료후UI담당수정라운드1로순차전달한다. 서버SQL중요결함은발견되지않았다.
- 08 DB경계 `9424f97`: 실제PostgreSQL추가5개exit0·XML5/0/0/0부모확인. 윤년/UTC동일시각·다중세트·두유형상태/owner·소수초·조회전체행불변통과. 보강test읽기전용리뷰착수. UI라운드1은 `task-08-summary-fix-brief.md`로직전담당에게전달했으며유일한구현작업이다.
- 08 DB보강리뷰:실행은통과했지만서울/UTC경계동일시간상쇄와Activity없는Taskreference누락2건으로검증미충족. `task-08-summary-boundary-review.md`에기록하고현재UI작업후순차보완한다. 테스트가녹색인것을요구증명과동일시하지않는다.
- 08 UI라운드1중간:집중15개중완료Task/등록갱신포함12개통과. 정밀timestamp본문이generated경계에서이중JSON직렬화되는3실패를확인해최종middleware단계본문교체로보완중이다. tsc통과후집중12개10907실행중. 전체라운드완료아님.
- 08 UI라운드1검증:10907 correction12/12,31287 추가6/6(sub-ms/시간직접편집/불확실초안재진입/lateGET/void충돌)통과보고. api-client build/tsc통과. 최종웹전체65495실행중이고실제완료Task정확ID/등록증분/수정APPLIED/실패재시도44px브라우저검증예정이다.
- 08 UI라운드1후속:65495웹91/91(14files)/build통과;8292실제등록5→6·완료Task두경로exactID·41분수정APPLIED·요약요청abort/44px재시도복구PASS. 부모harness/캡처확인. 저장전시작GET의저장후도착상태역행경계를46653으로추가검증중이며결과후커밋/재리뷰한다.
- 08 UI라운드1최종보강:46653은저장v5/PENDING후늦은GETv4의캐시역행을RED로확인했다. 낮은revision/동일revisionPENDING역행보호후48445 correction19+lifecycle23=42/42통과. 이전91/91과코드시점을구분한다. 최종build75749·native짧은확인후보고/커밋예정.
- 08 UI라운드1 `e88b885`:최종build/native최신entry읽기확인후커밋. 7개Important+44px재리뷰착수. DB검증라운드1(서울경계상쇄/unlinkedreference2건)은유일한구현worker로순차착수했다.
- 08 UI재리뷰통과:Important7+44px모두해결,새중요결함없음. 전용runtime down81120 exit0,컨테이너/volume/network0확인·소유browser3session종료·임시credential/key4파일삭제. harness/캡처는보존. DBtest2건보강은계속진행중이므로전체08완료아님.
- 08 DB라운드1 `dd544ea`:focused2/2실제PG·XML2/0/0/0부모확인. 잘못된UTC집계720초와정상서울300초구별,owner전체reference/orphan상태변경탐지보강. scoped재리뷰착수했으며결과전전체08완료보류.
- 08 최종완료:서버correction/snapshot/Kafka검증과UI `e88b885` 중요재리뷰통과,DB `dd544ea` 두검증재리뷰통과를합쳐판정. 실제과거저장/수정/void·월요약·완료상세·등록갱신·모바일검증증거는각보고서에있다. PWA배포전환은14필수항목으로유지. 09는승인된템플릿요구의계약/버전/일정연결세부설계부터착수한다. 10–16미완료.
- 09 설계 `d2422ba`: PRD12.3·상세설계·09A 기반 인계를 확정했다. 정의 관리→서비스 연결→UI 세 gate로 분리하고 09A 구현을 착수했다. 전체09는 미완료다.
- 09A 첫 RED: 원본 관리 계약·생성 타입과 실제 HTTP 테스트를 추가했다. POST templates의 기대 201 대비 실제 404로 미구현을 확인한 뒤 migration/service/controller 구현 중이다. 기존 테스트 entity 검증 간섭은 별도 fixture 설정에서 분리했으며 성공 증거로 계산하지 않는다.
- 09A 첫 GREEN: 실제 PostgreSQL migration·JWT HTTP 생성/조회 1/1 통과, 부모 XML1/0/0/0 확인. 다섯 형식·TIME 기본초·version1/revision0·Task/outbox 미생성·동일 GET 범위만 증명했다. strict body·rollback·owner/pagination/archive/경쟁·DB 불변성·최종 gate는 남아 있다.
- 09A 중간검증:73582 실제PG/JWT HTTP7시나리오 통과, 부모XML7/0/0/0확인. strict본문/rollback/owner·목록·보관·경쟁·불변성 포함. 대량필드/동시command재전달·최종activity suite·생성drift/build 및프록시413증거를남은gate로확인한다.
- 09A 추가검증:67085 보강3/3·부모XML3/0/0/0확인(512필드·동일command경합·record/history/command불변). 프록시exact1MiB는502간접증거대신실제stub의200/수신1048576bytes로검증하도록보강중이며초과chunked413도유지한다.
- 09A 프록시:83445 static+실제Nginx4/4통과보고. exact1MiB=200/수신1048576bytes,chunked+1=공통413JSON확인. TS왕복optional unit의undefined기대값조정후계약/drift·activity전체suite·package build를최종gate로진행중이다.
- 09A 최종gate중간:95960 activity전체28/28(템플릿8포함),부모7개XML합계28/0/0/0확인. 계약56075 6/6·drift22185일치·client5/build통과보고. 마지막TIME단위null DB제약강화후해당focused회귀와assemble/report/commit이남아있다.
- 09A 구현 `314d0d8`: 마지막TIME제약focused2/2(부모XML확인)·assemble통과후담당실행이사용량제한으로중단됐다. 부모가stage범위/공백검사를확인하고trailingwhitespace1곳정리후코드61파일을커밋해보존했다. 중요리뷰는생성소스포함분할diff로요청했으며완료판정은보류한다.
- 09A 리뷰:Important1건(fields필수누락이generated빈배열기본값으로처리돼새정의를비움)을확인했다. `task-09-template-foundation-review.md` 기준으로누락/null거부·명시[]허용·HTTP실패후상태불변을수정라운드1로전달했다. 다른중요결함은없으며전체09A완료는보류한다.
- 09A 리뷰반증:제품무수정실제HTTP13/13(부모XML확인)에서누락/null/비배열은이미거절됐다. 실제누락응답은VALIDATION_FAILED/fields NOT_NULL,주입mapper결과도fields=null로확인했다. 초기[]소스만으로판단한Important는오탐이므로제품수정없이정확한회귀assert와증거로재판정한다.
- 09A 반증회귀 `9fe9fe6`:최종15/15(부모XML15/0/0/0)·assemble통과. 제품변경없는test/report2파일을커밋하고scoped재판정요청했다. 기존Important는누락필드400의정확한오류/mapper/DB불변증거로평가하며최종판정전완료보류.
- 09A 완료:독립재리뷰가fields지적을오탐으로철회하고요구·품질승인. 원래28/28·proxy4/계약6/drift/client검증과최종15회귀/assemble증거를합쳐기반관리만완료판정한다. 전체09는B1선택/일정연결→B2기록/legacy→C UI가남는다. B1은실제context경로·서비스인증/외부차단을명시한별도brief로진행한다.
- 09B1 착수:서비스전용private/public key파일과base URL 설정을분리한다. 미설정내부승인은fail-closed,일반서비스/bootstrap시작은유지한다. 제안포트8080은실제nginx/compose/application의8082와달라부모가대조해8082로수정요청했다. 내부계약/생성·DB/HTTP회귀부터진행한다.
- 09B1 첫RED:실제PG/HTTP신규2개에서승인201대비401·사용자JWT내부401대비404로미연결을확인했다. internal계약생성ref/의존성compile보완과서비스JWT·binding구현중이며backend는아직frozen아니다. compile실패는동작검증통과로계산하지않는다.
- 09B1 중간:내부생성의존성보완후승인/JWT2개GREEN. planner는연결command누락400대비201로RED확인후command/링크구현중이다. 복수Gradletask의--tests범위로activity43개가실행돼41통과/마이그레이션기대치2실패였으며전체통과로표시하지않는다. projectionv2/실제두서비스·Kafka검증이남는다.
- 09B1 부모실제왕복RED83921:2개중outage/승인응답유실복구통과,최초Task의template_binding_id projection대기에서실패(90→626행),XML2/1/0/0·exit1·3m16s. 승인후planner실패/동시재전달까지통과한뒤v2미발행경계를재현했다. 실행종료후freeze해제해발행/소비와selection제거command우회회귀를담당에인계했다.
- 09B1 왕복후속44212: v2발행추가후에도최초Taskprojection.await90→677에서timeout,두fixture중outage/응답유실만통과(XML2/1/0/0·exit1·3m3s). 성공으로처리하지않고freeze해제후consumer/schema/발행경계원인확인을담당에인계했다. selection제거우회는별도focused47575통과보고가있다.
- 09B1 진단19916:outbox1건/version2/bindingId와동일eventIdKafka수신·Connect전송정상확인후projectionawait96실패. CDC까지는정상이며수신payload/소비검증경계로범위를좁혔다. JSON동등비교·직접예외확인으로원인확정중이다.
- 09B1 원인75703:같은eventId의outbox seriesId:null이Kafka에서키누락으로변환됨을확인했다. Debezium공식문서의기본null ignore와일치한다. 소비schema를약화하지않고planner source에만optional_bytes를적용한다. activity와planner각원본topic명시predicate로재변환을피하며v1Activity왕복회귀를포함한다.
- 09B1 Ruling: planner outbox만 null 보존 변환으로 분리한다 — v2 명시 null 계약을 지키면서 기존 Activity v1의 생략 정책을 유지하기 위함 — connector 설정 변경·검증·운영 업데이트 절차가 추가로 필요하다.
- 09B1 부모전체왕복94568:16개중15통과/1실패(XML16/1/0/0·3m45s). 신규binding/장애복구2개모두통과해null보존수정효과와기존v1흐름을확인했다. 유일실패는기존rollback검사의global climbing_round<=1이타테스트행2개를세는격리결함이다. 원시나리오에서해당owner의climbing은전부rollback이므로owner범위0/불변검증으로보완후focused재실행한다.
- 09B1 suite55530:planner46통과,activity47중46통과/기존정의생성global task_reference0가타owner projection행을세는1실패보고. owner범위미생성검증으로격리보완후94026focused/libs/build진행중. 승인/archive Lock·JWT행렬과Nginx실제경로·Node계약9/clientbuild통과보고. 부모rollbackfocused는동시heavy를피해대기한다.
- 09B1 후속94026:activity격리회귀1·planner check/coverage·event/messaging/web libs·main/migration/bootstrap assemble통과보고. 부모56424rollback단일왕복PASS(exit0/2m3s·XML1/0/0/0직접확인)로기존16개실행의유일격리실패를해소했다. freeze해제후drift/키/등록script·보고/commit을마무리한다.
- 09B1 파일키Compose smoke준비:부모전용projecttodorok-task09b-smoke/localhost5189와독립user/service키2쌍을.local에준비(.dockerignore제외확인). imagebuild91059는JDKbuilder에Node가없어새generateEventV2Java에서실패했다. 기동전빌드의존성누락을발견해담당에게수정인계했으며생성skip으로우회하지않는다. 아직up/probe미실행.
- 09B1 파일키실제기동:builder-onlyNode24보완후2869 3서비스/connect-init build exit0,69243 up/health/bootstrap exit0. localhost5189에서파일키승인·Task/replay·Kafka projection·archive·기록준비409흐름통과. 유효service JWT201/aud누락401도확인해추측null수정은하지않는다. 공개expectedTemplateVersion의소수1.5/문자열1이각201로수락돼계약위반을확정했고실제probe exit1. 담당에CreateTask/Series입력경계·부분저장없는400회귀를인계,전용runtime유지중이다.
- 09B1 입력보완:97573 영향HTTP22/0/0/0·assemble통과보고후부모60677 plannerimage재빌드성공. 실제probe에서Task/Series소수·문자열각400,정수생성/replay·파일키승인/Kafka/archive/recordguard모두통과했다. helper의일회성connect-init --wait오인을분리하고47985 up exit0로재기동확인. 82560 down exit0·전용컨테이너/volume/network0·임시키/credential6파일삭제확인. 스크립트는보존하고보고/commit/중요리뷰로진행한다.
- 09B1 구현ca9e0de·보고명령정정8ed2d4a:119개본인파일커밋/부모ledger제외확인. ce1cfb1..8ed2d4a의원본·제품·테스트·생성소스를5개패키지로분리해독립중요리뷰착수. B1완료판정은리뷰후이며B2/C는계속남아있다.
- 09B1 리뷰진행:planner 중복templateSelection키(앞소수/뒤null)의tree/DTO해석차이로사전검증우회가가능함을읽기전용probe로확인했다. 내부activity의숫자coercion은기존strict설정으로배제했다. projection target/series 불변경계와함께최종지적정리중이다. B2기록값/legacy 인계문서는준비했으나B1열린중요지적전에는실행하지않는다.
- 09B1 리뷰최종Important2건:중복키정수검사우회와TASK binding/null series에잘못된v2seriesId를채우는identity훼손을확정했다. `task-09-template-binding-review.md` 기준으로HTTP/DB회귀와최소수정을원담당라운드1에전달했다. 전체B1완료판정보류.
- 09B1 라운드1 RED4772:HTTP중복키4개400대비503(내부처리진입),DBidentity3개오염허용을재현했다. 생성advice전용중복키검사와기존binding의null포함series비교최소수정후23888 영향HTTP26/DB7·두서비스assemble검증중이다. 전역mapper/생성물/schema변경없음.
- 09 Ruling: Task/series identity는 유지하고 새 기록 최초 저장 시 현재 정의 버전을 고정한다 — PRD의 이후 기록 적용과 과거 수정 보존을 함께 지키기 위함 — 다르게 요구되면 버전 선택 UI와 계약 재작업이 필요하다.
- 09 Ruling: 보관은 신규 선택만 막고 기존 승인된 일정·반복은 계속 사용한다 — 일정 자체의 보관과 템플릿 보관을 분리하기 위함 — 잘못 해석했다면 반복 중단 정책과 사용자 안내를 수정해야 한다.
- 09 Ruling: 서비스 선택 승인의 잔존은 허용하되 대상·소유자·command에 고정한다 — 분산 원자성을 가장하지 않고 실패 재시도를 보존하기 위함 — 향후 안전한 정리 정책을 따로 마련해야 한다.
- 09 Ruling: 기존 자유 JSON은 보존 전용 읽기와 서버 보존 correction으로 이행한다 — 알 수 없는 필드 의미를 추측하지 않기 위함 — 기존 임의 자료의 자유 편집이 필요하면 별도 전환 UI가 필요하다.
- 03 실제 브라우저에서 native fetch의 잘못된 receiver로 Illegal invocation이 발생해 서버 요청이 없음을 재현했다. local 함수 호출로 수정하고 receiver-sensitive 회귀 테스트·build 통과 후 실제 브라우저를 재검증 중이다. 단위 테스트 통과를 실제 로그인 성공으로 대체하지 않는다.
- 새 worktree의 event-contracts Gradle gate 성공(캐시 재사용). 작업 01 runtime 테스트 결과는 위 완료 증거를 따른다.
- PRD AC-3를 §10.4의 반복 조건표와 일치시키는 문서 정정을 반영했다. 월말/집계 신규 규칙과 구분한다.

## 선행 검토

| 작업 | 자체 요구 정합성 | 공유 인터페이스·후속 |
|---|---|---|
| 01 | 보안 filter는 02에서 연결 | ProblemResponseFactory → 02 |
| 02 | JWT·cookie·rotation | session 응답 → 03 |
| 03 | 메모리 token·query | principal·fetch → 04 |
| 04 | 일반 CRUD·기록형 complete 금지 | Task event → 05·07 |
| 05 | 월말 정책 제안 문서화 필요 | series → 08 |
| 06 | debounce·version 충돌 | 날짜 detail·Activity 메모 → 07 |
| 07 | eventual consistency 명시 | 완료·partial → 08 |
| 08 | 집계 제안 문서화 필요 | revision·summary → 09·10 |
| 09 | immutable template | detail·snapshot → 10·12 |
| 10 | 일반 반복과 분리 | session 요청 → planner |
| 11 | 외부 데이터 이용 범위 필요 | importer → 10 |
| 12 | timestamp·부분 기록 | Activity 저장 → 07 |
| 13 | 전달 exactly-once 보장 불가 명시 | Task events·설정 → 14 |
| 14 | offline queue 제외 | routing·session 재사용 |
| 15 | 비용·배포 별도 권한 확인 | existing migration·Compose |
| 16 | 실기기·7일 관찰 실제 증거 필요 | 전체 기능 gate |

현재 결론: 외부 자료·실기기·운영 권한이 필요한 후반 검증은 실제 확보 상태로 판정하며, 로컬 기능 구현은 계속한다. 공개 endpoint·집계 의미를 바꾸는 작업은 PRD와 계약을 먼저 갱신한다.

## 후속 필수 검증

- 14 PWA/배포 업데이트: 작업08 frontend교체에서 기존 열린브라우저의 옛lazy chunk요청이 SPA fallback HTML(200/736bytes)을받아빈화면이발생했다. nginx12:12:23Z요청은 `DomainPage-DfclhuWz.js`/`StudyPage-DE_Ha1XF.js`, 직전새SW는새chunk를precache했다. 새로고침후새index와실제전체흐름은정상이다. 정적asset없는경로의정확한404와oldchunk/lazyimport실패복구, SW업데이트중열린페이지회귀를작업14에서검증한다. 새로고침복구를자동복구완료로간주하지않는다.
