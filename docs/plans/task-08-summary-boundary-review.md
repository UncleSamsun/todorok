# 작업08 월집계 DB 검증 리뷰

범위 `3e8fe6d..9424f97`. 판정: 수정 필요. 실제 실행 XML5/0/0/0은 확인됐으나 다음 검증 사각지대가 남는다. 제품 결함 지적이 아니라 요구를 증명하는 테스트의 결함이다.

후속 판정: `dd544ea`의 보완 범위를 재리뷰해 두 건 모두 해결됐음을 확인했다. 새 Critical/Important 문제는 없었다. 정상 서울 범위 300초와 잘못된 UTC 범위 720초를 구별하고, 소유자의 전체 Task reference 및 Activity에 연결되지 않은 fixture의 변경 탐지를 확인했다. 실제 집중 테스트 XML의 tests/failures/errors/skipped = 2/0/0/0 증거와 합쳐 검증 보강 완료로 판정한다.

## Important 1 — 서울/UTC 경계의 합계 상쇄

`ActivityPersistenceIntegrationTest.java:104,130–133`: 네 경계 fixture가 모두1분이고 합계2회/120초만 단언한다. 잘못된UTC2월집계가서울2월시작을제외하고서울3월시작을대신포함해도같은합계라통과한다.

수정: 각시각에다른시간을배정하거나독립owner로포함/제외를각각단언한다. UTC월로잘못바꾼경우테스트가실제로실패하는것을검증하고제품코드는정상서울구간으로복원된상태에서GREEN을확인한다. mutation검증은uncommitted제품작업과겹치지않게조율한다.

## Important 2 — 연결되지 않은 Task reference 누락

`ActivityPersistenceIntegrationTest.java:372`: snapshot이Activity에연결된task_reference행만본다. Activity없는기존reference변경이나새후속reference생성을놓친다.

수정: `task_reference`는 `user_id=?`로owner전체를조회한다. Activity없는Taskreference fixture를추가하고정상조회가그행도유지함을단언한다. 가능한경우snapshot의변경탐지를추가fixture변형으로확인하되정상조회검증과구별한다.

## 라운드 1 실행 범위

- 위두테스트/fixture만수정하고focused두메서드실제PostgreSQL실행. 기존나머지3개를무조건다시반복할필요없다.
- 테스트보고서의기대값과자체검토주장도수정해실제증거와일치시킨다.
- 제품코드/활성UI작업수정금지. 부모가UI구현작업종료를확인한뒤유일한구현worker로순차실행한다.
