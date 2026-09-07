# 작업05 반복 일정·이월·건너뜀

## 구현과 결정

- PRD9.1·10.2·10.3에 시작 anchor 고정, 일요일 주간 경계, 없는 월 일자 건너뜀, 종료일 포함, 기존 활성 회차가 있을 때 reopen409, archive 후 기존 Task 유지 정책을 먼저 명시했다. 기존 AC3는 보존했다.
- `planner-v1.yaml`에 series 생성·상세·수정·archive, Task skip와 명시적 rollover 계약을 추가한 뒤 생성기로 Spring interface와 TypeScript client를 갱신했다.
- V5에 `task_series`, 원래 `occurrence_date`, `series_id`, 메모, 소유자를 함께 검사하는 FK와 `task_series_one_planned` partial unique index를 추가했다. V1~V4는 변경하지 않았다.
- DAILY/WEEKLY/MONTHLY는 시작 anchor와 마지막 원래 발생일로 다음 날짜를 계산한다. 예정일 변경·이월은 anchor와 원래 발생일을 바꾸지 않는다. 월별로 아예 존재하지 않는 조합(매년 2월31일 등)도 유한한 Gregorian 주기 검사 뒤 종료한다.
- `TaskTransitionPolicy`가 일반 완료, 모든 유형의 명시적 건너뜀, 일반 완료/건너뜀 재개를 결정한다. 기록형 COMPLETED 재개만 Activity 경로로 제한하며 기록형 SKIPPED 재개는 허용한다.
- series→task 순서의 DB 잠금, 안정된 다중 series 잠금 순서, optimistic version과 DB partial unique로 활성 회차 하나를 보호한다. 다음 회차 생성에서 실제 unique index 충돌은 409로 반환한다.
- Task와 series 변경 이벤트는 같은 트랜잭션의 outbox에 저장한다. TASK_ROLLED_OVER·SERIES_CHANGED의 JSON Schema를 추가하고 TASK_CHANGED에 SKIPPED를 반영했다.
- `Clock`을 주입해 서버 오늘은 서울 LocalDate, 이벤트 시각은 UTC Instant로 계산한다. rollover는 같은 Task의 예정일만 바꾸며 GET은 변경하지 않는다. 중복 command는 추가 이벤트를 만들지 않는다.
- 기존 날짜·상세 조회는 `TaskView` constructor projection을 사용하며 entity를 API로 반환하지 않는다. 범위 집계와 상세의 기존 쿼리 수 테스트를 유지했다.
- 빠른 추가에 일/주/월 반복·간격·요일·일자·종료일 옵션, 편집에 건너뜀·재개·반복 설정·반복 중단을 연결했다. series 설정 수정은 이후 회차에 적용하고 현재 Task 수정과 구분한다.
- 오늘 진입은 rollover가 성공한 뒤 달력·상세를 읽는다. 서버가 반환한 오늘을 사용하며 실패와 명시적 재시도를 표시한다. 주간 날짜 계산은 브라우저 전역 없는 client-domain의 civil-date 함수로 처리한다.

## RED와 발견한 문제

1. 구현 전 실제 PostgreSQL·서명 JWT HTTP 테스트의 `POST /series`가 기대 201을 충족하지 못했다. 구현 후 같은 테스트가 통과했다.
2. UI 테스트는 반복 입력을 찾지 못하고 이월 실패 표시도 없어 2/2 실패했다. 반복 폼과 command→query 순서를 구현한 뒤 2/2 통과했다.
3. 날짜 정책 테스트의 최초 실행은 새 계약의 skip interface 미구현으로 compile 단계에서 막혔다. 상태 정책 테스트의 최초 실행은 새 클래스 부재로 compile 단계에서 실패했다. 이 둘을 날짜/상태 assertion의 행동 RED라고 주장하지 않는다.
4. 생성 TypeScript 계약은 weekdays를 `Set<number>`로 표현한다. 초기 배열 구현은 production TypeScript 검사에서 실패했고 Set을 사용하는 UI 및 생성 client 직렬화로 수정했다.
5. 새 이월 요청 때문에 기존 화면 fixture에도 rollover 응답을 추가했다. 병렬 전체 테스트에서 최초 비동기 화면 조회의 기본 1초 대기가 부족했던 한 테스트는 5초 대기로 조정했다. 이후 전체 웹 테스트가 통과했다.
6. 최초 coverage gate는 날짜 26/27 분기로 실패했다. 시작일보다 한 달 앞선 cursor→시작일 경계를 추가해 27/27로 통과했다. 정책 클래스나 실제 정책 분기를 제외하지 않았다.
7. 첫 실제 브라우저 실행은 반복 생성→완료→다음 회차→건너뜀→과거 확인→설정 수정→archive→재개까지 성공했다. 이후 CLI의 date input fill이 빈 날짜를 남겨 HTML required 검증으로 이월용 Task POST가 실행되지 않았고 대기 assertion이 실패했다. 날짜 input의 native value setter와 input/change 이벤트, 값 assertion을 쓰도록 검증 스크립트를 수정했다. 이는 서버 시계 변경이 아니며 제품 코드는 바꾸지 않았다.

