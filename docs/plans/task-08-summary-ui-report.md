# 작업08 월별 조회와 기록 관리 UI 보고

## 중요 리뷰 수정 라운드 1 (2026-09-07)

`task-08-summary-review.md` Important 1–7 및 모바일 재시도 44px를 보완했다. DB 경계 보강 커밋과 부모 문서는 보존했으며 API 계약/서버 변경은 없다.

| 리뷰 항목 | 수정과 이름 있는 회귀 |
| --- | --- |
| 1. 편집 v4와 background v5 | `CorrectionDrafts`에 원본 revision/시간/필드/원 요청을 같은 인증세션 snapshot으로 유지한다. `keeps v4 edit baseline when a background v5 arrives`가 expectedVersion5로 RED 후4로 GREEN이며409에서 초안을 유지한다. 성공 또는 사용자의 명시적 초안 폐기/재열기만 기준을 바꾼다. |
| 2. PATCH/void 동기화 | 생성 반환과 편집이 `useAppliedActivity`를 공유한다. PENDING 폴링, CONFLICT 및 수동 확인, generation/activityId/revision당 한 번의 APPLIED 갱신을 적용했다. 모든 planner day/range와 월 등록, owner Activity 목록/월 요약을 갱신해 이전/새 날짜·월을 포함한다. `observes PATCH/void PENDING through APPLIED…`, `shows PATCH/void CONFLICT…`, `ignores … after menu/logout` 회귀가 대응한다. |
| 3. 완료 Task 상세 | Today와 지난 기록, 오래된 taskId 직접 진입 모두 `CompletedTaskRecord`로 기존 목록의 cursor를 끝까지 조회한다. 날짜를 제한하지 않고 taskId/type/COMPLETED를 일치시킨다. abort/generation은 기존 SessionClient/query 경계를 사용한다. 완료 기록이 아직 없으면 재조회 상태를 표시하며 POST 폼을 열지 않는다. `opens the exact completed activity beyond the first page from today/past without creating`가 두 경로와2페이지/VOIDED 제외를 검증한다. |
| 4. 확정400 | 알려진 validation400/422이고 retryable이 true가 아니면 원 요청 잠금을 해제하고 같은 원본 revision 위에서 새 입력을 구성한다.409는 초안과 원 요청을 보존한다. `builds corrected input after a confirmed validation rejection`가 수정한 입력을 두 번째 요청에서 확인한다. |
| 5. 불확실 응답 | 네트워크/5xx는 원 요청·초안을 보존하고 결과 확인/동일 요청 재전송을 명시한다. 자동 결과 인정은 정확히 expectedVersion+1이고 task/type/status/메모/detail/수행·시작·종료 instant가 모두 같을 때만 허용한다. `does not accept another writer’s time/status/revision…`, `retains an uncertain edit across navigation and accepts only the exact next revision`이 잘못된 성공 처리 방지와정확한 성공 회복을 검증한다. |
| 6. planner 등록 갱신 | 등록 query key를 `calendar/summary/owner/month`로 옮겨 기존 생성/수정/삭제/반복/이월의 calendar 무효화 책임에 포함했다. `refreshes visited registration totals after planner creation and deletion`에서 방문한 도메인0→1→0을 확인했다. |
| 7. 초·소수초 | api-client의 `editable-activity.ts` 경계가 generated typed model과원본 wire timestamp를 함께 보존한다. Generated API의 pre-middleware에서 최종 JSON을 한 번 직렬화해 Date의 ms 절삭을 피한다. 메모만 수정하면 수행/시작/종료 원문을 그대로 보내며 직접 바꾼 시간만 재구성한다. `preserves exact seconds and fractional timestamps…`, `preserves a positive sub-millisecond interval…`, `applies an explicitly edited end minute…`, `rejects a partial explicit time edit…`가 대응한다. 실제 fetch body를 한 번 JSON.parse하여 object/정확한 원문을 검증하므로 이중 직렬화도 검출한다. |
| Minor 44px | 요약 재시도 min-height44px. 실제 브라우저의 요약 요청을 abort하여 오류 상태를 만들고 높이44px 및재시도 복구를 확인했다. 응답/DOM을 가짜로 주입하지 않았다. |

### 라운드 검증 결과와 코드 시점

