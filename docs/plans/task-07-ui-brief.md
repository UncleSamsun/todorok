# 작업07 기록 화면 연결

선행 서버 계약과 테스트가 확정된 뒤 착수한다. `task-07-backend-report.md`의 실제 client 호출·동기화 상태·실행 방법과 `task-07-brief.md`를 읽는다. 작업 위치 `C:\workspace\todorok-worktrees\functional`.

서버 구현 기준은 `aa65744`다. 실제 시간은 optional startedAt/endedAt 쌍이고, 수행일과 같은 서울 날짜·종료가 시작 이후인 조건을 사용한다. syncState는 PENDING/APPLIED/CONFLICT/NOT_REQUIRED다. 현재 수동 동작은 GET getActivity로 상태를 다시 확인하는 것이며 이벤트 재발행/충돌해결 명령이 아니다. UI문구도 '상태 다시 확인'처럼 실제 동작에 맞춘다.

## 책임과 파일

- `apps/web/src/features/activity/RecordPage.tsx`: query taskId와 도메인 유형으로 실제 Task 상세를 읽고 기록한다. server principal과 타입 오류를 숨기지 않는다.
- `WorkoutFields.tsx`, `StudyFields.tsx`, `ClimbingFields.tsx`: 확정된 generated detail type에 맞는 선택 입력. 숫자0·체크false와 미입력을 구분한다. 임의 JSON 편집창으로 폼을 대체하지 않는다.
- `RecordTimeFields.tsx`: 선택 수행일과 optional 시작/종료시간. DESIGN의 오전/오후·시·5분 선택을 사용한다. 미입력 시간을 생성시각으로 채우지 않는다.
- `SyncStatus.tsx`: 실제 getActivity 응답의 syncState를 조회해 완료·대기·충돌을 표시한다. 시간 경과만으로 성공 표시하지 않는다.
- 기존 domain route와 Today TaskGroups의 기록형 체크를 연결한다. `Configuration({fetchApi:session.fetch})`로 두 generated client에 인증·갱신을 공유한다.

## 사용자 흐름

1. 운동/공부/클라이밍 Task 체크 → 해당 type의 기록 폼. 취소·뒤로는 Task 미완료 유지.
2. 현재 입력을 검증해 commandId 한 개를 만들고 요청 snapshot을 유지한다. 동일 snapshot 재시도는 같은 commandId를 사용한다. 응답 유실 후 입력이 바뀌면 기존 요청 상태 확인 전 새 완료 기록을 생성하지 않는다.
   불확실한 저장 실패에서는 제출 snapshot을 보존하고 먼저 같은 요청으로 결과를 확인하도록 한다. 저장이 거절됐음이 명확한 validation 오류는 필드 수정이 가능해야 한다. 서버가 반환한 error code와 상태를 구분하고 모든 실패를 무조건 같은 처리로 덮지 않는다.
3. 기록 메모는 Activity.note로, Task 메모·DailyNote와 분리한다. 타입별 필드 미입력은 생략하고 고정 placeholder 값으로 저장하지 않는다.
4. 저장 성공 → 오늘의 실제 수행일을 선택해 돌아감. 기존 Task행에 요약을 표시하고 중복 행을 만들지 않는다. optional 실제 시간 쌍이 있을 때만 time block으로 표시한다.
5. PENDING은 반영중, APPLIED는 실제 planner반영 확인 후 완료 표시. CONFLICT는 이유와 서버가 지원하는 수동확인/조정 동작을 표시한다. NOT_REQUIRED/PARTIAL은 완료체크를 채우지 않는다.
6. 저장 전에 나가기·실패는 폼 입력 보존. 실패해도 임의 완료로 바꾸지 않는다. 수동 재시도는 서버 보고에 정의된 의미를 따른다.

## 범위

- 도메인 메인에는 실제 오늘 Task 목록을 읽어 같은 체크 흐름으로 들어갈 수 있게 한다. 월집계/수정/void/지난기록 상세 기능은08에서 연결하지만 가짜 통계는 표시하지 않는다.
- 템플릿 편집·프로그램·타이머는09 이후다. 이번 폼은 현재 지원된 타입별 기본 기록을 실제 저장할 수 있어야 한다.
- 코드에서 새 backend 동기화 규칙을 추측하지 않는다. 계약 불일치는 구체적인 요청/응답과 함께 조율 담당자에게 전달한다.

## 검증

- 선택 입력의0/false/빈값, 취소 시 저장0회, 실패초안보존, 동일commandId재전송, 성공복귀일, 시간미입력시timeblock없음.
- PENDING→APPLIED/CONFLICT/NOT_REQUIRED의 실제응답에 따른 상태. logout·다른Task전환 후늦은응답이새화면을덮지않음.
- 기존 로그인·calendar·메모 회귀와 production build.
- 새로운 두서비스+Kafka환경에서 실제브라우저3유형저장·취소·반영확인. mockresponse만으로서비스연동완료를주장하지않는다. 일반 pointer 조작과 정확한 요청/DB결과를 확인한다.

apply_patch/PowerShell 사용, 하위작업자생성·원격push/merge 금지. 부모 progress·다른brief·backend는 수정하지 않는다. 필요한 backend 수정은 증거와 함께 조율한다. scoped 한글commit과 docs/plans/task-07-ui-report.md에 실제실행결과를 남긴다.
