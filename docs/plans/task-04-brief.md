# 작업04 일반 Task·달력·오늘 화면

기준은 PRD1.4.2 §8~10·19.2, DESIGN1.2.19 §7~8이다. 작업 위치 `C:\workspace\todorok-worktrees\functional`. 작업03 리뷰를 통과한 실제 AuthProvider·session·router를 재사용한다.

## 구현 범위

- planner의 기존 생성 TaskApi/CalendarApi interface를 구현한다. Task 상세 GET은 먼저 planner-v1.yaml에 추가하고 생성기로 재생성한다.
- JPA Task와 owner-scoped repository/service, version, `(user_id,scheduled_date)` index와 신규 Flyway V4를 추가한다(최신 migration 번호 재확인). entity를 그대로 응답하지 않는다.
- GENERAL 생성·수정·완료·reopen·soft delete, 네 유형의 저장/달력 요약을 지원한다. WORKOUT/STUDY/CLIMBING은 일반 complete endpoint로 완료할 수 없다.
- 로그인 principal(UUID)만 owner로 사용한다. 다른 사용자의 Task는404, 낡은 version409, 삭제된 Task404, 빈 제목/공백만 제목400. title 최대120.
- 생성과 변경은 TaskScheduled/TaskChanged envelope를 OutboxEventWriter로 같은 DB transaction에 저장한다. 기존 event contract와 aggregateVersion을 재사용한다.
- summary는 from/to 포함 최대42일, 빈 날짜도 반환하고 GENERAL/WORKOUT/STUDY/CLIMBING을 각각 한 번씩 고정 순서로 반환한다. DELETED 제외. 날짜별 total/completed와 각 유형 진행 수가 일치해야 한다.
- detail과 range는 별도 API/Query key. 읽기는 item별 API/DB 호출 없이 DTO projection을 사용한다. 테스트에서100개/1000개 데이터의 query 수가 상수인지 검사한다.

## 화면

- router의 임시 Today 화면을 `features/today/TodayPage.tsx`로 교체하고 WeekCalendar·MonthCalendar·TaskGroups·QuickAdd·TaskEditor로 분리한다.
- 모바일 상단 토도록22px·보조문구, 모바일 하단5탭/데스크톱sidebar, 반복 큰 오늘 제목 제거. DESIGN의 현재 규칙을 따른다.
- 주간 주차1열+날짜7열과 아래 진행7열을 맞춘다. 월 기준1~6주차, 주간7일 이동·월간1개월 이동·오늘복귀, 다음해/월말 경계를 검증한다.
- 선택 강조는 날짜에만. 월간 날짜 아래 짧은 완료 카테고리 막대, 주간 완료비율 색상, 화면 숫자는 숨겨도 접근성 이름에 실제 수를 포함한다.
- 새 세션 오늘, 주/월 선택만 기기저장. 네 Task 그룹은 빈 상태에서도 나타나고 각각+를 둔다.
- GENERAL의 +는 제목 우선 빠른추가, 옵션에서 날짜. 다른 그룹+는 유형을 지정한 같은 Task 생성 흐름으로 연결한다. 세부 기록 폼은07 이후에 추가한다.
- 기록형 체크는 도메인 route로 이동하며 저장 전 완료로 바꾸지 않는다. 가짜 저장/임의 fixture UI를 넣지 않는다.
- 390/420/820/1440px, 라이트·다크,44px hitarea. 좁은 폭은 내부scroll로 열폭을 유지하고 페이지 전체 가로넘침은 막는다.

## 검증

1. 실제 PostgreSQL HTTP Task 생성/수정/완료/reopen/delete, type별 일반완료 거부, owner·version·잘못된 날짜·range43일 거부.
2. Task와 outbox 원자성, 실패 transaction에서 outbox/Task가 모두 rollback.
3. summary/detail 정확성과 query 수 상수, 빈 날짜·네 그룹·부분완료비율.
4. calendar 순수 날짜계산의 월말·윤년·연말 경계. locale/host timezone에 따라 날짜가 하루 바뀌지 않음.
5. 실제 로그인 후 생성→새로고침→체크→다른날→오늘복귀→수정→삭제 브라우저 E2E. 실패응답에서 기존 UI 상태/입력을 보존한다.

반복·이월은 작업05, 메모는06이다. 기존 기능을 깨뜨리지 않고 TypeScript/java 테스트·생성 drift·프로덕션build를 검증한다. 원격 push/merge 금지. 하위 작업자 생성 금지. PowerShell 파일작업/apply_patch 편집. scoped 한글 commit과 docs/plans/task-04-report.md에 실제 RED/GREEN·브라우저·미검증 항목을 기록한다. parent progress/다른 brief는 변경하지 않는다.
