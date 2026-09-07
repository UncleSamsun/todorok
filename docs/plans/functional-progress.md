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
- 04 달력·Task·오늘 화면: 완료. `fbfe2cf` planner24·Task최종5·웹22+진행률2·build/drift/package·실제브라우저CRUD/16view조합 통과. 편집경합 보완 `c4e5772` 관련9개/build·리뷰 통과. 보고 `docs/plans/task-04-report.md`.
- 04 리뷰1: 같은 Task 재조회에서 초안·version이 섞이는 경합은 snapshot고정·sameTask재클릭방지와409초안보존/닫기재열기 테스트로 해결했다.
- 05 반복·이월·skip: `f9c5629` 구현·`40e2bb2` 보고. HTTP14/정책6/event16/web27·정책분기27+24 모두통과, 실제브라우저47377 exit0. 독립리뷰 중. canonical생성물 EOF빈줄16건은 생성단계 정규화로 보완 중. 06–16 미착수.
- 05 리뷰1: range/detail 수동refetch가 ready=false를 우회하는 경로 보완 필요. 이월대기/실패에서 retry버튼/handler를 guard하고 cached queryerror 경계를 테스트한다. 반복·잠금·상태 핵심 검토는 통과했다.
- 05 최종: 완료. `4e293a8` 생성EOF정규화·실제재생성검증, `78f84ec` 이월중수동조회guard·3개회귀/build·보완리뷰 통과.
- 06 메모: `78f84ec` 기준 구현 착수. 07–16은 미착수이며 전체 목표 유지.
- 06 구현 `887a825`: PostgreSQLHTTP17·웹전체39·최종메모12·build/contracts/drift 통과. 실제메모/충돌/오프라인/다음회차와 별도 실제pointer·390/1440화면 검증 후 중요리뷰 중이다.
- 06 최종: 리뷰 통과로 완료. 저장·초안·세션경계에 중요결함 없음.
- 07 Activity: 서버연동 단계 착수(`887a825` 기준). 서버 계약/DB/Kafka를 확정 후 기록UI를 별도 하위 단계로 연결한다. 전체07완료는 실제브라우저까지 포함한다. 08–16 미착수.
- 07 서버 중간: 유형별detail·command멱등·sync상태·실제입력시간 계약과 ActivityV3/plannerV7 구현. 실제 HTTP/CDC/Kafka 왕복 테스트 session45094 실행 중(완료판정 아님). runtime Compose messaging 활성화, test/migration 격리 설정 반영.
- 07 검증 후속: 초기 compile/fixture Origin 오류 뒤 실제 Boot4 Kafka자동설정 누락을 발견해 starter와 retry/DLT listener factory를 연결했다. 현재 재실행23981 진행 중이며 앞선 초기화 실패를 동작테스트 성공으로 계산하지 않는다.
- 07 서버 `aa65744`: 실제왕복8개 및 typed추가1개·plannerHTTP17·persistence8·security2·event17·migration2·bootstrap1 통과. 중요리뷰 중. UI단계는 아직미착수이며 전체07완료 아님.
- 07 서버리뷰1: 두회차완료→첫Activity취소→재기록에서 종료된후속회차가중복생성될수있는경계 발견. 기존발생일(삭제포함)을재사용하지않고최신보존발생일이후로계산·DB발생일unique제약·실제재완료회귀로보완한다. 기존데이터삭제로해결하지않는다.
- 07 서버최종: `88fa718` maxcursor/V8unique 보완, DBHTTP2·실제Kafka1·재리뷰 통과. 서버단계 완료.
- 07 UI: `88fa718` 기준 기록폼·동기화표시·실제브라우저 연결 착수. 전체07완료는 이 단계 검증 후다.
- 07 UI 검증 준비: 최신 planner/activity runtime+migration jar가 포함된 Docker이미지를 session16737에서 빌드 중이다. UI담당과 중복빌드하지 않도록 분리했다. 완료시 imageID를 기록해 실제브라우저 fixture에 전달한다.
- 07 UI 서버이미지 준비완료: 16737 exit0. planner `sha256:0762771324927cd737dd04fcf02fdd4f41f687157fb67c22c57d5c73ab864d65`, activity `sha256:98b3e1da3c72a72586b9a4d5c4f3b8c67fde43dc7df567907995a872d6424c82`. `88fa718` 서버소스의 V8/V3와 migration/bootstrap 포함. UI담당에전달.
- 07 UI 전용환경: project `todorok-task07-ui`, URL `http://localhost:5187`. `.local/task07-runtime/runtime.mjs up` session39236 exit0; `probe.mjs` 실제Task→Activity→Kafka→plannerCOMPLETED/APPLIED 확인 exit0. UI검증을 위해 유지 중이며 부모가 마지막에 down으로 전용자원만 정리한다. 비밀값은 local파일에만 있음.
- 07 UI `34f36d5`: 웹43/build·실제3유형저장/APPLIED/기존행완료/취소DB0·390px검증 통과. StrictMode effect replay·이탈후늦은응답·자동APPLIED갱신 전용회귀는 미완료여서 별도 집중보강 중이다. 프로덕션브라우저를 StrictMode 개발재실행 증거로 간주하지 않는다.
- 07 UI 집중검증: Task queryparam 구독 누락과 자동APPLIED 갱신중복/기록전환 경계를 재현해 보완 중이다. 전용runtime down22444 exit0, 컨테이너/volume/network와 임시키·credential4파일 제거. screenshot/harness 보존, liveAPI없이 회귀 진행.
- 07 UI `4bbf1de`: queryparam구독·동기화갱신/전환·StrictMode 보완, 집중17/build통과. 최종리뷰3건(HTTP5xx불확실요청보존, 이탈후세션내초안/요청복원, 부분시간입력거부)을 집중보완 중이며 전체07미완료.
- 07 최종완료: `77bf83b` 세션초안/불확실HTTP/부분시간 보완·집중30/build/diff·재리뷰 통과. 서버실제왕복과 UI실제3유형브라우저 증거를 합쳐 전체07 완료.
- 08 기록변경 서버단계 착수: correction/void/지난기록 반영 후 월별조회·UI를 후속하위단계로 연결한다. 09–16 미착수.
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