- RED: correction 첫12개 중9실패, 별도 v4 background flush 및 planner 경로3개 모두실패를 원인별 확인했다. 초·소수초 절삭/400 snapshot 재사용/PENDING 미관찰/늦은 응답 부작용, 완료 Task 신규 폼 및등록 stale를 재현했다.
- GREEN: 핵심 correction12/12, 추가시간/재진입/지연GET/void CONFLICT6/6 및planner3/3 확인 후 **전체 웹91/91(14 files,107.33s)**을 한 번 실행했다. `pnpm --filter @todorok/api-client build`, `pnpm --filter @todorok/web build` 성공.
- 전체91개 이후 마지막 경합 점검에서 **저장 전 시작한 GET v4가 PATCH v5/PENDING 후 도착해 캐시를 v4/APPLIED로 되돌리는 결함**을 추가 재현했다. `does not let a pre-correction GET replace the saved pending revision`가 version4≠5로 RED였다. 상세 query의 structuralSharing에서 낮은 revision과같은 revision의 terminal→PENDING 역행을 거부하도록 최소 보완했다. 이 최종 보완 후 **correction19+lifecycle23 =42/42(45.57s)**, 최종 web build/TypeScript/diff 검사를 확인했다. 91/91은 이 마지막 단조 revision 보호 전 코드의 증거이며 전체 suite를 반복했다고 주장하지 않는다.
- 명령: `pnpm --filter @todorok/web test`; 후속 `pnpm --filter @todorok/web exec vitest run src/features/activity/correction-review.test.tsx src/features/activity/lifecycle.test.tsx`; 최종 `pnpm --filter @todorok/web build`, `git diff --check`. 계약/생성물 수정이 없으므로 drift 재생성 및backend rebuild는 필요하지 않았다.

### 실제 브라우저

- `.local/task08-runtime/review-ui.mjs`의 native agent-browser0.36.0 실제 로그인 세션에서 공부 도메인 방문→planner Task 생성→등록 **5개→6개** 갱신을 확인했다.
- 기록 저장 후 Today의 완료 Task를 클릭해 Activity **90f2540c-9558-45b3-9a57-87282bd4bcc0**의 기존 수정 화면을 열었다.41분/수정 메모로 저장하고 **일정 반영 완료**를 확인했다. 지난 기록 패널에서 같은 완료 Task를 클릭해 동일 Activity ID/수정 메모를 다시 확인했다. PostgreSQL readback은 **COMPLETED, revision1, APPLIED**였다.
- 390px에서 월 요약 endpoint만 network abort하여 개별 오류 표시와재시도 버튼 **44px**, overflow없음을 확인했다. route를 제거하고 실제 다시 시도로 회복했다. 캡처는 `.local/task08-runtime/review-registration.png`, `review-completed-edit.png`, `review-retry-mobile.png`이며직접 시각 확인했다.
- 마지막 revision 보호 후 frontend를 다시 빌드했다. `.local/task08-runtime/review-smoke.mjs`에서 최신 entry **index-Da3fgbO3.js**, 같은 상세의41분/APPLIED를 읽기 전용으로 확인했고 `review-final-detail.png`를 직접 보았다. 첫 시도는 기존SW entry가 남아 최신entry 대기가 끝났고, 재새로고침한 두 번째 실행은 통과했다. 기존 Task14 SW전환 추적 사항에 해당하며 서버 runtime5188은 재기동하지 않았다.

이하에는 초기 구현 및앞선 디버깅 기록을 그대로 남긴다.

## 구현

- 원본 Activity OpenAPI에 보호된 `GET /activities/summary?month=YYYY-MM&activityType=...`와 `MonthlyActivitySummaryResponse`를 추가하고 Java/TypeScript를 재생성했다. 기존 `ActivityController`가 generated interface를 단독 구현하므로 중복 mapping은 없다.
- Activity 집계는 owner/type/서울월 구간으로 제한한다. COMPLETED만 완료 수에 포함하고 COMPLETED/PARTIAL만 시간에 포함한다. header interval이 있으면 초 단위 내림값을 우선하고, 없을 때만 WORKOUT 세트 합/STUDY 분×60/CLIMBING 시간을 사용한다. VOIDED는 제외하며 correlated detail 합으로 기록 수가 늘지 않는다.
- `Clock`을 주입하고 서울 timezone으로 현재월을 판정한다. 잘못된 월은 `INVALID_MONTH`, 미래월은 `FUTURE_MONTH` 400이다. 조회 경로에는 write/outbox 동작이 없다.
- 인증 shell 안에서 운동/공부/클라이밍이 월 state를 공유한다. 820px 폭에서 월 이전/다음(현재월 상한), 추가/지난 기록, 완료/시간/등록 3칸을 표시한다. Activity 요약과 기존 planner CalendarSummary는 owner/month/type query key로 분리하고 한쪽 실패를 0으로 숨기지 않는다.
- 지난 기록은 과거 날짜의 실제 planner Task를 조회해 기존 RecordPage로 이동한다. 가짜 루틴 목록은 만들지 않았다.
- 기록 목록은 Activity keyset cursor의 `더 불러오기`를 사용하며 상세는 행을 연 뒤 GET한다. 상세 화면은 기존 세 유형 field와 시간 field를 재사용해 PATCH/void를 연결한다.
- PATCH snapshot의 expectedVersion은 응답까지 고정한다. 409에서 초안을 유지하며 최신 version으로 자동 재작성하지 않는다. 응답 불확실 시 GET 결과가 snapshot과 동일한 새 revision인지 확인하고, 다르면 자동 overwrite하지 않는다. 성공 응답 revision을 detail cache에 prime하고 activity list/요약 및 이전·새 수행일과 planner 월 캐시를 invalidate한다.

