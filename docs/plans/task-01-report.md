# 작업 01 구현 보고

## 변경

- `libs/web-support` 공통 모듈을 추가하고 서버 생성 UUID trace ID를 응답 헤더, MDC, 오류 본문에 동일하게 연결했다. 외부 `X-Trace-Id` 값은 사용하지 않으며 요청 종료 시 MDC를 정리한다.
- RFC 9457 `ProblemDetail` 기반 공통 factory와 전역 handler를 추가했다. 잘못된 JSON, validation, route 없음, query/path 변환 및 필수값 누락, 지원하지 않는 method/media type, optimistic 충돌, 업무 규칙, 일시적 데이터 접근 실패, 예상하지 못한 오류를 안정적인 외부 계약으로 변환한다.
- 내부 예외 메시지는 외부 detail로 사용하지 않는다. 401/403은 동일 factory 계약으로 직렬화하되 security filter 연결은 포함하지 않았다.
- 세 서비스가 `libs/web-support`를 의존하도록 연결하고 planner 서비스 classpath에서 auto-configuration이 활성화되는 smoke test를 추가했다.
- API client에 알 수 없는 오류를 Problem Details, 네트워크 실패, 비JSON 응답으로 분류하는 기능과 fetch transport를 추가했다. mutation 실패를 자동 재시도하지 않는다.

## 테스트와 결과

### RED

- `.\gradlew.bat :libs:web-support:test --tests '*invalidOrMissingQueryValuesAre400Problems' --tests '*unsupportedMethodAndMediaTypeRemain4xxProblems' --no-daemon --no-configuration-cache`
  - 결과: 2개 실패. 공통 handler가 해당 요청을 500으로 처리해 필요한 4xx 분기가 없음을 확인했다.
- `pnpm --filter @todorok/api-client test`
  - 결과: `problem.ts` 모듈 부재로 실패해 오류 분류 기능이 아직 없음을 확인했다.

### GREEN

- `.\gradlew.bat :libs:web-support:test --rerun-tasks --no-daemon --no-configuration-cache`
  - 결과: 성공. 실제 MockMvc handler/filter 경로의 12개 테스트가 통과했다.
- `.\gradlew.bat :libs:web-support:test :services:planner-service:test --tests '*WebSupportConfigurationTest' :services:activity-service:compileJava :services:notification-service:compileJava --rerun-tasks --no-daemon --no-configuration-cache`
  - 결과: 성공. 공통 모듈, planner smoke, activity/notification 컴파일을 검증했다.
- `pnpm --filter @todorok/api-client test`
  - 결과: 성공. 런타임 3개 테스트와 TypeScript 타입 검사가 통과했다.

## 우려와 후속 경계

- 401/403 security entry point와 access denied handler 연결은 인증 작업 범위다.
- 다른 인프라 client가 Spring Data 예외 계층을 사용하지 않으면 해당 adapter에서 안전한 `ApiFailure`로 변환해야 한다.

## 리뷰 수정 1차

- 비동기·오류 dispatch에서도 최초 요청에 생성한 trace ID를 request attribute에서 복원하도록 filter를 보완했다. 각 dispatch 종료 시 기존 MDC 값을 원상 복구한다.
- 실제 `Callable` 실패를 `asyncDispatch`하는 HTTP 테스트와 기존 MDC 복원 테스트를 추가했다.
- API client 검증을 `common-v1.yaml`과 맞춰 추가 속성 금지, 오류 코드 패턴, 문자열 최소 길이, 정수 status, 중첩 field error, URI-reference 형식을 검사하도록 강화했다. 빈 title과 상대 URI-reference처럼 계약이 허용하는 경계는 유지했다.

### RED

- `.\gradlew.bat :libs:web-support:test --tests '*asyncFailureUsesTheOriginalTraceIdInHeaderAndBody' --no-daemon --no-configuration-cache`
  - 결과: 1개 실패. async handler 본문의 trace ID가 `unavailable`로 직렬화되어 최초 응답 헤더와 달랐다.
- `pnpm --filter @todorok/api-client test`
  - 결과: schema near-miss 테스트 1개 실패. 추가 속성이 있는 객체가 정상 Problem Details로 분류됐다.

### GREEN

- `.\gradlew.bat :libs:web-support:test --rerun-tasks --no-daemon --no-configuration-cache`
  - 결과: 성공. 비동기 dispatch와 MDC 복원을 포함한 전체 공통 HTTP 테스트가 통과했다.
- `pnpm --filter @todorok/api-client test`
  - 결과: 성공. 런타임 5개 테스트와 TypeScript 타입 검사가 통과했다.

## 리뷰 수정 2차

- MVC async support에 `CallableProcessingInterceptor`를 등록해 `Callable` 실행 worker에 최초 trace ID를 복원하고 처리 직후 기존 MDC를 원상 복구한다.
- 단일 worker executor를 사용하는 실제 async HTTP 테스트에서 `Callable` 내부 MDC, async 오류 응답의 header/body 일치, 같은 worker의 후속 작업에서 MDC가 제거됐는지 검증한다.
- URI-reference 검사에서 RFC 3986이 허용하지 않는 raw backslash, caret, pipe, brace와 기타 금지 문자를 URL 정규화 전에 거부하고 near-miss를 추가했다.
- `DeferredResult` 값을 만드는 임의의 외부 producer thread는 Spring MVC `Callable` 실행 경계 밖이므로 자동 MDC 전파를 보장하지 않는다. 해당 코드는 명시적인 context 전파 wrapper를 사용해야 한다.

### RED

- `.\gradlew.bat :libs:web-support:test --tests '*asyncFailureUsesTheOriginalTraceIdInHeaderAndBody' --no-daemon --no-configuration-cache`
  - 결과: 1개 실패. `Callable` worker 내부 MDC가 비어 있었다.
- `pnpm --filter @todorok/api-client test`
  - 결과: URI-reference near-miss가 정상 Problem Details로 분류되어 1개 실패했다.

### GREEN

- `.\gradlew.bat :libs:web-support:test --tests '*asyncFailureUsesTheOriginalTraceIdInHeaderAndBody' --rerun-tasks --no-daemon --no-configuration-cache`
  - 결과: 성공. worker 내부 trace와 처리 후 cleanup, async 응답 trace 일치를 검증했다.
- `pnpm --filter @todorok/api-client test`
  - 결과: 성공. 런타임 5개 테스트와 TypeScript 타입 검사가 통과했다.

## 리뷰 수정 3차

- RFC 3986 URI-reference는 IRI가 아니므로 URL 정규화 전에 raw 비ASCII 문자를 거부한다. 같은 UTF-8 값의 percent-encoded URI-reference는 허용한다.
- ASCII 검사는 RFC 3986의 unreserved, reserved, 유효한 percent-encoding을 남기고 raw 공백·제어 문자와 금지 문자를 거부하는 범위임을 재확인했다.

### RED/GREEN

- `pnpm --filter @todorok/api-client test`
  - RED: raw 한글 URI-reference가 정상 문제 응답으로 분류되어 1개 실패했다.
  - GREEN: 런타임 5개 테스트와 TypeScript 타입 검사가 통과했다.
