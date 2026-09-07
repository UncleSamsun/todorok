# 작업07 기록 UI 구현 보고

## 구현

- 운동·공부·클라이밍 Task 체크를 실제 타입별 기록 폼에 연결했다. 숫자 `0`, 미입력, 클라이밍의 명시적 `false`를 구분하며 Activity 메모를 Task/DailyNote와 분리한다.
- 수행일과 선택 시간 쌍을 전송한다. 시간은 오전/오후·1–12시·5분 단위 선택이며 미입력 시 `startedAt`/`endedAt`을 보내지 않는다.
- planner/activity 생성 클라이언트 모두 `Configuration({ fetchApi: session.fetch })`를 사용한다. 응답 불확실 상태에서는 입력과 commandId snapshot을 잠그고 같은 요청만 재전송한다. 명확한 400/422와 로컬 검증 실패에서는 수정할 수 있다.
- 저장 후 수행일의 오늘 화면으로 돌아가 Activity GET으로 PENDING/APPLIED/CONFLICT/NOT_REQUIRED를 표시한다. GET은 상태 확인만 수행한다. APPLIED 전이는 달력 query를 한 번 무효화해 기존 Task 단일 행의 완료 요약과 실제 시간 블록을 갱신한다.
- 이탈·logout·Task 전환 후 늦은 요청이 새 화면을 덮지 않도록 session generation, attempt, mount와 taskId key를 함께 검사한다.
- 세 도메인 첫 화면은 당일 실제 Task를 읽어 같은 기록 흐름으로 진입한다.

## RED / GREEN

- RED: `RecordPage.test.tsx` 최초 2건은 기록 화면과 타입 폼이 없어 2/2 의도대로 실패했다.
- 후속 RED: 불확실 응답 뒤 메모 입력이 활성화된 실패를 확인하고 입력 잠금과 동일 commandId 재전송을 구현했다.
- GREEN: `pnpm --filter @todorok/web test` — 10 files, 43 tests 통과. 주요 검증은 0/false·시간 생략·수행일 복귀, 응답 유실 snapshot/commandId 보존, 로컬 시간 오류 POST 0회, 명확한 422 뒤 편집 가능이다.
- `pnpm --filter @todorok/web build`와 `git diff --check` 통과.
- 남은 집중 검증: StrictMode effect replay에서의 제출, 취소·Task 전환 뒤 지연 성공 무시, 자동 PENDING→APPLIED 전이의 calendar 갱신은 구현 guard/effect가 있으나 전용 자동화 회귀 테스트를 아직 추가하지 않았다. 실제 production 브라우저 통과는 StrictMode 개발 replay를 대신하지 않는다.

## 실제 브라우저 / 서비스

2026-09-07 전용 `todorok-task07-ui` 환경에서 현재 source로 새로 만든 planner/activity 이미지(V8/V3), PostgreSQL 17.11, Kafka 4.3.1, Debezium Connect 3.6.2, messaging 활성화 서비스와 최신 web dist를 사용했다.

- 일반 포인터 조작으로 고유한 WORKOUT/STUDY/CLIMBING Task를 만들고 각 기록 폼을 저장했다.
- WORKOUT은 스쿼트 5회·60kg, 19:00–19:30과 메모를 저장했다. STUDY는 duration `0`, CLIMBING은 attempts `0`과 completed `false`를 저장했다.
- 각 저장은 수행일 오늘 화면으로 복귀했고 Activity GET의 실제 APPLIED와 planner 기존 Task COMPLETED를 확인했다. DB exact join에서도 세 Activity 모두 APPLIED/COMPLETED였으며 새 Activity 행을 별도 Task 행으로 중복 표시하지 않았다.
- 취소 폼의 메모는 Activity DB에 0건이었다. 390×844에서 가로 overflow가 없음을 확인했다.
- 증거 이미지: `.local/task07-runtime/task07-mobile.png`.

초기 브라우저 실행 두 번은 submit 셀렉터와 CLI select 사용법 불일치로 기록 저장 전에 중단했다. 세 번째 부분 실행에서 운동·공부 저장까지 확인한 뒤 클라이밍 select의 접근 가능한 이름 누락을 발견해 `aria-label`을 추가했다. 최종 전체 실행은 위 항목을 모두 통과했다.

## 후속 수명주기 회귀 검증 (2026-09-07)

