# 기능 구현 진행 기록

계획: `docs/superpowers/plans/2026-09-07-mvp-functional-implementation.md`
기준: develop `5eea3c16a309c1828483d8c0bd627d4585d4b145`.
작업 위치: `C:\workspace\todorok-worktrees\functional`.

## 진행

- 01 공통 오류·trace ID: 완료. `a198c7d`→`4aef39c`→`548049e`→`8a15c84`, 보완 3차 리뷰 통과. HTTP 14개 전체 통과 후 Callable 강화 focused 통과, API client 5개·tsc 통과, planner 연결 smoke·activity/notification compile 통과. 검증 보고는 `docs/plans/task-01-report.md`.
- 01 리뷰 이력: async dispatch → Callable worker MDC 전파·정리, common-v1 추가 속성·URI ASCII 검증 순서로 보완해 모두 해소했다. 원본 계약의 title은 빈 문자열을 허용하므로 임의 nonempty 제약을 추가하지 않았다.
- 02 인증 backend: 구현·리뷰·통합 검증 완료. `d00d84d`·`3c7a77b` 서비스30/Nginx429/공통오류14/client build·보안 리뷰 통과. 웹 TS5097 수정 `4ba0120` build:web/API5/리뷰 통과. Compose `51301` exit0: 새 이미지·fresh DB·health·Connect·marker 보존·동일 volume 재기동·정리 통과. 커버리지 비율 계측은 최종 gate에 남아 있다.
- 03 로그인·session·routing: 완료. `63e7591` 구현·실제 브라우저 로그인/갱신/두탭logout/재접속/token storage 비어있음 검증. `4a3e35a` late refresh 경쟁 보완 후 웹20/build·리뷰 통과. 브라우저 session54304 exit0, 자원정리 완료.
- 03 리뷰1: Web Locks 미지원 환경의 늦은 refresh 성공/실패 race를 generation 재확인과 두탭 결정론적 테스트로 해결했다.
- 04 달력·Task·오늘 화면: `fbfe2cf` 구현·검증 완료, 독립 리뷰 중. planner24·Task최종5·웹22+진행률2·build/drift/package와 실제브라우저CRUD/16view조합 통과. 보고 `docs/plans/task-04-report.md`. 05–16은 미착수.
- 03 실제 브라우저에서 native fetch의 잘못된 receiver로 Illegal invocation이 발생해 서버 요청이 없음을 재현했다. local 함수 호출로 수정하고 receiver-sensitive 회귀 테스트·build 통과 후 실제 브라우저를 재검증 중이다. 단위 테스트 통과를 실제 로그인 성공으로 대체하지 않는다.
- 새 worktree의 event-contracts Gradle gate 성공(캐시 재사용). 작업 01 runtime 테스트 결과는 위 완료 증거를 따른다.
- PRD AC-3를 §10.4의 반복 조건표와 일치시키는 문서 정정을 반영했다. 월말/집계 신규 규칙과 구분한다.

## 선행 검토

| 작업 | 자체 요구 정합성 | 공유 인터페이스·후속 |
|---|---|---|
| 01 | 보안 filter는 02에서 연결 | ProblemResponseFactory → 02 |
| 02 | JWT·cookie·rotation | session 응답 → 03 |
| 03 | 메모리 token·query | principal·fetch → 04 |
| 04 | 일반 CRUD·기록형 complete 금지 | Task event → 05·07 |
| 05 | 월말 정책 제안 문서화 필요 | series → 08 |
| 06 | debounce·version 충돌 | 날짜 detail·Activity 메모 → 07 |
| 07 | eventual consistency 명시 | 완료·partial → 08 |
| 08 | 집계 제안 문서화 필요 | revision·summary → 09·10 |
| 09 | immutable template | detail·snapshot → 10·12 |
| 10 | 일반 반복과 분리 | session 요청 → planner |
| 11 | 외부 데이터 이용 범위 필요 | importer → 10 |
| 12 | timestamp·부분 기록 | Activity 저장 → 07 |
| 13 | 전달 exactly-once 보장 불가 명시 | Task events·설정 → 14 |
| 14 | offline queue 제외 | routing·session 재사용 |
| 15 | 비용·배포 별도 권한 확인 | existing migration·Compose |
| 16 | 실기기·7일 관찰 실제 증거 필요 | 전체 기능 gate |

현재 결론: 외부 자료·실기기·운영 권한이 필요한 후반 검증은 실제 확보 상태로 판정하며, 로컬 기능 구현은 계속한다. 공개 endpoint·집계 의미를 바꾸는 작업은 PRD와 계약을 먼저 갱신한다.
