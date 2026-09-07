# 작업05 반복 일정·이월·건너뜀

작업 경로 `C:\workspace\todorok-worktrees\functional`. 작업04의 실제 Task service/entity/API를 재사용한다. 전체 계획05, PRD9.1·10.2~10.4가 기준이다. 새 규칙은 구현 전에 PRD에 다음 내용으로 명시한다.

## 규칙 결정

- 반복 anchor는 series 시작일이며 이월된 예정일로 변경하지 않는다.
- DAILY는 시작일 기준 interval 일, WEEKLY는 일요일 시작 주 기준 interval 주와 선택 요일, MONTHLY는 시작월 기준 interval 월과 선택 일자를 사용한다.
- 없는 월 일자(예:2월31일)는 해당 월을 건너뛴다. 임의 월말 보정하지 않는다. endDate는 포함이고 다음 발생일이 endDate 뒤이면 생성하지 않는다.
- 활성 PLANNED는 series별 하나. 완료·skip 후 다음 규칙 발생일 하나를 만들며 이미 활성 회차가 있으면 유지한다. 일회성에는 다음 회차가 없다.
- 일반 완료/skip을 reopen할 때 다른 활성 회차가 있으면409를 반환한다. 자동으로 수행된 기록/미래회차를 지우지 않는다. 활동void는 작업08 정책과 연결한다.
- archive는 이후 회차 생성을 중단한다. 기존 활성 Task를 같이 삭제하지 않는다. 사용자가 원하면 Task 삭제를 별도로 한다.
- 오늘 진입에서 명시적 rollover command를 실행한 뒤 query한다. 과거 날짜를 보는 GET이나 자정 scheduler는 변경을 실행하지 않는다.
- rollover는 overdue PLANNED의 scheduledDate를 서울 오늘로 바꾸고 기존 taskId를 유지한다. COMPLETED/SKIPPED/DELETED는 이월하지 않는다. 여러 미수행 회차를 생성하지 않는다.
- 완료 후 계산한 다음 날짜가 과거이면 한 회차만 만들어 오늘로 이동한다. 미래이면 미래 유지. 기존활성/종료/PARTIAL에 대한 PRD10.4 네 분기를 지킨다.

## 파일과 API

- planner `series/TaskSeries`, `RecurrenceRule`, `NextOccurrencePolicy`, `SeriesService`, `task/RolloverService`; repository/service를 기능별로 분리한다.
- 신규 migration은 작업04 이후 번호. series FK와 PLANNED partial unique index, 원래 반복 anchor/occurrence date와 예정일을 구분할 필드를 추가한다. 기존 migration 수정 금지.
- planner-v1.yaml에 series 생성/수정/archive·skip·rollover command 계약을 먼저 추가한다. owner는 principal UUID, version은 optimistic check다. generated interface/client는 생성기로 갱신한다.
- outbox 변경 event는 Task와 같은 transaction. 연산 lock 순서는 series → task로 통일하고 오류 rollback에서 Task/outbox/다음회차가 모두 되돌아가야 한다.
- Clock을 주입하고 LocalDate.now(clock.withZone(Asia/Seoul))를 서버 기준으로 사용한다. client-domain 날짜순수함수는 브라우저 전역 없이 구현한다.
- UI 빠른 추가의 반복 옵션, 건너뜀/reopen·반복중단을 연결한다. today 진입시 rollover 성공 후 calendar/detail을 갱신한다. 실패는 조용히 성공으로 표시하지 않는다.

## 테스트

1. 순수 날짜 정책: DAILY interval2, WEEKLY 여러요일과 주경계, MONTHLY31일2월skip, 윤년2월29, 연말, endDate당일/직후.
2. 시작 anchor가 이월돼도 다음 규칙이 바뀌지 않음. 월요일반복의 지난완료를 수요일입력하면 다음월요일, 기존활성 있으면그대로.
3. 같은 이월command 두번, 동시에 두개 호출→taskId하나/날짜오늘/event불필요중복없음.
4. 동시 complete/skip/reopen 및 partial unique 충돌→정합한1회차, 409와rollback.
5. archived/endDate뒤/PARTIAL/일회성에서 회차 미생성.
6. 실제브라우저 반복추가→완료→다음회차→skip→지난날과오늘확인, 서버 고정clock 테스트로 이월 재현. UI시간변조만으로 서버오늘을 검증하지 않는다.

중요 날짜·상태 규칙 branch100%는 경계표와 coverage로 확인한다. 전체percent gate 설정을 변경하는 경우 생성물 제외는 명시하고 실제 policy를 제외하지 않는다. 원격push/merge 금지. 하위작업자 생성금지. apply_patch/PowerShell 사용. scoped 한글commit과 docs/plans/task-05-report.md에 테스트 실제결과를 기록한다.
