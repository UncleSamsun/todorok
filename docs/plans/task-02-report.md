# 작업 02 인증 구현 결과

## 구현

- 최초 계정 전용 비HTTP bootstrap command와 실행 jar/Compose profile을 추가했다. DB advisory lock으로 동시 최초 실행을 직렬화하며 재실행은 기존 계정과 BCrypt cost 12 비밀번호 해시를 유지한다.
- planner가 RS256 access JWT를 10분간 발급한다. 세 서비스는 공개키, RS256 허용 목록, issuer, audience, expiry, UUID subject를 검증한다. private key는 planner만 사용하며 누락·불일치 설정에서 HTTP 서버가 기동되지 않는다.
- 32바이트 난수 refresh의 SHA-256 해시·family·만료·소비 상태를 PostgreSQL에 저장한다. family lock 획득 후 최신 소비 상태를 읽고 회전한다. replay는 폐기 transaction이 정상 commit된 이후 401로 변환한다.
- cookie 발급·회전·삭제는 같은 Path와 HttpOnly/Secure/SameSite=Lax 속성을 사용한다. local profile만 Secure=false를 허용한다. auth endpoint의 엄격한 Origin 검증을 CSRF 방어로 사용하고 나머지 unsafe 요청은 bearer가 있을 때만 기본 CSRF 검사에서 제외한다.
- PostgreSQL 로그인 제한과 Nginx 클라이언트 주소 제한을 추가했다. 401/403/429는 공통 Problem Details와 trace를 유지한다. 429에는 Retry-After를 전달한다.
- 원본 OpenAPI의 cookie·Origin·403·429 명세와 생성된 Java/TypeScript 산출물을 동기화했다.
- Compose 공개키/비밀키 secret mount, 로컬 키 생성 스크립트, 임시 smoke 키 fixture, bootstrap 실행 경로를 연결했다. `.local`은 Git과 Docker build context에서 제외한다. 사용법은 `docs/authentication.md`에 기록했다.

## 실제 RED 관찰

1. `.\gradlew.bat :services:planner-service:test --tests '*AuthenticationIntegrationTest' --no-daemon --no-configuration-cache`
   - 첫 실행은 기존 persistence sample의 test migration을 fixture에 포함하지 않아 context startup에서 실패했다. 이 실행은 인증 RED 증거로 계산하지 않았다.
   - fixture 수정 후 1개 실패: `expected: 401 but was: 404`. 인증 endpoint가 없는 상태에서 실제 PostgreSQL과 HTTP를 사용했다.
2. `.\gradlew.bat :services:planner-service:test --tests '*SessionSecurityIntegrationTest' --no-daemon --no-configuration-cache`
   - 11개 중 1개 실패. 누락된 Origin이 immutable set의 `contains(null)`에 들어가 NPE를 만들고 error dispatch에서 401이 반환됐다. 명시적 null 처리로 403을 반환하도록 수정했다. 비허용 Origin이 기본 CORS 응답으로 빠지지 않도록 필터를 CORS 앞에 두었다.
3. `node --test scripts/compose-smoke.test.mjs`
   - 3개 중 1개 실패: `smoke.provisionSmokeAuth is not a function`. 임시 RSA 키 fixture를 구현한 뒤 동일 명령이 3개 모두 통과했다.

## 실제 GREEN 검증

```powershell
.\gradlew.bat :services:planner-service:test --tests '*SessionSecurityIntegrationTest' --tests '*PlannerPersistenceIntegrationTest' --tests '*PlannerMigrationApplicationTest' :services:activity-service:test --tests '*ActivitySecurityIntegrationTest' --tests '*ActivityPersistenceIntegrationTest' --tests '*ActivityMigrationApplicationTest' :services:notification-service:test --tests '*NotificationSecurityIntegrationTest' --tests '*NotificationPersistenceIntegrationTest' --tests '*NotificationMigrationApplicationTest' :services:planner-service:bootstrapBootJar --no-daemon --no-configuration-cache
```

결과: `BUILD SUCCESSFUL in 3m`, 27개 task 중 14개 실행. 테스트 30개, 실패 0개:

