# 작업08 월별 조회와 기록 관리 UI 보고

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
