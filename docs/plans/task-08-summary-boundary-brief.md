# 작업08 월집계 검증 누락 보강

기준 요구: `task-08-summary-ui-brief.md`의 실제 DB 월집계 및 read-only 검증. 기존 workout 1회/1,920초, Clock 현재월, HTTP 오류 검증을 반복하는 작업이 아니다. 아래 아직 직접 증명되지 않은 경계만 보강한다. UI 집중 수정이 끝난 후 순차 실행한다.

## 변경 범위

- `services/activity-service/src/test/java/io/todorok/activity/persistence/ActivityPersistenceIntegrationTest.java`의 기존 실제 PostgreSQL fixture를 재사용한다. 클래스가 커지면 같은 설정의 명확한 별도 월집계 테스트로 분리할 수 있으나 기존 경합 테스트를 재작성하지 않는다.
- 제품 코드 변경이 필요하면 재현 결과와 원인을 먼저 보고한다. mock SQL 응답으로 아래 DB 증거를 대체하지 않는다.
- 보고서 `docs/plans/task-08-summary-boundary-report.md`. 사용자 소스·다른 작업 문서·브라우저 runtime을 수정하지 않는다. 하위 에이전트, push, merge 금지.

## 필요한 테스트

1. 서울 월 포함 구간: 2024-02-01T00:00+09:00 포함, 그 직전 제외, 2024-02-29T23:59:59+09:00 포함, 2024-03-01T00:00+09:00 제외. 같은 시각의 UTC offset 표현도 동일 구간으로 계산한다. owner별 fixture를 분리해 count와 duration을 명시적으로 단언한다.
2. WORKOUT 여러 세트의 시간은 합산하고 Activity 건수는 한 번만 센다. 예: 60초+120초 세트 → 1회/180초. 기존 header 우선 테스트와 별개다.
3. STUDY와 CLIMBING 각각 COMPLETED/PARTIAL/VOIDED, 미입력/명시적0, 다른 owner 및 다른 type을 넣어 완료 수와 시간 합을 계산한다. PARTIAL은 시간만 포함하고 VOIDED는 양쪽 제외한다.
4. 실제 구간의 소수초 내림: 1.9초 구간은 1초. detail 시간이 함께 있더라도 더하지 않는다.
5. 조회 전후 해당 owner의 activity_record/typed detail/task_reference/revision history와 outbox 상태를 비교한다. 같은 월 반복 조회와 빈 월 조회가 행 수뿐 아니라 revision/status/내용을 바꾸지 않음을 단언한다. activity 서비스 계정에서 planner DB를 조회하지 않는다.

기존 `recordRequest`의 고정일 2026-09-07에 경계 테스트가 암묵적으로 종속되지 않게 fixture 날짜를 명시한다. DB fixture의 수행일 변경은 집계 테스트 데이터 구성임을 명시하고 실제 API 수정 검증으로 주장하지 않는다.

## 실행·보고

- 추가한 메서드만 `gradlew.bat :services:activity-service:test --tests <fully-qualified-method>` 형태로 실행한다. 현재 루트 Gradle 경로는 설정을 확인해 맞춘다.
- 보고서에 정확한 명령, 실제 test result XML의 tests/failures/errors/skipped, 경계별 기대값을 기록한다. 테스트가 현재 제품을 통과하면 검증 보강으로 보고하고 버그를 찾았다고 표현하지 않는다.
- `git diff --check` 후 본인 파일만 한글 커밋한다. 전체 작업08 완료 여부는 부모가 UI/리뷰와 합쳐 판정한다.