- RED: `pnpm --filter @todorok/web exec vitest run src/features/activity/lifecycle.test.tsx`에서 8건 중 2건 실패, 6건 통과. 지연 POST 뒤 taskId를 바꾸면 이전 성공 응답이 `/climbing?taskId=task-2`를 `/today?...activityId=activity-1`로 덮었다. 수동 APPLIED 확인은 calendar 무효화를 예상 1회 대신 2회 실행했다.
- 원인과 수정: `DomainOrRecord`의 전역 location 조회를 `useSearchParams` 구독으로 바꿔 taskId 변경 때 기록 컴포넌트 key가 실제 갱신되도록 했다. APPLIED 무효화는 조회 결과 effect 한 곳에서만 실행하고 세션 generation·activityId·version으로 중복을 막는다. 수동 확인 중 상태는 query의 `isFetching`에서 읽어 이전 promise가 새 화면의 상태를 변경하지 않게 했다.
- GREEN: `pnpm --filter @todorok/web exec vitest run src/features/activity/lifecycle.test.tsx src/features/activity/RecordPage.test.tsx src/App.test.tsx` — 3 files, 17 tests 통과. 새 lifecycle 10건은 React StrictMode effect replay 이후 제출, 취소·Task 전환·logout 이후 지연 POST 무시, 실제 refetchInterval의 PENDING→APPLIED 자동 조회와 기존 Task 단일 행 완료/요약 갱신 및 무효화 1회, 수동 확인 무효화 1회와 다음 Activity의 독립 갱신, 수동/자동 GET 이후 Activity 전환·logout guard를 검증했다. APPLIED 뒤 추가 polling도 발생하지 않았다.
- 기존 RecordPage의 응답 유실 commandId 재전송, 로컬 시간 오류, 명확한 validation 거절 후 편집 회귀를 유지했다. 첫 build는 테스트의 지원하지 않는 `getByRole` 옵션 `exact` 때문에 실패했고 제거 후 `pnpm --filter @todorok/web build`와 `git diff --check`를 통과했다.
- 초기 탐색 실행은 StrictMode로 모든 fixture를 감싸 취소된 GET도 횟수에 넣는 mock 때문에 추가 실패가 있었다. StrictMode 제출 검증은 유지하고 나머지 상태 전이 fixture는 일반 mount로 분리한 뒤 위 RED를 재확인했다. 초기 `test -- ...`는 전체 suite를 실행했으므로 최종 집중 실행은 `exec vitest run`을 사용했다.
- 이 후속 작업에서 backend/runtime을 다시 만들거나 브라우저 검증을 재실행하지 않았다. 상위의 실제 브라우저 증거와 이번 개발 effect replay·지연응답 회귀 증거는 별개다. 작업07 전체 완료 여부는 부모 검토에 남긴다.

## 리뷰 1차 수정: 불확실 응답과 초안 보존

- 502/504 및 ProblemDetails로 validation 거절을 확인할 수 없는 응답은 저장 결과 불확실 상태로 유지한다. 입력을 잠그고 원래 commandId와 요청 전체를 다시 전송한다. 400/422의 알려진 validation code는 새 입력을 허용한다. `TASK_REFERENCE_PENDING` + `retryable: true`는 같은 요청 재확인을 안내하고, 재시도 불가 409는 충돌 코드와 오늘 화면 확인 동작을 표시한다.
- 인증된 화면 수명 동안 유지되는 메모리 전용 `RecordDraftProvider`를 추가했다. 유형/Task별 초안과 제출 snapshot은 취소·메뉴·뒤로 이동 후 복원된다. 제출 시 실제 수행일을 고정해 Task 일정이 변경되어도 표시·재전송·복귀일이 일치한다. logout/generation 변경은 provider를 교체하며 이전 요청 callback은 현재 화면이나 새 세션 초안을 갱신하지 않는다. localStorage나 오프라인 queue는 사용하지 않는다.
- 시/분이 모두 비었을 때만 시간을 생략한다. 일부만 선택한 시작/종료는 POST 전에 오류를 표시한다.
- 검증 과정: 리뷰 지적을 바탕으로 수정과 새 회귀를 추가했다. 수정 전 새 테스트의 RED 실행은 하지 않았으므로 별도 RED로 주장하지 않는다. 첫 실행은 28건 중 27건 통과, 1건은 logout 재로그인 후 이월 중인 비활성 Task 버튼을 fixture가 먼저 클릭해 실패했다. 활성화 대기를 추가했다. 이어 수행일 보존 회귀를 추가한 집중 실행은 3 files / 29 tests 통과했다. 최종 실행 수치는 아래에 기록한다.
- 최종 GREEN: `pnpm --filter @todorok/web exec vitest run src/features/activity/lifecycle.test.tsx src/features/activity/RecordPage.test.tsx src/App.test.tsx` — 3 files / 30 tests 통과. 새 13건은 불확실 400/502/504 이후 이탈·복귀·요청 전체 동일 재전송, 미제출 초안의 취소/메뉴/뒤로 복원, 확인된 400/422 validation 수정 후 새 commandId, 두 409 분기, 양쪽 부분 시간, logout·재로그인 후 늦은 성공과 초안 폐기, 제출 중 이탈 뒤 일정 변경에도 원래 수행일/commandId 보존을 포함한다. 앞선 StrictMode·지연 응답·자동 APPLIED 회귀도 유지했다.
- 최종 production source에서 `pnpm --filter @todorok/web build`, `git diff --check` 통과. backend와 실제 브라우저 재실행은 하지 않았다. 전체 작업07 완료 판정은 부모 리뷰에 남긴다.
