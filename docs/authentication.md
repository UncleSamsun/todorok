# 인증 운영과 로컬 실행

API는 `/api/planner/v1/auth/login`, `/auth/refresh`, `/auth/logout`을 제공한다. 공개 회원가입이나 사용자 생성 HTTP endpoint는 없다. access token은 RS256 서명, issuer `todorok`, audience `todorok-api`, 유효기간 10분이며 bearer 헤더로 전달한다. 검증 후 principal은 사용자 UUID다. activity와 notification에는 공개키만 배포한다.

## 키와 실행 환경

운영에서는 2048-bit 이상의 RSA 공개키(SPKI PEM)와 비밀키(PKCS8 PEM)를 secret mount로 제공한다. planner는 `AUTH_PRIVATE_KEY_LOCATION`, 모든 서비스는 `AUTH_PUBLIC_KEY_LOCATION`을 읽는다. 환경 변수 `AUTH_PRIVATE_KEY`, `AUTH_PUBLIC_KEY`에 PEM 값을 직접 전달할 수도 있다. 키 누락·잘못된 형식·키 쌍 불일치 시 HTTP 서비스가 기동되지 않는다. 개발용 키를 자동으로 대체 발급하지 않는다.

`AUTH_ISSUER`와 `AUTH_AUDIENCE`는 발급자와 검증 서비스에서 일치시킨다. `AUTH_ALLOWED_ORIGINS`는 스킴과 포트까지 포함한 공개 Origin을 쉼표로 구분한다. 예: `https://todorok.example`. path나 wildcard는 허용하지 않는다. cookie를 쓰는 세 endpoint는 Origin 누락·`null`·중복·비허용 값을 거부한다. 이 검증을 cookie 요청의 CSRF 방어로 사용하며, bearer가 없는 나머지 unsafe 요청에는 기본 CSRF 보호를 유지한다. 임의의 forwarded host는 allowlist에 반영하지 않는다.

로컬 HTTP 개발:

```powershell
./scripts/New-LocalAuthSecrets.ps1
Copy-Item -LiteralPath .env.example -Destination .env
docker compose --env-file .env -f infra/docker/compose.yml up -d --build
```

`.env`의 DB 비밀번호를 실행 환경에 맞게 설정한다. 기존 DB volume이면 기존 비밀번호를 사용한다. 키 생성 스크립트는 `.local/auth`에 키를 만들고 기존 파일을 덮어쓰지 않는다. `.local`은 Git과 Docker build context에서 제외한다. 해당 디렉터리는 로컬 사용자만 읽도록 관리한다. Compose는 비밀키를 planner에만 mount한다.

로컬 `.env.example`은 `SPRING_PROFILES_ACTIVE=local`, `AUTH_COOKIE_SECURE=false`, `AUTH_ALLOWED_ORIGINS=http://localhost`를 명시한다. 직접 다른 포트로 접속할 경우 Origin도 정확히 바꾼다. 운영은 HTTPS와 `AUTH_COOKIE_SECURE=true`를 사용하고 local profile을 해제한다. local profile 없이 Secure를 끄면 기동이 실패한다.

## 최초 계정

schema migration이 완료된 뒤 한 번 실행한다. 비밀번호는 명령행 인자나 저장소에 넣지 않고 환경 변수로 주입한다.

```powershell
$env:TODOROK_BOOTSTRAP_EMAIL = 'owner@example.com'
# TODOROK_BOOTSTRAP_PASSWORD는 secret manager 또는 현재 세션 환경에 주입한다.
docker compose --env-file .env -f infra/docker/compose.yml --profile bootstrap run --rm planner-bootstrap
```

컨테이너 없이 실행할 때는 `:services:planner-service:bootstrapBootJar`로 생성한 `*-bootstrap.jar`에 DB 접속 환경과 두 bootstrap 환경 변수를 제공한다. 이 command는 HTTP 서버·JWT 키가 필요 없다. 최초 계정 생성을 DB lock으로 직렬화하며 재실행은 기존 계정과 비밀번호를 유지한다. 비밀번호는 최소 12자, UTF-8 최대 72바이트이며 BCrypt cost 12로 저장한다.

## 세션과 제한

refresh는 32바이트 난수를 URL-safe opaque 값으로 발급하고 SHA-256 해시만 PostgreSQL에 보관한다. 매번 갱신한 토큰은 발급 시점부터 30일간 유효하다. family row를 잠근 다음 기존 token을 소비하고 새 token을 같은 transaction에서 저장한다. 사용된 토큰이 다시 들어오면 family를 폐기한 transaction을 commit한 뒤 401을 반환한다. 같은 토큰의 동시 refresh에서는 한 요청만 성공하고 나머지 요청의 replay 탐지로 승자가 받은 토큰도 폐기된다. 클라이언트는 탭 간 refresh를 직렬화해야 한다.

`todorok_refresh` cookie는 `Path=/api/planner/v1/auth; HttpOnly; Secure; SameSite=Lax`를 사용한다. 발급·회전·삭제 속성이 동일하며 응답은 저장하지 않는다. logout에는 유효한 bearer와 허용된 Origin이 필요하다. cookie가 가리키는 현재 사용자 family만 폐기한다. 이미 발급된 access JWT는 최대 10분 만료까지 유효하다.

로그인 요청은 계정별 분당 5회와 서버가 직접 관측한 원격 주소별 분당 30회로 제한한다. forwarded IP는 신뢰하지 않는다. Nginx 뒤에서는 서버 주소 제한이 공유되며 Nginx가 실제 클라이언트 주소별 인증 요청을 분당 30회와 burst 10으로 추가 제한한다. 429는 공통 Problem Details, `Retry-After: 60`, `X-Trace-Id`를 포함한다. PostgreSQL 제한 상태는 서비스 재시작 및 복제본 사이에서도 유지된다.

격리 Compose smoke는 임시 디렉터리에 매 실행마다 새 RSA 키를 만들고 종료 시 fixture와 격리 volume을 정리한다. 실운영 키나 고정 비밀키를 테스트 fixture로 사용하지 않는다.
