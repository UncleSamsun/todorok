# 작업04 Task·달력·오늘 화면

## 구현

- `planner-v1.yaml`에 Task 상세 GET을 추가하고 Spring interface와 TypeScript Fetch client를 재생성했다. 생성 파일을 직접 편집하지 않았다.
- V4 `task` table, `(user_id, scheduled_date)` index, JPA optimistic version을 추가했다. 로그인 principal UUID만 소유자로 사용하며 다른 사용자 및 삭제된 Task는 404다.
- GENERAL 생성·수정·완료·재개·soft delete와 네 유형의 저장·집계를 구현했다. 기록형의 일반 완료/재개는 409이며 실제 기록 저장은 후속 작업의 도메인 경로로 연결한다. 상태 변경은 허용된 command만 실행한다.
- TaskScheduled/TaskChanged를 기존 envelope와 OutboxEventWriter로 저장한다. JPA flush 뒤의 version을 envelope에 넣으며 Task와 JDBC outbox는 같은 transaction에 참여한다. 실제 값이 변하지 않은 수정은 추가 이벤트를 만들지 않는다.
- 날짜 상세와 Task 상세는 DTO constructor projection, 범위는 날짜·유형별 group-by projection이다. 범위는 양끝 포함 1~42일이며 빈 날짜와 고정 순서 네 유형을 포함한다. entity를 API로 반환하지 않는다.
- TodayPage를 WeekCalendar, MonthCalendar, TaskGroups, QuickAdd, TaskEditor로 분리했다. 세션의 인증 fetch를 생성 client에 주입하고 range/day/task Query key를 분리했다. 변경 성공 후 관련 cache를 갱신하며 저장 실패에는 입력과 기존 상태를 유지한다.
- 주/월 선택만 기기에 저장한다. 새 세션은 실제 서울 오늘에서 시작한다. 주간은 7일, 월간은 월말을 보정해 한 달 이동한다. 날짜 계산은 UTC civil date를 사용한다.
- 모바일 브랜드와 하단 5탭, 데스크톱 sidebar와 달력/목록 두 열, 주간 8열 정렬, 월간 완료 카테고리 막대, 비율 채움과 실제 수의 접근성 이름을 추가했다. 네 그룹은 빈 상태에서도 추가 버튼을 표시한다.

## RED와 수정

1. HTTP 테스트 기동 조건을 맞춘 후 실제 PostgreSQL과 서명 JWT로 Task 생성 시 기대 201/실제 404를 확인했다. 두 HTTP 테스트가 새 endpoint 부재로 실패했다.
2. Today 실컴포넌트 테스트는 `할 일 추가` 버튼을 찾지 못해 실패했다. 날짜 계산 테스트는 `addDays is not a function`으로 실패했으며 이는 모듈 부재 증거다.
3. 최초 Task 구현 후 생성은 outbox table 탐색 실패로 500이었다. 테스트 컨테이너 JDBC URL에 이미 query가 있어 단순 `?currentSchema` 추가가 적용되지 않았다. 테스트 connection 초기 SQL로 search_path를 지정해 해결했다. 실제 Compose는 기존 서비스 DB URL의 currentSchema를 사용하며 브라우저에서도 저장을 확인했다.
4. no-op 수정 회귀 테스트에서 outbox 행 기대 1/실제 2를 확인했다. 동일 값 수정에서 기존 DTO를 반환하도록 고쳤다. 변경 이벤트는 0,1,2,3,4 aggregateVersion 순서로 검증했다.
5. 기존 앱 테스트는 제거한 큰 `오늘` 제목과 모든 요청을 auth 응답으로 처리하던 fixture 때문에 실패했다. 실제 새 화면의 선택 날짜 region과 날짜 조회 응답으로 변경했다. 인증/cache 검증은 유지했다.
6. 기존 persistence migration-count assertion 두 개가 V4 추가로 기대 4/실제 5가 되어 실패했다. V1~V4와 테스트 migration V9000의 다섯 개를 반영했다.

처음 HTTP harness의 테스트용 entity migration 누락과 Origin 누락은 context 기동 실패였으며 endpoint 동작 RED로 계산하지 않았다. TypeScript Date/string 불일치는 생성 계약의 date가 string인 점을 맞춰 수정했다. 임시 이미지 첫 build는 `.dockerignore`의 build 제외 때문에 실패했고, 전용 Dockerfile.dockerignore로 방금 빌드한 세 jar만 전달했다. 이 빌드 실패는 제품 런타임 실패가 아니다.

## 자동 검증

