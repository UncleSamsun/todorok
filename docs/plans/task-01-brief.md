# 작업 01: 공통 오류와 trace ID

실행 위치는 `C:\workspace\todorok-worktrees\functional`이다. PRD §16.6의 runtime 오류 처리를 구현한다. Spring Boot 4.1.1, Java 25, Jackson 3, 기존 OpenAPI common-v1.yaml을 사용한다.

신규 libs/web-support 모듈에 TraceIdFilter, ApiFailure, ProblemResponseFactory, GlobalExceptionHandler를 작성한다. settings와 세 서비스 의존성/config에 연결한다. 외부 오류는 RFC9457 ProblemDetail 및 code, traceId, retryable, fieldErrors 계약을 준수한다. 서버 생성 UUID trace ID를 응답 X-Trace-Id와 MDC, 오류 body에 동일하게 넣고 요청 종료 후 MDC를 정리한다. 외부 trace header는 그대로 신뢰하지 않는다. 내부 SQL/stack/token/exception 메시지를 외부로 내보내지 않는다.

잘못된 JSON 400, validation 400, 없는 route 404, optimistic version 409, 업무 규칙 422, 일시 인프라 실패 503, 예상 못한 오류 500을 실제 HTTP handler 경로 테스트한다. 401/403은 factory 직렬화만 이번 작업에서 다루고 security filter는 인증 작업에서 연결한다. 실제 서비스가 구성을 사용하는 smoke 또는 테스트도 포함한다.

packages/api-client/src/problem.ts에 unknown JSON 오류 분류를 추가하고 기존 transport와 exports에 연결한다. 정상 ProblemDetails, network 실패, 비JSON 응답을 구분하며 mutation 자동 재시도는 하지 않는다. meaningful 테스트를 작성하고 실패 먼저 확인한 뒤 구현한다.

이번 작업은 기능 01만 구현한다. 하위 작업자를 생성하지 않는다. 원격 push/merge 금지. 파일 편집은 apply_patch, Windows 파일 작업은 PowerShell만 사용한다. project 기록·commit에 도구/모델 이름이나 co-author를 넣지 않는다. 변경 파일만 한글 conventional commit으로 커밋한다. docs/plans/functional-progress.md는 상위 담당자가 관리하므로 커밋에서 제외한다.

보고: docs/plans/task-01-report.md에 변경·테스트 명령·RED/GREEN 결과·우려를 기록한다. 보고 파일도 불필요한 도구 이름을 쓰지 않는다. 테스트는 새 모듈과 영향 서비스/패키지에 맞춰 실행한다. 완료 응답은 상태·commit·검증 요약·보고 경로만 전달한다.
