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
- 08 서버 `65aa6b8`: 실제Kafka6+시간보강1/contracts19/migration-security4/drift/build통과. 리뷰에서동시PATCH중별도header/detail SQL조회가다른revision을섞을수있는P2를발견해 일관snapshot조회·list/replay 회귀보완 중이다.
- 08 서버완료: `a3a58ff` 단일SQLsnapshot·PGbarrier9조합·samecommand8경합·validator3개·재리뷰 통과. 월별조회API/기록관리UI 하위단계 착수. 전체08미완료.
- 08 UI환경: backendbuild85961 exit0, planner `78e7f4ca90890651d0c80c49f18c2bb7e71e944e24c621044216b4ebd2a80166`, activity `f2a41f32bd694d81347dd076af285135ecd8f7baff5cf30129d31af9877633c9`. runtime8767 exit0 project`todorok-task08-ui` localhost5188. probe40915 exit0: 실제badmonth400/correction+void APPLIED/month합계확인. UI브라우저 검증용유지중.
- 08 UI `53c031a`: 월API DB/Clock/실제HTTP·신규UI3개/build/drift통과. 기존StrictMode조회대기와 실제과거Task선택timeout 미해결로 집중인계. 별도허용된브라우저CLI0.36 정상실행·부모로그인PASS확인(차단된실행파일과구분). 전체08미완료.
- 08 실제UI 후속: 과거Task 선택·저장·APPLIED는 native 조작으로 확인했다. 저장 후 이미 방문했던 도메인의 월요약/목록이25초 동안 이전값을 유지하는 실제 캐시갱신 누락을 발견했다. 기존 생성/APPLIED 경로가 calendar만 무효화하고 새 Activity조회키를 빠뜨린 원인으로 집중회귀·수정 중이다. StrictMode조회는1,111ms에성공하여 초기render부하와 구분했고 timeout상향 없이 mount완료대기를검증한다.
- 08 검증계속: async act 초기mount와단일worker로 기존69/69통과, 월요약stale 신규회귀RED확인 후 생성/APPLIED경로수정 중이다. 월경계·윤년·세유형·조회불변성 직접DB증거의 누락을 `task-08-summary-boundary-brief.md`에 명시해 UI수정이후 순차보강한다. 현재08완료아님.
- 08 UI최신검증: 수정후70/70·build통과와실제과거저장/APPLIED→수정45분→합계75분→VOIDED→합계30분·탭간월공유를확인했다. 부모모바일캡처검토에서390px추가버튼글자줄바꿈을발견해배치보완요청했다. 빌드전환중단발성blank원인도증거확인중이며 최종리뷰/DB누락보강전완료처리하지않는다.
- 08 UI보완확정 `3e8fe6d`:70/70/build/실제저장수정취소·390px버튼행보완통과,부모최종캡처확인. `a3a58ff..3e8fe6d` frozen diff중요리뷰와별도DB경계검증보강착수. PWA전환blank는아래14필수항목에별도추적한다.
- 08 중요리뷰: 요구미충족/수정필요. 편집version혼합·수정/void동기화미연결·완료Task신규폼·400옛snapshot재전송·불확실응답시간비교누락·등록캐시미갱신·메모수정시간초손실7건,재시도44px1건을 `task-08-summary-review.md`에기록했다. DB검증작업완료후UI담당수정라운드1로순차전달한다. 서버SQL중요결함은발견되지않았다.
- 08 DB경계 `9424f97`: 실제PostgreSQL추가5개exit0·XML5/0/0/0부모확인. 윤년/UTC동일시각·다중세트·두유형상태/owner·소수초·조회전체행불변통과. 보강test읽기전용리뷰착수. UI라운드1은 `task-08-summary-fix-brief.md`로직전담당에게전달했으며유일한구현작업이다.
- 08 DB보강리뷰:실행은통과했지만서울/UTC경계동일시간상쇄와Activity없는Taskreference누락2건으로검증미충족. `task-08-summary-boundary-review.md`에기록하고현재UI작업후순차보완한다. 테스트가녹색인것을요구증명과동일시하지않는다.
- 08 UI라운드1중간:집중15개중완료Task/등록갱신포함12개통과. 정밀timestamp본문이generated경계에서이중JSON직렬화되는3실패를확인해최종middleware단계본문교체로보완중이다. tsc통과후집중12개10907실행중. 전체라운드완료아님.
- 08 UI라운드1검증:10907 correction12/12,31287 추가6/6(sub-ms/시간직접편집/불확실초안재진입/lateGET/void충돌)통과보고. api-client build/tsc통과. 최종웹전체65495실행중이고실제완료Task정확ID/등록증분/수정APPLIED/실패재시도44px브라우저검증예정이다.
- 08 UI라운드1후속:65495웹91/91(14files)/build통과;8292실제등록5→6·완료Task두경로exactID·41분수정APPLIED·요약요청abort/44px재시도복구PASS. 부모harness/캡처확인. 저장전시작GET의저장후도착상태역행경계를46653으로추가검증중이며결과후커밋/재리뷰한다.
- 08 UI라운드1최종보강:46653은저장v5/PENDING후늦은GETv4의캐시역행을RED로확인했다. 낮은revision/동일revisionPENDING역행보호후48445 correction19+lifecycle23=42/42통과. 이전91/91과코드시점을구분한다. 최종build75749·native짧은확인후보고/커밋예정.
- 08 UI라운드1 `e88b885`:최종build/native최신entry읽기확인후커밋. 7개Important+44px재리뷰착수. DB검증라운드1(서울경계상쇄/unlinkedreference2건)은유일한구현worker로순차착수했다.
- 08 UI재리뷰통과:Important7+44px모두해결,새중요결함없음. 전용runtime down81120 exit0,컨테이너/volume/network0확인·소유browser3session종료·임시credential/key4파일삭제. harness/캡처는보존. DBtest2건보강은계속진행중이므로전체08완료아님.
- 08 DB라운드1 `dd544ea`:focused2/2실제PG·XML2/0/0/0부모확인. 잘못된UTC집계720초와정상서울300초구별,owner전체reference/orphan상태변경탐지보강. scoped재리뷰착수했으며결과전전체08완료보류.
- 08 최종완료:서버correction/snapshot/Kafka검증과UI `e88b885` 중요재리뷰통과,DB `dd544ea` 두검증재리뷰통과를합쳐판정. 실제과거저장/수정/void·월요약·완료상세·등록갱신·모바일검증증거는각보고서에있다. PWA배포전환은14필수항목으로유지. 09는승인된템플릿요구의계약/버전/일정연결세부설계부터착수한다. 10–16미완료.
- 09 설계 `d2422ba`: PRD12.3·상세설계·09A 기반 인계를 확정했다. 정의 관리→서비스 연결→UI 세 gate로 분리하고 09A 구현을 착수했다. 전체09는 미완료다.
- 09A 첫 RED: 원본 관리 계약·생성 타입과 실제 HTTP 테스트를 추가했다. POST templates의 기대 201 대비 실제 404로 미구현을 확인한 뒤 migration/service/controller 구현 중이다. 기존 테스트 entity 검증 간섭은 별도 fixture 설정에서 분리했으며 성공 증거로 계산하지 않는다.
- 09A 첫 GREEN: 실제 PostgreSQL migration·JWT HTTP 생성/조회 1/1 통과, 부모 XML1/0/0/0 확인. 다섯 형식·TIME 기본초·version1/revision0·Task/outbox 미생성·동일 GET 범위만 증명했다. strict body·rollback·owner/pagination/archive/경쟁·DB 불변성·최종 gate는 남아 있다.
- 09A 중간검증:73582 실제PG/JWT HTTP7시나리오 통과, 부모XML7/0/0/0확인. strict본문/rollback/owner·목록·보관·경쟁·불변성 포함. 대량필드/동시command재전달·최종activity suite·생성drift/build 및프록시413증거를남은gate로확인한다.
- 09A 추가검증:67085 보강3/3·부모XML3/0/0/0확인(512필드·동일command경합·record/history/command불변). 프록시exact1MiB는502간접증거대신실제stub의200/수신1048576bytes로검증하도록보강중이며초과chunked413도유지한다.
- 09A 프록시:83445 static+실제Nginx4/4통과보고. exact1MiB=200/수신1048576bytes,chunked+1=공통413JSON확인. TS왕복optional unit의undefined기대값조정후계약/drift·activity전체suite·package build를최종gate로진행중이다.
- 09A 최종gate중간:95960 activity전체28/28(템플릿8포함),부모7개XML합계28/0/0/0확인. 계약56075 6/6·drift22185일치·client5/build통과보고. 마지막TIME단위null DB제약강화후해당focused회귀와assemble/report/commit이남아있다.
- 09A 구현 `314d0d8`: 마지막TIME제약focused2/2(부모XML확인)·assemble통과후담당실행이사용량제한으로중단됐다. 부모가stage범위/공백검사를확인하고trailingwhitespace1곳정리후코드61파일을커밋해보존했다. 중요리뷰는생성소스포함분할diff로요청했으며완료판정은보류한다.
- 09A 리뷰:Important1건(fields필수누락이generated빈배열기본값으로처리돼새정의를비움)을확인했다. `task-09-template-foundation-review.md` 기준으로누락/null거부·명시[]허용·HTTP실패후상태불변을수정라운드1로전달했다. 다른중요결함은없으며전체09A완료는보류한다.
- 09A 리뷰반증:제품무수정실제HTTP13/13(부모XML확인)에서누락/null/비배열은이미거절됐다. 실제누락응답은VALIDATION_FAILED/fields NOT_NULL,주입mapper결과도fields=null로확인했다. 초기[]소스만으로판단한Important는오탐이므로제품수정없이정확한회귀assert와증거로재판정한다.
- 09A 반증회귀 `9fe9fe6`:최종15/15(부모XML15/0/0/0)·assemble통과. 제품변경없는test/report2파일을커밋하고scoped재판정요청했다. 기존Important는누락필드400의정확한오류/mapper/DB불변증거로평가하며최종판정전완료보류.
- 09A 완료:독립재리뷰가fields지적을오탐으로철회하고요구·품질승인. 원래28/28·proxy4/계약6/drift/client검증과최종15회귀/assemble증거를합쳐기반관리만완료판정한다. 전체09는B1선택/일정연결→B2기록/legacy→C UI가남는다. B1은실제context경로·서비스인증/외부차단을명시한별도brief로진행한다.
- 09 Ruling: Task/series identity는 유지하고 새 기록 최초 저장 시 현재 정의 버전을 고정한다 — PRD의 이후 기록 적용과 과거 수정 보존을 함께 지키기 위함 — 다르게 요구되면 버전 선택 UI와 계약 재작업이 필요하다.
- 09 Ruling: 보관은 신규 선택만 막고 기존 승인된 일정·반복은 계속 사용한다 — 일정 자체의 보관과 템플릿 보관을 분리하기 위함 — 잘못 해석했다면 반복 중단 정책과 사용자 안내를 수정해야 한다.
- 09 Ruling: 서비스 선택 승인의 잔존은 허용하되 대상·소유자·command에 고정한다 — 분산 원자성을 가장하지 않고 실패 재시도를 보존하기 위함 — 향후 안전한 정리 정책을 따로 마련해야 한다.
- 09 Ruling: 기존 자유 JSON은 보존 전용 읽기와 서버 보존 correction으로 이행한다 — 알 수 없는 필드 의미를 추측하지 않기 위함 — 기존 임의 자료의 자유 편집이 필요하면 별도 전환 UI가 필요하다.
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

## 후속 필수 검증

- 14 PWA/배포 업데이트: 작업08 frontend교체에서 기존 열린브라우저의 옛lazy chunk요청이 SPA fallback HTML(200/736bytes)을받아빈화면이발생했다. nginx12:12:23Z요청은 `DomainPage-DfclhuWz.js`/`StudyPage-DE_Ha1XF.js`, 직전새SW는새chunk를precache했다. 새로고침후새index와실제전체흐름은정상이다. 정적asset없는경로의정확한404와oldchunk/lazyimport실패복구, SW업데이트중열린페이지회귀를작업14에서검증한다. 새로고침복구를자동복구완료로간주하지않는다.