## 실제 DB와 규칙 검증

최종 집중 실행(2026-09-07 17:13 KST 완료):

`gradlew.bat :services:planner-service:test --tests '*TaskHttpIntegrationTest' --tests '*NextOccurrencePolicyTest' --tests '*TaskTransitionPolicyTest' :services:planner-service:jacocoTestCoverageVerification :libs:event-contracts:test :services:planner-service:bootJar :services:planner-service:migrationBootJar :services:planner-service:bootstrapBootJar`

- HTTP 14/14, 날짜 정책 4/4, 상태 정책 2/2 통과. Testcontainers PostgreSQL17.11이며 실제 HTTP·JWT·JPA·outbox를 사용한다.
- 이벤트 계약 테스트 16/16 통과(Envelope4, Schema12).
- 그 전 전체 planner 테스트 실행에서도 테스트 자체는 모두 통과했다. 당시 coverage gate만 날짜 1개 미검증 분기로 실패했다. V5 반영 후 migration 성공 수는 테스트용 V9000 포함 6으로 확인했다. 후속 회차/조회 보강은 위 집중 테스트에서 검증했다.

| 경계 | 실제 기대·검증 |
|---|---|
| DAILY interval2·연말 | 2026-12-31 다음은 2027-01-02 |
| WEEKLY 여러 요일·주간격2 | 9월7일 월요일 다음은 9월20일 일요일, 그 다음 9월21일 |
| MONTHLY31·2월 없음 | 1월31일 다음은 3월31일 |
| 윤년·연 간격 | 2027년2월 기준 29일 반복 다음은 2028-02-29 |
| 종료일 | 당일 포함, 다음 발생일이 종료일 뒤면 없음 |
| anchor 보존 | 월요일 원래 회차를 수요일 완료하면 다음 월요일9월14일 |
| 실제 이월 | 과거로 수정한 같은 Task를 이월해도 id와 원래 발생일 유지 |
| 동시 rollover | 두 호출의 movedCount 합1, 추가 재호출0, 이벤트1 |
| 서버 날짜 | 고정 Clock 2026-09-08T15:00:00Z → 서울9월9일 |
| 동시 complete/skip | 200과409, 기존 terminal1·활성1·이벤트4 |
| 동시 reopen | 삭제한 미래 회차 후 기존 재개 경쟁은 200/409와 활성1 |
| partial unique 직접 충돌 | DB insert 실패, 활성1 유지 |
| partial unique API 충돌 | 테스트 DB trigger가 다음 생성 시 경쟁 회차 삽입 → HTTP409, 완료/생성/outbox 모두 rollback |
| 다음 outbox 실패 | CHECK constraint로 다음 TASK_SCHEDULED 거절 → HTTP500, 원래 PLANNED/version0·Task1·이벤트2 |
| 기존 활성 회차 | advance 호출이 기존 id·날짜 유지, reopen409은 이벤트 추가 없음 |
| archive/end/PARTIAL/일회성 | 불필요한 다음 회차 없음, archive 시 현재 Task 유지 |
| 소유자·버전·유효성 | 다른 사용자 series GET/PATCH/archive404, stale409, 간격/요일/일자/종료일 오류400 |
| series 수정 | anchor·현재 Task 불변, 다음 회차 제목·메모·규칙 반영, no-op 이벤트 없음 |

`jacocoTestReport.xml`의 실제 **정책 클래스 branch** 수는 NextOccurrencePolicy 27 covered/0 missed, TaskTransitionPolicy 24 covered/0 missed다. 이것은 전체 service branch100%라는 뜻이 아니다. report는 생성 API 디렉터리만 제외하며 coverage gate는 위 두 실제 정책 클래스에 BRANCH1.0을 적용한다. 기존 전체 percent gate를 낮추지 않았다.

