# 작업06 메모 저장과 화면

기준 PRD11과 DESIGN8.5. 작업 경로 `C:\workspace\todorok-worktrees\functional`. 선행 Task/series 소유권과 session.fetch, calendar/detail Query 경계를 재사용한다.

## 저장 계약

- 날짜별 메모는 planner 소유 DailyNote, `(user_id,date)` unique, 최대20,000자와 optimistic version이다. 메모 없음과 내용 빈 문자열을 구분한다.
- planner OpenAPI에 GET/PATCH `/notes/{date}`를 추가한다. 최초 쓰기는 expectedVersion=null, 기존 쓰기는 읽은 version을 요구한다. 이미 존재하는데 null이거나 version이 다르면409로 응답한다.
- GET은 메모 없음에도 date/content/nullable version을 명시적으로 반환해 화면이 처음 작성할 수 있게 한다. 날짜는ISO local date, owner는JWT principal.
- PostgreSQL 신규 migration은 실제 최신 이후 번호. 기존 migration 수정 금지. concurrent 최초 INSERT는 unique 충돌을409로 안정적으로 변환한다.
- Task/series 수행 전 메모는 해당 entity 필드로, 완료 기록 메모는 작업07 Activity에 귀속한다. 하나의 공통 note row로 세 종류를 섞지 않는다. Task contract 확장은 원본과 생성물로 동기화한다.

## 화면

- 선택 날짜의 Task 목록 아래 한 줄 미리보기, 클릭하면 같은 화면에서 확장 편집.
- 500ms debounce PATCH. 입력 buffer/dirty revision/request date/request version을 분리한다. 날짜 변경 후 과거 요청이 새 날짜 buffer를 덮지 않는다.
- 응답 성공이 현재 입력 revision과 일치할 때만 저장됨 표시. 늦은 성공으로 저장되지 않은 최신 입력을 저장됐다고 표시하지 않는다.
- 실패409/network는 초안을 유지하고 저장실패·수동재시도를 표시한다. server 최신version을 다시 읽는 과정에서도 내초안을 버리지 않는다. 자동 merge/직렬queue/offline저장은 만들지 않는다.
- 다른 날짜로 이동하는 동안 저장중인 날짜의 결과는 해당 Query cache만 갱신한다. logout에서는 cache/초안을 지워 계정 간 누출을 막는다.

## 필수 검증

- 실제 PostgreSQL HTTP: 없음·최초작성·빈내용·20,000/20,001자·다른owner·낡은version·동시최초쓰기·잘못된날짜.
- component fake timer:499ms미전송/500ms전송, 연속입력최신내용, 응답역전·날짜전환·409·network·초안보존·재시도.
- 실제브라우저 로그인→날짜별메모저장→다른날→복귀→새로고침→내용유지와 실패시입력보존.
- Task 메모 수정·series 메모의 다음 회차 유지 옵션이 동작하고 DailyNote와 분리됨.

공유 validation에는 순수 길이/입력 검증만 둔다. 하위작업자 생성과 원격push/merge 금지. apply_patch/PowerShell 사용. 변경파일만 한글commit, docs/plans/task-06-report.md에 실제 테스트와 후속Activity메모 연결 범위를 기록한다.