## RED / GREEN 증거 (2026-09-07)

- RED: 실제 PostgreSQL 테스트가 `ActivityService.monthlySummary` 부재로 compile 실패.
- GREEN: `ActivityPersistenceIntegrationTest.summarizesEachActivityOnceAndPrefersTheHeaderInterval` 성공. header 1,800초가 detail 999초보다 우선하고 PARTIAL detail 120초를 더해 1회/1,920초이며 VOIDED와 다른 owner를 제외했다.
- GREEN: `ActivityMonthlySummaryTest` 성공. UTC 8월31일15시가 서울 9월1일인 경계에서 9월을 허용하고 10월을 거부했다.
- GREEN: `ActivitySummaryHttpTest` 성공. 실제 Tomcat/JWT 경로에서 malformed month, 13월, 잘못된 type, 미래월이 모두 `application/problem+json` 400이고 현재월은 200이었다.
- RED: 월 공유/부분 실패/지난 Task/수정 충돌 UI가 없어 신규 component test 실패.
- GREEN: `DomainPage.test.tsx` 신규 3개 성공. 공유 월과 독립 실패, 과거 Task→기존 폼, stale expectedVersion 및 초안 보존을 검증했다.
- GREEN: `pnpm --filter @todorok/web build`, `node scripts/check-contract-drift.mjs`, `git diff --check` 성공.
- 기존 웹 전체 suite는 69개 중 66개 성공했다. 실패 3개는 Today 기존 테스트에서 즉시 응답 mock에도 query가 `pending`인 채 Testing Library 기본 1초를 넘긴 사례이며, 신규 테스트와 non-Strict lifecycle 사례는 성공했다. StrictMode 단일 재현에서도 calendar/day query가 pending이었다. 원인을 확정하지 못해 timeout 상향이나 제품 변경을 하지 않았다.

## 실제 runtime

- 부모가 frozen backend 이미지로 별도 `todorok-task08-ui` runtime을 기동했고 실제 HTTP probe에서 create→APPLIED→PATCH 180초→APPLIED→월 집계 반영→void→APPLIED→월 집계 제거를 확인했다.
- gstack bundled browser는 Windows application-control policy에 차단됐지만, 이전에 허용된 `agent-browser 0.36.0`은 실행됐다. credential을 스크립트 내부에서만 읽어 실제 로그인, 과거 날짜 선택, 공부 Task 생성, 도메인 월 요약 레이아웃까지 확인했다. 지난 기록 패널에서 해당 과거 Task가 나타나기를 기다리다 25초 timeout이 발생해 기존 폼 저장/수정/void의 UI gate는 완료하지 못했다. 실제 API probe의 create/PATCH/void 검증과 구분해 미완료로 남긴다.

## 후속 원인 확인과 보완 (2026-09-07 21:17 KST)

위 미완료 항목은 아래 후속 검증으로 해소했다. 월 집계의 추가 DB 경계 행렬은 별도 보강 대상으로 유지한다.

