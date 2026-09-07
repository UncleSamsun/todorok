# 작업 02 인증

작업 경로: `C:\workspace\todorok-worktrees\functional`. 선행 결정은 `docs/plans/task-02-auth-decisions.md`를 읽는다. 실제 공통 오류 public API를 재사용한다. 이 작업은 backend 인증만 담당하고 로그인 화면은 작업 03이다. 테스트는 실제 PostgreSQL과 security filter-chain을 포함한다. apply_patch로 편집하고 Windows 파일 작업은 PowerShell만 사용한다. 원격 push/merge 금지. 하위 작업자는 생성하지 않는다. 변경 파일만 한글 conventional commit을 작성하고 작성 도구/모델 이름과 coauthor는 넣지 않는다.

### 02. 인증·bootstrap·회전형 session — T5 / #2

**파일:** planner 신규 `auth/{UserAccount,RefreshSession,AuthService,AuthController,BootstrapCommand}.java`, 세 서비스 `security/SecurityConfiguration.java`; planner 신규 `src/main/resources/db/migration/V3__add_authentication.sql`(현재 마지막 V2); `contracts/openapi/planner-v1.yaml`; `infra/nginx/nginx.conf`의 인증 경로 요청 제한.

**입출력:** 기존 `login`, `refreshSession`, `logout` 생성 interface를 구현한다. access 유효기간 10분, refresh 30일; DB에는 refresh 해시·family·만료·사용/폐기 상태를 둔다. user ID는 UUID principal로 전달한다.

- [ ] bootstrap 두 번 실행해도 사용자 추가가 안 되는 테스트, 비밀번호 해시 검증, login 실패 공통 응답 테스트를 작성한다.
- [ ] RSA 서명·공개키 검증, issuer·audience·expiry·허용 algorithm을 검증한다. 비밀키는 환경/secret mount로 받고 저장소에 넣지 않는다.
- [ ] refresh를 트랜잭션과 행 잠금으로 한 번만 소비한다. 사용된 token 재전달은 session family 전체 폐기; 동시 두 요청·만료·로그아웃을 DB 통합 테스트한다.
- [ ] HttpOnly·Secure·SameSite=Lax cookie의 발급·회전·삭제에서 Path/속성을 일치시킨다. local 개발에서만 별도 cookie 정책을 명시한다.
- [ ] cookie 인증을 쓰는 refresh/login 요청의 Origin 검증과 CSRF 정책을 구현한다. 전역 CSRF 해제를 기본값으로 두지 않는다.
- [ ] 인증 실패 401·권한 실패 403을 01 factory로 연결한다. 타 사용자 데이터는 이후 owner-scoped 조회에서 404로 처리한다.
- [ ] 잘못된 서명·만료·다른 audience·비허용 Origin·재사용·동시 refresh·logout 이후 refresh 실패를 검증한다. 로그인 시도 제한과 429도 계약에 추가한다.

**완료:** 실제 DB 계정으로 로그인·갱신·로그아웃하고 activity/notification이 공개키만으로 JWT를 검증한다. 최초 계정 생성 HTTP endpoint는 공개하지 않는다.


결과는 `docs/plans/task-02-report.md`에 구현·테스트 명령·결과·미완료 사항·commit을 기록한다. 보고에는 실제 실행된 것과 아직 없는 증거를 구분한다.

