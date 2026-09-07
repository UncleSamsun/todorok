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
