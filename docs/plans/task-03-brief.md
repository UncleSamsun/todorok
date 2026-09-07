# 작업 03 로그인 화면·세션 복원·라우팅

작업 경로: `C:\workspace\todorok-worktrees\functional`. 선행 작업 02의 실제 인증 API와 보고를 읽고 연결한다. 아직 backend 완료 전에는 구현 착수하지 않는다.

## 완료할 사용자 흐름

1. 최초 화면에서 cookie refresh로 세션 복원을 시도한다. 인증되지 않으면 로그인 화면을 표시한다.
2. login 성공 시 오늘 route를 연다. 사용자 화면의 제품명은 토도록이다.
3. access token은 React 메모리에만 저장한다. 웹 저장소·URL·로그에 token을 넣지 않는다.
4. 동시 401에 대한 refresh는 하나로 합치고 원래 요청을 최대 한 번만 재전송한다. network 실패 mutation은 자동 재전송하지 않는다.
5. logout은 서버 session 폐기 후 Query cache·메모리를 정리한다. refresh 실패는 로그인 상태를 지우고 로그인 화면으로 전환한다.
6. 새 세션은 항상 실제 서울 날짜로 시작한다. 주/월 보기만 저장하고 과거 선택일·마지막 domain tab은 복원하지 않는다.

## 파일·경계

- `apps/web/src/app/router.tsx`: 로그인과 보호된 오늘·운동·공부·클라이밍·설정 route. 오늘 외 domain은 lazy import한다.
- `apps/web/src/app/query-client.tsx`: TanStack Query 설정과 cache 경계. mutation retry는 끈다.
- `apps/web/src/features/auth/AuthProvider.tsx`, `LoginPage.tsx`: session state와 UI. token이 error object나 debug output에 섞이지 않게 한다.
- `packages/api-client/src/session.ts`: DOM 없는 주입식 refresh single-flight. generated client의 fetch/config 연결점을 사용한다.
- 브라우저 Web Locks·BroadcastChannel은 `apps/web` adapter로 둔다. lock 획득 뒤 최신 세션 갱신 결과를 확인한다. token 공유는 메모리 message에만 한정하고 로그·영구 저장을 금지한다.
- lock 미지원 환경에서는 동일 탭 single-flight만 보장되므로 다중 탭 충돌은 재로그인으로 안전하게 처리하고 refresh replay 탐지를 무력화하지 않는다.
- `App.tsx`, `main.tsx`, `apps/web/package.json`과 lockfile을 수정한다. 기존 welcome 화면을 로그인·보호 화면 흐름으로 교체한다.

React Router·TanStack Query는 설치 시 호환되는 지원 버전을 확인하고 고정한다. 기존 React19.2/Vite8/TypeScript 설정을 유지한다. 아직 오늘 기능은 작업04에서 구현하므로 보호 route에서 거짓 완료/가짜 데이터는 표시하지 않는다.

## 테스트

- 세션 복원 성공/401/network 실패, 로그인 실패 후 입력 보존, logout 후 cache 소거.
- logout의 204 응답은 본문 JSON 파싱을 시도하지 않는다. 현재 공통 requestJson은 Content-Type만 보고 body를 파싱하므로 호출 방식에 따라 빈 JSON body가 SyntaxError가 될 수 있다. 실제 generated logout 경로로 빈 본문 성공을 테스트하고 필요한 transport 경계를 보완한다.
- 여러 동시 401에서 refresh 1회, 재발한 401은 무한 재시도하지 않음.
- 응답 유실 network mutation은 1회 전송, 401 응답만 갱신 후 재전송.
- 사용자 전환 시 이전 사용자 Query 응답이 새 화면을 덮지 않음.
- storage에 access token을 쓰지 않음. 브라우저 lock adapter를 실제 지원 환경과 미지원 환경으로 나눔.
- component 테스트와 실제 로그인/refresh/logout API 브라우저 흐름을 검증한다. 실제 서비스가 필요하면 local test credentials만 생성하고 제품 코드에 기본 계정을 넣지 않는다.

apply_patch 편집, PowerShell 파일 작업, 하위 작업자 생성 금지, 원격 push/merge 금지. 완료 후 변경 파일만 한글 conventional commit하고 `docs/plans/task-03-report.md`에 테스트 명령·결과·남은 제약을 기록한다.
