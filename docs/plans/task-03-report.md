# 작업 03 로그인·세션·라우팅 구현

## 구현 범위

- 생성된 `planner.AuthApi`의 login/refresh/logout과 Configuration의 fetch 연결점을 사용했다. access token은 SessionClient 메모리에만 두며 React snapshot에는 사용자 ID·상태·세대 번호만 노출한다. 오류는 상태 코드만 보존하는 SessionError로 정리한다.
- `packages/api-client/src/session.ts`는 브라우저 전역에 의존하지 않는다. 같은 탭 refresh single-flight, 401 이후 최대 한 번 재전송, 사용자 전환 중 응답 차단, 빈 204 로그아웃을 구현했다. 네트워크 실패 mutation은 재전송하지 않는다.
- 웹 adapter가 Web Locks로 인증 작업을 직렬화하고 lock 안에서 BroadcastChannel의 최신 메모리 세션을 다시 확인한다. 새 탭은 기존 탭에 최신 상태를 요청한다. 로그인·로그아웃·실패도 전파한다. 브라우저 lock이 없으면 같은 탭만 직렬화하며 서버의 replay 방어를 변경하지 않는다.
- AuthProvider는 세션 세대가 바뀔 때 Query cache를 비운다. 이전 사용자의 진행 중 Query도 제거되어 늦은 응답이 새 사용자의 cache를 덮지 않는다. Query와 mutation의 자동 재시도를 껐다.
- 로그인 입력은 실패 후 보존한다. 로그인·복원 후 항상 `/today`와 실제 서울 날짜로 시작한다. 운동·공부·클라이밍·설정은 lazy import한다. 설정에서 주/월 보기만 저장한다. 오늘 기능은 후속 작업이므로 가짜 일정·완료 수치는 없다.
- React Router `7.18.3`, TanStack Query `5.102.8`을 정확히 고정했다. npm metadata에서 각각 Node >=20, React 18/19 호환성을 확인했고 기존 Node24·React19.2.8·Vite8 구성으로 빌드했다. [React Router 설치 문서](https://reactrouter.com/start/framework/installation), [TanStack Query 설치 문서](https://tanstack.com/query/latest/docs/framework/react/installation)를 확인했다.

## RED 관찰과 수정

1. 빈 204 + JSON Content-Type에서 실제 `requestJson`이 `SyntaxError: Unexpected end of JSON input`을 발생시켰다. 204/205를 본문 없이 처리한 뒤 통과했다.
2. 새 세션·UI·adapter 테스트는 구현 전 누락된 SessionClient 또는 모듈로 실패했다. 이는 초기 기능 부재 증거이며 동작 assertion RED와 구분한다.
3. 생성 logout이 401이면 즉시 실패하던 테스트를 확인했다. 같은 인증 lock 안에서 refresh 후 logout을 한 번만 다시 보내도록 수정했다.
4. 실제 Chromium에서 로그인 요청이 서버에 도착하지 않았다. Origin을 지정한 서버 probe는 401이었고 브라우저 auth network 목록은 비어 있었다. 기본 fetch를 객체 메서드로 호출하면 `TypeError: Failed to execute 'fetch' on 'Window': Illegal invocation`이 발생함을 확인했다. receiver-sensitive 회귀 테스트에서 기대 authenticated/실제 anonymous를 재현한 뒤, 로컬 함수 변수로 fetch를 호출하도록 수정했다.

## 자동 검증

- `pnpm test:web`: 4개 파일, 18개 테스트 통과.
- `pnpm --filter @todorok/api-client test`: 5개 런타임 테스트와 TypeScript 검사 통과.
- `pnpm build:web`: TypeScript와 production/PWA build 통과. 4개 domain chunk가 별도로 생성된다.
- `git diff --check`: 종료 코드 0.

주요 검증: 세션 복원 성공/401/network 실패, 로그인 실패 입력 보존, 401 병합·재발 시 중단, network mutation 1회 전송, 401 mutation 본문 보존, 이전 사용자 HTTP 응답 차단, 이전 사용자 Query 응답 차단, logout 후 cache 소거, 생성 logout의 204·401 경로, native fetch receiver, storage 미기록, lock 지원/미지원 adapter, 서울 날짜 자정 경계.

## 실제 브라우저 검증

`node .local/task03/browser-smoke.mjs`로 임시 RSA 키·임시 계정·별도 PostgreSQL volume을 사용했다. 기존 `4ba0120` planner image와 최신 웹 production build를 Nginx에서 `http://localhost:5183`으로 제공했다. backend source·운영 설정은 수정하지 않았다. agent-browser 0.36.0과 실제 Chromium을 사용했다.

최종 실행은 2026-09-07 16:09 KST에 종료 코드 0으로 통과했다. 실제 Nginx 로그에서 최초 cookie 없는 refresh 401, login 200, reload refresh 200, logout 204를 확인했다. 두 번째 탭의 `/study?date=2020-01-01` 진입이 `/today`·`2026-09-07`로 초기화됐다. 두 번째 탭 로그아웃 뒤 첫 번째 탭도 `/login`으로 전환됐고 첫 탭 reload 뒤에도 로그인 화면이 유지됐다. native Web Locks와 BroadcastChannel을 사용할 수 있었고 localStorage/sessionStorage key는 모두 빈 배열이었다. browser errors 출력은 비어 있었다.

로그인·오늘 화면과 375×812 모바일 로그인 PNG를 직접 열어 확인했다. 최종 harness는 `REAL_BROWSER_AUTH_PASS`를 출력했고 전용 Compose project의 컨테이너·volume·network를 정리했다. 임시 RSA 키도 실행 후 제거했다.

증거 파일: `.local/task03/login.png`, `.local/task03/today.png`, `.local/task03/logged-out.png`, `.local/task03/login-mobile.png`. 이 경로는 테스트 산출물이며 Git에 넣지 않는다. 한 번의 harness 실행은 구버전 문서의 숫자 탭 선택 명령 때문에 중단됐고, 실제 CLI의 stable tab ID를 사용하도록 harness를 수정했다. 이 중단을 제품 인증 실패로 계산하지 않았다.

## 후속 작업 경계

- 오늘·각 domain 데이터 기능은 작업 04 이후 구현한다. 생성 domain client는 `Configuration({ fetchApi: session.fetch })`를 사용하면 메모리 인증과 401 처리를 공유할 수 있다.
- Web Locks 미지원 환경에서는 여러 탭이 동시에 refresh를 보내면 서버 replay 방어에 따라 재로그인이 필요할 수 있다.
- PWA는 정적 파일을 precache한다. lazy domain은 별도 코드 chunk이지만 service worker 설치 과정에서 정적 다운로드될 수 있다. 인증 응답은 cache에 넣지 않는다.
- 서버 session 폐기 성공 후 메모리와 cache를 지운다. logout의 네트워크 실패는 오류를 보여주고 재시도를 허용한다. 이미 발급한 access JWT의 서버 만료 정책은 작업02와 같다.

## 리뷰 1차 후속 수정: 늦은 refresh 응답 경합

Web Locks가 없고 BroadcastChannel만 있는 두 탭에서, 탭 A의 refresh 응답을 보류한 뒤 탭 B를 로그아웃시키는 회귀 테스트를 추가했다. 실제 SessionClient 두 개와 웹 coordinator 두 개를 사용하고 외부 HTTP 응답만 지연했다.

- RED: `pnpm --dir apps/web exec vitest run src/app/browser-session.test.ts` — 4개 중 2개 실패. 늦은 성공은 기대 anonymous/실제 authenticated로 세션을 되살렸다. B 로그아웃 후 다른 사용자로 로그인한 상태에서 늦은 401은 기대 authenticated/실제 anonymous로 새 세션을 지웠다.
- 수정: refresh HTTP를 시작할 때 세대를 캡처하고 응답을 적용하기 전에 다시 비교한다. 이미 다른 세대라면 원래 요청을 실패시키되 현재 메모리와 다른 탭에는 쓰지 않는다. refresh 실패 정리와 logout 내부 refresh 실패 정리에도 같은 세대 조건을 적용했다.
- GREEN: `pnpm test:web` — 4개 파일·20개 테스트 통과. 두 탭의 최종 상태와 새 사용자 ID가 보존되고 원래 요청은 실패함을 확인했다.
- GREEN: `pnpm build:web` — TypeScript와 production/PWA build 통과.

이번 후속 수정은 결정적으로 제어한 두 탭 경합 테스트로 검증했다. Docker·실제 브라우저 전체 흐름은 재실행하지 않았으며 앞 절의 브라우저 결과는 최초 구현 커밋 검증이다.
