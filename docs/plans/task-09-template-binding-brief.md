# 작업09B1 템플릿 선택 승인과 일정 연결

기준 제품 `9fe9fe6`. 09A 정의 관리와 검증은 완료됐다. 요구는 `docs/superpowers/specs/2026-09-07-record-templates-design.md` §3,5–8,11–13이며 이 작업은 단계 B의 선택 승인·일정·projection 부분이다. 값 저장과 legacy 이행은 다음 09B2, 사용자 화면은 09C다. 전체09 완료로 표시하지 않는다.

## 구현할 결과

1. activity가 템플릿 선택을 승인하고 불변 binding을 저장한다. 동일 owner/requestId의 같은 요청은 같은 승인, 다른 요청은409. owner·domain·targetId/type·template/version을 검증하고 template 행 잠금으로 archive와 선후를 결정한다.
2. planner는 templateSelection이 있는 Task/series 생성에 commandId를 요구한다. 로컬 생성 command에 fingerprint·안정적인 targetId를 먼저 저장하고 잠금을 풀어 내부 승인 호출을 수행한다. 승인 후 Task/series/첫 회차/outbox/완료 응답을 한 로컬 트랜잭션으로 저장한다. 서비스 간 트랜잭션이 있다고 가정하지 않는다.
3. 승인 응답 유실·planner 최종 저장 실패·동시 같은 command·최종 응답 유실을 같은 command로 복구한다. 승인만 남아도 정상이며 다른 대상에 재사용하지 못한다. 내부 서비스 실패는503이고 Task/일정 outbox가 생기지 않는다.
4. Task/series에 immutable templateLink를 저장하고 기존 응답에 반환한다. 생성 후 제목·날짜·반복 변경으로 identity를 바꾸지 않는다. GENERAL은 templateSelection을 거부한다. 템플릿 없는 기존 생성은 호환한다.
5. 기존 일정·series는 보관된 템플릿 binding을 계속 사용하며, 다음 회차·이월·skip·과거 완료 복구에서도 연결이 유지된다. 새로운 Task/series 선택에는 보관 후 새 승인을 주지 않는다.
6. TaskScheduled/Changed/RolledOver와SeriesChanged의 필요한 v2 원본·생성 모델·fixture·소비를 연결한다. v1 소비를 유지하고 늦은v1/낮은version/다른binding이 기존 링크를 지우거나 바꾸지 못하게 한다. activity task_reference는 자기DB의binding을검증한다. 다른서비스DB직접조회금지.
7. owner Task의 record-template 조회를 추가해 현재 정의와 version을 반환한다. GET은 쓰지 않는다. projection 미도착은 명시적409/retryable, 다른owner는404. 연결없는Task는명시적link없음으로반환한다.

## 경로·인증 경계

실제 servlet context는 `/api/activity/v1`다. 내부 controller 상대 경로는 `/internal/template-selections`, 실제 내부 URL은 `/api/activity/v1/internal/template-selections`로 한다. 상세설계의 옛 `/internal/activity/v1/template-selections` 제안은 이 경로로 갱신한다. 공개 Activity·planner 경로를 바꾸지 않는다.

Nginx에서 내부 경로와 하위 경로를 일반 Activity proxy보다 먼저 차단한다. 앱에서도 별도 우선순위 SecurityFilterChain과 서비스용 decoder를 사용한다. 사용자 JWT가 내부 승인을 호출하거나 서비스 JWT가 공개 사용자 API를 호출할 수 없어야 한다.

서비스 서명 키는 기존 사용자 access-token 키와 분리한다. planner만 private key, activity만 public key를 가진다. 내부 JWT는 issuer/subject=`todorok-planner`, audience=`todorok-activity-internal`, scope=`template:select`, 짧은60초유효기간을검증한다. 요청별ownerId/requestId/targetType/targetId와본문fingerprint를서명된claim에결합하고수신본문과일치검증한다. URL·method도선택승인용으로한정한다. 토큰·키·본문개인정보를로그에남기지않는다. 기존 JWT 라이브러리·공통 ProblemDetails를재사용하되사용자principal변환과서비스principal을혼용하지않는다.

키 파일/환경 설정·Compose 전달·테스트 임시 키·runbook을 함께 다룬다. 설정 누락 시 내부 승인 경로는 fail-closed여야 하고 공개 API 인증을 약화시키면 안 된다. 기본 서비스 실행·마이그레이션·bootstrap 테스트의 기존 동작도 유지한다. 구체적인 설정명과 초기화 순서는 코드 변경 전에 짧게 부모에게 전달한다.

## 구현 경계·필수 보존

- 원본 internal OpenAPI와 생성 설정을 만들고 정확한generated interface/DTO로구현한다. 새Flyway번호를서비스별마지막이후에부여한다. 적용된migration을수정하지않는다.
- 링크의 표시 metadata는서버가생성하고선택당시version과일치해야한다. 필드개수제한을표시요약으로위장하지않으며전체정의는별도조회로복구가능해야한다.
- Activity의자유값/서버snapshot/legacy변환을이작업에서임시JSON으로구현하지않는다. 연결Task의새기록처리는09B2에서완성하므로그전에는무검증STANDARD기록으로조용히저장하지않도록명시적준비상태/거절을둔다. 09B2에서제거·완성할경계를보고서에적는다.
- 기존template없는Task·Activity·일반반복과28개기존검증을유지한다. 기존event/inbox/outbox를임의삭제하지않는다.

## 검증

- 실제HTTP:서비스인증성공,사용자JWT/틀린audience/scope/만료/본문claim변조/다른owner·domain·target거절,Nginx외부차단,기존공개API정상.
- 실제DB경합:선택과archive선후각각,같은request중복과payload변경,다른Task로binding재사용거절,template현재version변경충돌.
- 실제두서비스:승인뒤planner실패와응답유실복구,같은command경합중Task/series/첫회차/outbox한건,서비스중단503/부분Task없음.
- 실제이벤트왕복:Task·series링크전달,중복·역순v1/v2혼재·잘못된binding격리,다음회차/이월/skip/과거완료시링크유지와반복unique유지.
- 계약fixture/생성왕복/drift,영향서비스focused/마지막관련suite/build. 결과마다실행명령·실제수·누락검증을보고한다. 단위mock만으로서비스왕복완료를주장하지않는다.

## 수행·인계

유일한구현worker로작업하며하위agent/push/merge금지. PowerShell/apply_patch,본인파일만한글scopedcommit. 부모ledger/vault파일은편집하지않는다. 보고서 `docs/plans/task-09-template-binding-report.md`에인터페이스·설정·RED/GREEN·장애주입·남은09B2/C를명시한다. 아키텍처충돌이발견되면증거와선택지를부모에게전달하고묵시적인범위축소로해결하지않는다.
