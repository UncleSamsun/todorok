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