## 웹·계약 검증

- `pnpm test:web`: 8 files, 27 tests 통과(17:16 KST 실행). 이월 실패 시 calendar 요청0, 재시도 성공 뒤 range/day 두 요청, weekly 옵션의 정확한 입력, 기존 version 스냅샷·실패 초안 보존을 검증한다.
- `pnpm test:packages`: 네 패키지 TypeScript 검사와 api-client runtime5 tests 통과.
- `pnpm contracts:check`: 생성 계약과 원본 일치.
- `pnpm build:web`: production/PWA 빌드 통과. 초기 JS gzip94.31KB, 나머지 네 도메인은 lazy chunk다.
- stage 전 `git diff --check`는 기존 추적 파일에 대해 통과했다. stage 뒤 검사에서는 새로 추가된 생성 파일 16개에서 `new blank line at EOF` 경고가 나왔다. 이는 생성기의 현재 canonical 출력이며 `contracts:check`는 통과한다. 생성 파일을 수작업 수정하거나 whitespace 검사 설정을 완화하지 않았다. 기능 검증과 별개인 남은 정리 사항으로 기록한다.

## 실제 브라우저

`node .local/task05/browser-smoke.mjs` 재실행은 종료 코드0과 `REAL_BROWSER_TASK05_PASS`로 완료했다(2026-09-07 17:23 KST HTTP 기록). 실제 Chromium·agent-browser0.36.0, `http://localhost:5185`, 새 임시 계정/RSA 키와 격리 PostgreSQL volume을 사용했다. 네트워크 응답을 mock하지 않았다.

현재 코드의 bootJar·migrationBootJar·bootstrapBootJar 세 개를 새 이미지 `todorok/planner-service:task05`에 COPY했다. 이미지 manifest digest는 `sha256:3a1c95e2761c0f002da208ac767c097f5b019102b2ebec69cbfbd9559b06e78c`다. 기존 local image에서는 JRE/runtime 기반만 재사용했다. 이후 제품 파일 변경은 formatting과 unused import 정리뿐이다.

실제 확인한 순서:

1. 로그인 → rollover 성공 → 서울 오늘 상세 → DAILY 반복 추가.
2. 완료 → 다음날 새 회차 → 해당 회차 건너뜀 → 그 다음날 활성 회차 하나.
3. 원래 날짜의 COMPLETED와 다음날의 SKIPPED 상태를 다시 선택해 확인.
4. 반복 제목·메모 수정의 DB 영속화 확인 → archive → 기존 활성1 유지 → skip 후 활성0 → 건너뜀 취소 후 원래 Task 재개.
5. 일회성 Task의 예정일을 어제로 입력 → 어제 GET에서 그대로 조회 → 오늘 버튼으로 rollover → DB의 taskId가 이전과 동일하고 날짜만 오늘 → 어제 목록에서 제외.
6. 브라우저 offline → 오늘 진입 실패와 재시도 표시 → online → 명시적 재시도 성공.
7. 390/1440px × light/dark 네 조합에서 가로 overflow 없음 assertion. 새로고침 뒤 같은 이월 Task 유지.

Nginx 로그에서 series POST201·GET200·PATCH200·archive200, skip200, rollover200을 확인했다. `.local/task05/390-light.png`, `390-dark.png`, `1440-light.png`, `1440-dark.png`가 화면 증거다. 390-light와 1440-dark 파일을 직접 열어 모바일 세로 배치·데스크톱 두 열·완료/미완료 표시와 잘림 여부를 확인했다.

브라우저 시계는 바꾸지 않았다. 서버의 서울 자정 경계는 별도의 실제 HTTP 고정 Clock 테스트로 검증했다. 검증 전용 Compose 컨테이너·network·volume은 harness finally에서 제거했고 임시 RSA PEM 두 파일도 삭제했다. 테스트 이미지·harness·PNG는 로컬 `.local/task05`에 남기며 Git에는 넣지 않았다.

## 범위

- Activity 완료/void 이벤트의 실제 수신 연결은 후속 작업08이다. 이번에는 완료 결과가 COMPLETED/PARTIAL일 때 다음 회차를 결정하는 정책과 서비스 경계를 검증했다.
- 실제 iPhone Safari와 네이티브 PWA 설치는 검증하지 않았다.
- 원격 push/merge와 하위작업자 생성은 하지 않았다. progress ledger·다른 brief·`.superpowers`는 parent 소유로 유지한다.
