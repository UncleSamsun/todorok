# 작업 08 월집계 경계 검증 보고서

## 범위

- 기존 `ActivityPersistenceIntegrationTest`의 실제 PostgreSQL Testcontainers fixture와 `ActivityService`를 사용했다.
- 제품 코드는 변경하지 않았다.
- 수행일은 모두 집계 fixture를 구성하기 위해 명시했다. 이는 활동 수정 API 동작 검증이 아니다.
- 공유 브라우저/Compose runtime은 조작하지 않았고 planner 스키마를 조회하지 않았다.

## 추가 검증과 결과

| 경계 | fixture와 기대값 | 실제 결과 |
| --- | --- | --- |
| 윤년 서울 월 경계 및 동일 시각 UTC 표현 | 서울 표현 owner와 UTC 표현 owner를 분리하고 네 경계에 각각 `1/2/4/8분`을 배정했다. 각 owner에서 `2024-02-01T00:00+09:00` 및 `2024-02-29T23:59:59+09:00`에 해당하는 `1+4분`만 포함하고 바로 전/다음 시각은 제외: STUDY `2회 / 300초` | 두 owner 모두 `2회 / 300초`; 잘못된 UTC 달력 범위는 count가 같은 2회여도 `4+8분=720초`로 구별됨 |
| WORKOUT 여러 세트 | 한 활동의 `60초 + 120초`: `1회 / 180초` | `1회 / 180초` |
| STUDY 상태·시간·격리 | COMPLETED 2분 및 미입력, PARTIAL 3분 및 명시적 0, VOIDED 5분, 다른 owner의 7분, 같은 owner의 다른 type 포함: `2회 / 300초` | `2회 / 300초` |
| CLIMBING 상태·시간·격리 | COMPLETED 40초 및 미입력, PARTIAL 20초 및 명시적 0, VOIDED 50초, 다른 owner의 70초, 같은 owner의 다른 type 포함: `2회 / 60초` | `2회 / 60초` |
| 소수초 내림과 header 우선 | header 구간 `1.9초`와 detail `99초`를 함께 저장: `1회 / 1초` | `1회 / 1초` |
| 조회 불변성 | 보정 이력 활동과 Activity에 연결되지 않은 task reference가 함께 있는 owner에 대해 같은 월을 2회 조회하고 빈 월을 1회 조회. 매 조회 후 `activity_record`, owner의 전체 `task_reference`, 모든 typed detail/child, `activity_revision_history`, `outbox_event`의 전체 행 JSON을 조회 전 snapshot과 비교 | orphan을 포함한 task reference 2행 유지. 같은 월은 매번 `1회 / 180초`, 빈 월은 `0회 / 0초`; 모든 snapshot 동일 |

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

## 리뷰 후속 라운드 1

Important 2건에 대해 제품 코드 없이 테스트 오라클과 fixture만 보강했다.

- 서울/UTC 경계: 동일 owner 안의 네 경계 duration을 `1/2/4/8분`으로 서로 다르게 만들었다. 정상 서울 범위는 시작과 말일 fixture의 `1+4분=300초`, UTC 달력 경계를 잘못 적용한 격리 오라클은 말일과 다음 달 시작 fixture의 `4+8분=720초`였다. 따라서 기존처럼 count와 합계가 함께 상쇄되지 않는다.
- task reference: snapshot 조건을 Activity 연결 여부가 아닌 `task_reference.user_id=?`로 바꿨다. Activity 없는 orphan fixture를 넣어 owner task reference 2행을 확인했다. 정상 월집계 호출 전에 orphan의 `status/version`을 `PLANNED/0 → SKIPPED/1`로 바꾸면 snapshot 불일치가 검출되고, 원복하면 다시 일치함을 별도 detector control로 확인했다.

focused 실행 명령:

```powershell
.\gradlew.bat :services:activity-service:test --tests io.todorok.activity.persistence.ActivityPersistenceIntegrationTest.summarizesTheLeapYearSeoulMonthAcrossEquivalentOffsets --tests io.todorok.activity.persistence.ActivityPersistenceIntegrationTest.repeatedAndEmptyMonthlySummaryReadsLeaveOwnerStateUnchanged
```

- 명령 결과: exit code `0`, `BUILD SUCCESSFUL in 1m 22s`
- 실제 XML suite: `tests=2`, `failures=0`, `errors=0`, `skipped=0`, `time=44.48`
- XML testcase:
  - `summarizesTheLeapYearSeoulMonthAcrossEquivalentOffsets()` (`2.481s`)
  - `repeatedAndEmptyMonthlySummaryReadsLeaveOwnerStateUnchanged()` (`0.519s`)
- 제품 결함은 발견되지 않았고, 리뷰에서 확인된 두 테스트 사각지대를 제거했다.
