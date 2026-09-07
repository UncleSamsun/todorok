# 작업08 월별 조회와 기록 관리 UI

서버 변경 단계의 `task-08-backend-report.md`가 확정된 뒤 착수한다. 전체 요구는 `task-08-brief.md`, PRD9.2/10.4, DESIGN9다. workspace는 `C:\workspace\todorok-worktrees\functional`이다.

## 월별 조회

- activity 원본 OpenAPI에 월·유형을 받는 보호된 조회를 추가한다. `GET /activities/summary?month=YYYY-MM&activityType=...`, method `getMonthlyActivitySummary` 제안을 사용하되 기존 확정 계약이 있으면 재사용한다.
- 결과에는 month/activityType/completedCount/durationSeconds를 명시한다. principal owner로 제한하고 잘못된 월/유형·미래 월을400으로 거부한다. Clock의 서울월을 기준으로 비교한다.
- 완료는 COMPLETED만, 시간은 COMPLETED와PARTIAL만 합산한다. VOIDED는 둘 다 제외한다.
- 시간은 실제 header 구간이 있으면 그 초수를 우선한다. 없으면 WORKOUT의 세트 durationSeconds 합, STUDY의 durationMinutes×60, CLIMBING의 durationSeconds를 사용한다. 둘을 더하지 않는다. 없는값은 추정하지 않는다. 초단위 미만이 있으면 각 기록의 실제구간 초를 내림한 정수를 사용한다.
- 등록은 새 중복endpoint 대신 기존 planner 월 범위 CalendarSummary의 해당유형 total을 합산한다. DELETED는 서버가 제외한다. 다른서비스 schema조회 금지.
- 집계는 읽기 전용이며 endpoint 호출로 이월·기록수정·다음회차생성하지 않는다. 타입별detail을중복join해count가늘지않도록한다.

## UI

- 월선택 state를 인증세션 안에서 세 도메인이 공유한다. 기본현재월, 이전/다음, 현재월에서다음비활성. 도메인폭820px중앙·상단좌측월/우측추가·지난기록·둘째줄3칸중앙수치·수평구분선없음.
- 서로다른두API의응답은같은user/month/type만조합한다. 한쪽실패를0으로위장하지않고 개별실패·재시도를표시한다. 월변경중늦은응답이현재월을덮지않는다.
- 신규기록은 기존폼을재사용한다. 지난기록 날짜→기존Task/루틴선택→동일폼→수행과거일복귀. 지원하지않는가짜루틴목록은추가하지않는다.
- 실제API의목록·상세·PATCH·void를연결한다. expectedVersion을편집snapshot과함께보존하고409에서초안을지킨다. 취소는행삭제가아닌VOIDED다.
- correction의수행일변경은이전/새날짜와두월의캐시를갱신한다. revision별동기화상태를07의컴포넌트와연결한다.
- 긴이력은nextCursor기반더불러오기, 상세는사용자가열때조회한다. 기록형완료클릭으로새Activity를만들지않고상세를열도록한다.

## 테스트

- 실제DB월집계: 월경계서울00시/윤년/VOIDED/PARTIAL/미입력/0/구간과detail동시입력중복합산방지/세유형/다른owner/미래월.
- read-only검증: 조회전후Task/Activity/outbox행·version동일. 상세행여러개인workout도count1.
- component: 세탭공유월,현재상한,두API부분실패,빠른월변경·늦은응답,수정충돌초안,void후명시상태.
- 실제브라우저 과거기록·수정·void·월이동·기록지연반영과요약을검증한다. 서버단계테스트만으로UI완료를주장하지않는다.

일반조회/UI구현 범위이며 서버correction/동시성의 새결함은증거와함께조율한다. 하위작업자생성/push/merge금지. apply_patch/PowerShell. 부모progress/다른brief는수정하지않는다. scoped한글commit과 `task-08-summary-ui-report.md`에실제검증을기록한다.
