# 인증 구현 선행 결정

기준: PRD 1.4.2 §16.6·18. 실행 순서는 공통 오류 리뷰 완료 다음이다.

## 확인된 기존 계약

- `/auth/login`과 `/auth/refresh`는 공개 operation, logout은 bearer 인증을 요구한다.
- 응답 SessionResponse는 기존 생성 모델을 유지한다. cookie는 body 필드가 아닌 Set-Cookie로 전달한다.
- planner와 activity는 Nginx 외부 prefix를 보존한다. notification HTTP는 내부망에서 사용한다.

## 구현 결정

- refresh cookie의 Path는 `/api/planner/v1/auth`, SameSite=Lax, HttpOnly, 운영 Secure다. 발급과 삭제에 같은 속성을 사용한다.
- cookie 쓰기 endpoint는 구성된 공개 Origin allowlist와 대조한다. 임의 Host/X-Forwarded-Host로 allowlist를 만들지 않는다.
- access token은 RS256·10분, refresh는 opaque random·30일로 한다. 검증할 issuer/audience는 서비스별 명시 설정이다.
- bootstrap은 별도 명령 실행으로만 사용자 생성, 재실행은 기존 사용자를 변경하지 않는다. 기본 비밀번호나 임시 공개 endpoint를 만들지 않는다.
- session family row를 잠그고 token hash를 검증한다. rotation은 기존 token used 상태와 새 token 저장이 같은 transaction이다.
- 재사용 탐지 후 family 폐기는 오류 응답과 함께 반드시 commit돼야 한다. 폐기 update 뒤 rollback되는 예외를 던지는 구현은 금지한다.
- 일반 resource API의 principal만 사용자 ID로 사용한다. 다른 사용자 소유 row는 404, 인증 자체 실패는 401이다.
- login 제한은 principal 식별자와 신뢰 가능한 원격 주소를 기준으로 한다. 제한 응답도 공통 ProblemDetails와 429/Retry-After를 유지한다.
- local HTTP 테스트에서만 Secure cookie를 끌 수 있다. 운영 기본값은 Secure이며 잘못된 secret 설정에서 조용히 개발용 키로 기동하지 않는다.

## 검증 우선순위

1. 원본 OpenAPI의 cookie·429·Origin 거절 응답 명세.
2. PostgreSQL bootstrap 멱등·rotation 동시성·재사용 family 폐기 유지.
3. MVC security chain 401/403과 공통 trace ID.
4. 서명/issuer/audience/만료 거절.
5. HTTP login→refresh→logout→재갱신 거절.

클라이언트 탭 간 refresh 경쟁은 작업 03에서 lock과 세션 갱신 결과 공유로 제어한다. 서버에서 replay 허용 grace window를 추가해 보안 규칙을 완화하지 않는다.