- `gradlew.bat :services:planner-service:test`: 24 tests, 실패 0, 2026-09-07 16:39 KST 종료. 실제 PostgreSQL Testcontainers를 사용했다.
- Task HTTP 5개 테스트: lifecycle, 다른 사용자 조회/변경 차단, stale version, 빈/공백/121자 제목, 잘못된 날짜, 역전/43일 범위, 기록형 완료 차단, deleted 제외, no-op event, rollback, projection 집계 및 query 수를 검증한다.
- 상태 경계(PLANNED 재개 및 COMPLETED 중복 완료 409)와 타 사용자 PATCH/complete/delete 404, 잘못된 생성 날짜까지 보강한 최종 `gradlew.bat :services:planner-service:test --tests '*TaskHttpIntegrationTest'`도 5/5 통과했다(16:42 KST). 이 추가 assertion 뒤에는 planner 전체를 다시 반복하지 않았다.
- 실제 outbox CHECK constraint를 실패시키면 HTTP 생성/완료는 500이고 새 Task는 없으며 기존 Task는 PLANNED/version0, 기존 outbox 한 행을 유지한다. mock writer가 아니다.
- 실제 DB에 100/1000개를 각각 넣고 날짜 상세와 42일 범위를 HTTP로 조회했다. Hibernate prepared statement는 각각 정확히 1회다. 100개는 total100/completed25/GENERAL50 중25 완료, 1000개는 total1000/completed250/GENERAL500 중250 완료를 확인했다. 타 사용자 데이터는 반환하지 않는다.
- `pnpm test:web`: 6 files, 22 tests 통과. 이어 추가한 CalendarProgress 테스트까지 `pnpm --dir apps/web exec vitest run src/features/today`: 3 files, 4 tests 통과. 추가 두 테스트는 사후 회귀 보강이며 최초 RED라고 주장하지 않는다.
- 날짜 테스트: 윤년 2월29일, 1월31일→2월말, 3월31일→2월말, 12월→다음해1월, 연말 주간, 6주차 월간, 서울 자정 경계. 진행률 테스트: 1/2는 50%, 실제 수의 접근성 이름, 선택 배경은 날짜에만, 월간 막대는 완료 유형만.
- `pnpm test:packages`: 네 패키지 TypeScript 검사와 api-client runtime 5 tests 통과.
- `pnpm contracts:check`: 생성 원본과 일치.
- `pnpm build:web`: TypeScript·production·PWA build 통과. 초기 JS gzip 91.98KB, 도메인 네 lazy chunk 별도 생성.
- `git diff --check`: 통과.

## 실제 브라우저

`node .local/task04/browser-smoke.mjs` 종료 코드 0, 2026-09-07 16:39 KST `REAL_BROWSER_TASK_PASS`를 출력했다. agent-browser 0.36.0과 실제 Chromium, `http://localhost:5184`, 새 RSA 키·임시 계정·격리 PostgreSQL volume을 사용했다.

`:services:planner-service:bootJar`, `migrationBootJar`, `bootstrapBootJar`로 현재 코드의 jar 세 개를 만들고 모두 `todorok/planner-service:task04`에 COPY했다. 기존 local image에서는 JRE/curl runtime layer만 재사용했다. 이미지 manifest digest는 `sha256:ea6176897240196af56a6a34e659552a10d433951b08484c99e6bdb2fd01d6f0`다. 이 이미지로 V4 migration·bootstrap·HTTP server를 실행했으며 최신 웹 production dist를 Nginx로 제공했다. 이후 소스 수정은 formatting/import 정리와 테스트 보강이었다.

실제 흐름:

1. 로그인 → GENERAL 생성 → 새로고침 후 같은 항목 유지.
2. 완료 → 다음 주 이동 → 오늘 복귀 → 완료 취소.
3. Task 상세 GET → 제목 수정 → 네 유형 생성.
4. 공부 체크 → `/study?taskId=...` 이동 → 오늘 복귀 시 PLANNED 유지.
5. 390/420/820/1440px × light/dark × week/month 16조합. 모든 조합에서 document scrollWidth≤viewport, 모든 날짜 버튼 너비·높이≥44px assertion 통과.
6. 실제 브라우저 offline → 저장 실패 → 제목 입력 보존 → online 복귀 → 같은 입력 저장 성공.
7. 수정한 Task 삭제 → 새로고침 후 삭제 유지.

Nginx actual access log에서 Task POST201, complete/reopen200, PATCH200, 상세 GET200, DELETE204와 날짜/range GET200을 확인했다. 네트워크를 mock하지 않았다. `.local/task04/390-light-week.png`, `390-dark-month.png`, `1440-light-week.png`를 직접 열어 화면을 확인했다. 나머지 13개 조합 PNG도 같은 디렉터리에 있다.

전용 Compose project의 컨테이너·network·volume은 harness finally에서 제거했다. 테스트 RSA private/public PEM도 제거했다. 테스트 이미지와 screenshot/harness는 로컬에 남아 있으며 Git에 넣지 않았다.

## 경계

- 반복·이월은 작업05, 메모는06, 상세 Activity 기록 폼은07 이후다. 기록형 체크는 실제 도메인 route로 연결하며 완료를 임의로 만들지 않는다.
- 화면 검증은 Chromium이다. 실제 iPhone Safari 및 네이티브 PWA 설치는 이 작업에서 검증하지 않았다.
- 모든 동작을 RED로 개별 관찰했다고 주장하지 않는다. 최초 endpoint/화면 부재와 no-op event 실패를 관찰했고, 나머지 경계는 실제 통합 회귀 테스트와 브라우저로 검증했다.
- 원격 push/merge는 하지 않았다. parent 소유 PRD/progress/brief와 다른 작업 파일은 변경·commit하지 않는다.
