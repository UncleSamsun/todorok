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
