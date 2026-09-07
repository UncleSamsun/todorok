# 작업07 Activity 기록과 일정 동기화

기준 PRD9.2·10.2·16.4~16.5, 전체계획07. 작업 경로 `C:\workspace\todorok-worktrees\functional`. 작업06까지 실제 Task/series/note/API/UI와 authentication/session.fetch를 재사용한다. outbox/inbox 라이브러리는 이미 구현돼 있다.

## 데이터와 메시지 경계

- activity schema만으로 기록을 저장한다. planner schema 직접조회·service간 DB join 금지.
- TaskScheduled/TaskChanged를 activity TaskReference projection으로 수신한다. envelope userId·aggregateVersion과 taskId/type/date/status를 검증하고 낮은/동일 version이 최신 reference를 덮지 않는다. 삭제 tombstone을 유지한다.
- `InboxEventGuard.claim`은 projection/result 변경과 같은 transaction이다. 예외를 잡고 inbox만 commit하지 않는다.
- Activity header에 owner/taskId/type/performedAt/status/commandId/request fingerprint/revision/note를 저장한다. Detail은 유형별 명시한 record로 검증한다. 입력 필드가 없다는 이유만으로 실패시키지 않되 잘못된 타입/다른유형 필드는 거부한다.
- WORKOUT 세트, CLIMBING 라운드는 관계형 detail, STUDY값과 snapshot은JSONB 경계를 유지한다. 템플릿 편집UI는09지만 레코드 입력과 실제저장은 지금 연결한다.
- `(userId,commandId)` unique와 fingerprint로 같은command/payload 재시도는 같은결과, 같은command/다른payload는409. 같은Task에 비VOIDED COMPLETED가 중복 연결되지 않게 DB제약을 둔다.
- header/detail/outbox 저장은 한 transaction. PARTIAL은 Task완료/다음회차 생성 event를 발행하지 않는다.

## 검증과 처리

- principal owner와 TaskReference owner 일치. 없거나 동기화가 아직 안 된reference는 retryable409. 다른owner/삭제/유형불일치는 해당안전오류로 거부한다.
- Task가 동시에 삭제/skip되는 race를 consumer에서도 재확인한다. ActivityCompleted를 받았다고 DELETED/SKIPPED를 무조건COMPLETED로 덮지 않는다. 재조정이 필요한 충돌을 조회할수있는 명시 상태로 노출한다.
- planner completion consumer는 event중복·낮은version·다른Activity충돌을 처리하고 기존Task 하나에 완료요약을 연결한다. 신규별도 Task를 만들지 않는다.
- 완료일은 실제수행날짜(서울)로 표시한다. series다음회차는05 정책을 사용하고 같은 transaction과 lock순서를 유지한다.
- 필요 ack/rejection/sync조회 계약은 실제 상태를 확인할 수 있는 형태로 원본 OpenAPI/event부터 명시한다. '반영중'을 타이머 경과만으로 성공으로 바꾸지 않는다.

## 화면

- 기록형Task 체크→해당record폼→저장성공→오늘 해당일. 취소/닫기는Task미완료 유지.
- 네트워크 응답 유실 시 현재화면의 commandId와 입력을 유지한다. 수동재시도에서 새commandId로 중복기록을 만들지 않는다.
- Activity저장과 planner반영을 분리해 '반영중', 장기지연은 '기록됨 · 일정 반영 재시도'로 표시한다. 재시도는 기존상태재조회/정의된조정명령이며 Activity를 다시생성하지 않는다.
- 완료결과가 기존 Task행에 표시된다. 시간있으면timeblock, 없으면수행요약. 빠른추가와별도기록행 복제금지.
- 실제타입별기록 폼을 제공하되 프로그램진급·템플릿관리·타이머는09이후다.

## 테스트

1. 실제 PostgreSQL owner/type/status검증·fingerprint/idempotency·동시같은command·다른command같은Task·header/detail/outbox rollback.
2. 실제Kafka Task→reference→Activity완료→planner반영. Consumer정지중기록보존, 재시작동기화, duplicate/lowerVersion재전달 결과한번.
3. concurrentTask삭제/skip에서기록이숨겨지거나삭제Task복구되지않음. 충돌상태조회검증.
4. PARTIAL저장후Task미완료·회차미생성.
5. 실제브라우저 세유형기록 취소/저장, 응답유실 재시도, 기존행요약, 동기화지연표시.

원본계약→생성→서비스/UI순서. 신규migration번호는해당서비스최신다음. 하위작업자생성·원격push/merge금지. apply_patch/PowerShell 사용. 작업별한글commit, docs/plans/task-07-report.md에실제검증·미검증을구분한다. 부모progress/다른brief는수정하지않는다.