- planner 16개: auth 11개, persistence 4개, migration 1개.
- activity 7개: 실제 HTTP 공개키 검증 2개, persistence 4개, migration 1개.
- notification 7개: 실제 HTTP 공개키 검증 2개, persistence 4개, migration 1개.

검증 내용: bootstrap command 실제 2회 실행·기존 해시 유지, HTTP login과 UUID principal, RS256 및 10분 만료, refresh hash-only 저장, rotation, replay 폐기 commit 유지, 다른 family 보존, 두 HTTP 요청 동시 refresh, 만료·잘못된·누락 refresh, bearer 없는 logout 거절, 동일 속성 cookie 삭제, 다른 사용자 family 보존, Origin 누락·null·위조·유사 도메인 거절, 잘못된 서명·issuer·audience·expiry·algorithm·subject·만료 누락, DB 로그인 제한과 window 복구. activity/notification 테스트 환경에는 비밀키가 없다. 기존 migration은 JWT 키 없이 비HTTP로 실행됐다.

추가 실행:

- `node --test scripts/compose-smoke.test.mjs scripts/persistence-compose.test.mjs scripts/openapi-contract.test.mjs scripts/api-routing.test.mjs`: 8개 통과.
- `node --test scripts/auth-nginx.integration.test.mjs`: 실제 격리 Nginx에서 1개 통과. 인증 burst를 소진시킨 뒤 429의 Problem Details·Retry-After·trace 일치를 검증하고 컨테이너를 제거했다.
- `docker compose --env-file .env.example -f infra/docker/compose.yml -f infra/docker/compose.smoke.yml config --quiet`: 종료 코드 0.
- `git diff --check`: 종료 코드 0. 생성 목록 파일의 CRLF 정규화 안내만 있었다.
- `.\gradlew.bat :libs:web-support:test --no-daemon --no-configuration-cache`: `BUILD SUCCESSFUL in 39s`. 공통 오류 라이브러리 회귀 검사를 통과했다.
- `pnpm --filter @todorok/api-client build`: 종료 코드 0. 생성 클라이언트의 TypeScript 빌드를 통과했다.

## 범위와 남은 증거

- 작업 03 로그인 UI, 탭 간 refresh lock은 구현하지 않았다.
- 이번 작업에서 전체 Compose를 새 이미지로 build/up하는 smoke 전체 과정은 실행하지 않았다. Compose 연결 검증과 임시 키 fixture, 실제 Nginx 요청 제한을 개별 검증했다.
- branch coverage 계측 수치는 산출하지 않았다. 위 결과는 실행한 동작 테스트의 통과 증거다.
- logout은 refresh family를 폐기하며 이미 발급한 access JWT는 최대 10분 만료까지 유효하다.
- 서버는 forwarded IP를 신뢰하지 않아 Nginx 뒤 원격 주소별 로그인 제한은 공유된다. Nginx에서 실제 클라이언트 주소별 제한을 함께 적용한다.

## 커밋

구현 커밋: `d00d84d` — `feat(auth): 최초 계정과 회전형 세션 인증 구현`.
이 보고서는 별도 문서 커밋으로 기록한다. 원격 push/merge는 실행하지 않았다.

## 조립 검증 후속 수정

전체 Compose build에서 웹 소비자의 TypeScript 검사 실패가 발견됐다. API client는 TypeScript source를 export하고 내부에서 `.ts` 경로를 사용하지만, `apps/web/tsconfig.json`에는 이를 허용하는 옵션이 없었다. `noEmit`과 Bundler 해석을 사용하는 웹 설정에 `allowImportingTsExtensions=true`를 추가했다. 기존 타입 검사는 그대로 유지한다.

- RED: `pnpm build:web` — `transport.ts(1,29)`, `transport.ts(31,47)`의 TS5097로 종료 코드 1.
- GREEN: `pnpm build:web` — TypeScript 검사와 production/PWA build 완료, 종료 코드 0.
- GREEN: `pnpm --filter @todorok/api-client test` — 런타임 5개 테스트와 타입 검사 통과, 종료 코드 0.

이 수정은 전체 Compose 재실행 이전의 웹 빌드 검증 결과이며 전체 조립 성공을 의미하지 않는다.
