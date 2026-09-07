# 작업 08 월집계 경계 검증 보고서

## 범위

- 기존 `ActivityPersistenceIntegrationTest`의 실제 PostgreSQL Testcontainers fixture와 `ActivityService`를 사용했다.
- 제품 코드는 변경하지 않았다.
- 수행일은 모두 집계 fixture를 구성하기 위해 명시했다. 이는 활동 수정 API 동작 검증이 아니다.
- 공유 브라우저/Compose runtime은 조작하지 않았고 planner 스키마를 조회하지 않았다.

## 추가 검증과 결과

| 경계 | fixture와 기대값 | 실제 결과 |
| --- | --- | --- |
| 윤년 서울 월 경계 및 동일 시각 UTC 표현 | 서울 표현 owner와 UTC 표현 owner를 분리했다. 각 owner에서 `2024-02-01T00:00+09:00` 및 `2024-02-29T23:59:59+09:00`에 해당하는 시각만 포함하고 바로 전/다음 시각은 제외: STUDY `2회 / 120초` | 두 owner 모두 `2회 / 120초` |
| WORKOUT 여러 세트 | 한 활동의 `60초 + 120초`: `1회 / 180초` | `1회 / 180초` |
| STUDY 상태·시간·격리 | COMPLETED 2분 및 미입력, PARTIAL 3분 및 명시적 0, VOIDED 5분, 다른 owner의 7분, 같은 owner의 다른 type 포함: `2회 / 300초` | `2회 / 300초` |
| CLIMBING 상태·시간·격리 | COMPLETED 40초 및 미입력, PARTIAL 20초 및 명시적 0, VOIDED 50초, 다른 owner의 70초, 같은 owner의 다른 type 포함: `2회 / 60초` | `2회 / 60초` |
| 소수초 내림과 header 우선 | header 구간 `1.9초`와 detail `99초`를 함께 저장: `1회 / 1초` | `1회 / 1초` |
| 조회 불변성 | 보정 이력이 있는 owner에 대해 같은 월을 2회 조회하고 빈 월을 1회 조회. 매 조회 후 `activity_record`, `task_reference`, 모든 typed detail/child, `activity_revision_history`, `outbox_event`의 전체 행 JSON을 조회 전 snapshot과 비교 | 같은 월은 매번 `1회 / 180초`, 빈 월은 `0회 / 0초`; 모든 snapshot 동일 |

현재 제품이 모든 경계 검증을 통과했으므로 결함 발견이 아니라 검증 누락 보강으로 판정한다.

## 실제 실행 증거

실행 명령:

```powershell
.\gradlew.bat :services:activity-service:test --tests io.todorok.activity.persistence.ActivityPersistenceIntegrationTest.summarizesTheLeapYearSeoulMonthAcrossEquivalentOffsets --tests io.todorok.activity.persistence.ActivityPersistenceIntegrationTest.sumsWorkoutSetsWithoutCountingTheActivityMoreThanOnce --tests io.todorok.activity.persistence.ActivityPersistenceIntegrationTest.summarizesStudyAndClimbingByStatusOwnerAndType --tests io.todorok.activity.persistence.ActivityPersistenceIntegrationTest.floorsFractionalHeaderSecondsAndDoesNotAddDetailDuration --tests io.todorok.activity.persistence.ActivityPersistenceIntegrationTest.repeatedAndEmptyMonthlySummaryReadsLeaveOwnerStateUnchanged
```

- 명령 결과: exit code `0`, `BUILD SUCCESSFUL in 1m 42s`
- XML: `services/activity-service/build/test-results/test/TEST-io.todorok.activity.persistence.ActivityPersistenceIntegrationTest.xml`
- XML suite: `tests=5`, `failures=0`, `errors=0`, `skipped=0`, `time=42.401`
- 개별 XML testcase:
  - `summarizesTheLeapYearSeoulMonthAcrossEquivalentOffsets()`
  - `sumsWorkoutSetsWithoutCountingTheActivityMoreThanOnce()`
  - `summarizesStudyAndClimbingByStatusOwnerAndType()`
  - `floorsFractionalHeaderSecondsAndDoesNotAddDetailDuration()`
  - `repeatedAndEmptyMonthlySummaryReadsLeaveOwnerStateUnchanged()`

## 자체 검토

- 월 시작 inclusive/월 종료 exclusive 조건을 1초 차이 fixture로 각각 깨뜨리면 경계 테스트가 실패한다.
- detail을 직접 join해 활동 수를 중복 계산하거나 세트 합계를 누락하면 WORKOUT 테스트가 실패한다.
- COMPLETED/PARTIAL/VOIDED 분기, null/0 처리, owner 또는 type 조건을 잘못 바꾸면 유형별 테스트가 실패한다.
- header와 detail을 더하거나 소수초를 반올림하면 interval 테스트가 실패한다.
- 월집계 조회가 행 추가·수정·삭제, revision/status 또는 JSON/outbox 내용을 바꾸면 조회 불변성 테스트가 실패한다.
- 조회 불변성 snapshot은 owner 연관 활동 테이블만 대상으로 하며 planner DB 접근은 포함하지 않는다.
- 첫 실제 실행 뒤 `git diff --check`는 출력 없이 exit code `0`이었다.