- StrictMode focused 재실행은 처음 1/1 성공했지만 전체 병렬 실행은 64/69, `--maxWorkers=1`은 68/69였다. 임시 query event 계측에서 refresh 72ms, 첫 observer commit 930ms, rollover 935ms, day/range 요청 1109ms, 두 query `success` 1111ms를 확인했다. 쿼리가 영구 pending인 결함은 아니었다. Testing Library의 반복적인 전체 role 검색 전에 React 초기 비동기 commit을 `await act(async () => render(...))`로 마치도록 lifecycle setup만 수정했다. timeout은 늘리지 않았다. jsdom worker 간 CPU 경쟁을 줄이도록 Vitest `maxWorkers: 1`을 설정했다. 수정 후 기존 69/69 및 신규 회귀 포함 **70/70**이 통과했다(12 files, 87.47s).
- native agent-browser의 `wait text=...`는 실제 snapshot에 보이는 버튼도 25초 동안 찾지 못했다. 날짜 `fill`은 native 날짜 control의 연/월/일을 0으로 표시하면서 기존 React 조회 날짜가 남는 상황도 확인됐다. 테스트를 우회하지 않고 실제 날짜 입력의 `일 일` spinbutton을 클릭하고 ArrowDown으로 7→6을 바꿨다. 입력값 `2026-09-06`, snapshot의 일=6, 해당 날짜의 Task 목록, 기존 기록 폼의 수행일 `2026-09-06`을 차례로 확인했다. 이전 로그인에서 이월된 동일 제목 Task와 새 과거 Task를 날짜로 구분했다.
- 실제 제품 결함: 이미 방문했던 공부 화면에서 과거 기록을 저장하고 APPLIED를 확인해도 월 요약은 **0회/0분**, 목록은 빈 상태로 25초 유지됐다. 원인은 기존 create/return 흐름이 `calendar`만 무효화하고 새 `activities`, `activity-summary`, `calendar-summary` 캐시를 갱신하지 않는 것이었다. `DomainPage.test.tsx`의 기존 화면 방문→과거 폼 저장→요약/목록 재방문 회귀가 `1회` 부재로 RED였다. create 응답과 APPLIED revision에서 현재 owner의 세 캐시를 갱신하도록 연결해 GREEN으로 바뀌었다. APPLIED의 revision당 한 번 처리, 다른 owner 캐시 보존, 로그아웃/화면 전환 후 늦은 응답 무시 회귀도 유지했다.
- 최신 frontend build를 nginx host bind에 반영한 후 실제 브라우저에서 `브라우저8e 과거 공부`를 9월6일에 기록했다. Activity `a1f4c745-3ca8-428c-919b-5a108ddd2ac4`: 30분 저장→과거 날짜 복귀/APPLIED→기존 공부 화면 **2회/60분/등록5개**와 새 이력 표시→메모 및 45분 PATCH→**75분**→void→명시적 `취소된 기록`/목록 `취소됨`, **1회/30분** 복귀를 확인했다. 기존 1회/30분 fixture가 있으므로 증분을 검증했다. 실제 PostgreSQL readback도 **VOIDED, revision 2, APPLIED, 수정 메모**였다.
- 운동에서 8월로 변경 후 클라이밍에서도 8월 유지됨을 실제 메뉴로 확인했다. 390px 최초 캡처에서는 추가 버튼이 글자 단위로 줄바꿈되는 문제가 보여 480px 이하에서 월 이동과 추가/지난 기록을 두 행으로 배치했다. 최종 screenshot을 직접 확인했으며 가로 overflow 없음, 월 이동 버튼 **44×44**, 추가/지난 기록 버튼 **높이46px**, nowrap을 확인했다. 요약 3칸은 유지한다.
- 검증 명령: `pnpm --filter @todorok/web exec vitest run --maxWorkers=1` **70/70**, `pnpm --filter @todorok/web build` 성공, `git diff --check` 성공. UI script는 `.local/task08-runtime/browser-ui.mjs`, 최종 모바일 검증은 `mobile-check.mjs`이며 credential은 로컬 state 내부에서만 사용한다.
- 실제 캡처: `.local/task08-runtime/task08-created.png`, `task08-edited.png`, `task08-voided.png`, `task08-mobile.png`.

### 별도 배포/PWA 후속 사항

기존 브라우저에서 dist를 교체한 직후 공부 lazy 화면이 한 차례 blank가 됐다. nginx 로그 12:12:23 UTC에 이전 `/assets/DomainPage-DfclhuWz.js`, `/assets/StudyPage-DE_Ha1XF.js` 요청이 **200/736bytes(index.html fallback)**였고, 12:12:22에는 새 SW가 새 chunk를 precache했다. 새로고침 후 새 entry `index-DO9x0r2f.js`로 전체 UI flow가 성공했다. 모바일 보완 build 이후도 새 entry `index-3-5T6J7U.js`를 확인했다. 낡은 lazy chunk 오류 복구와 asset 경로의 HTML fallback/SW 전환 검증은 Task14의 배포/PWA gate로 전달하며, 이 변경에서 SW 설계를 확장하지 않았다.
