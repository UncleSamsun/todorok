# 작업08 기록 수정·취소·지난 기록·월 요약

작업 경로 `C:\workspace\todorok-worktrees\functional`. 작업07의 실제 Activity aggregate·detail·TaskReference·완료동기화와05의series정책을 재사용한다. PRD9.2/10.2/10.4 및 DESIGN9가 기준이다.

## 계약과 데이터

- activity 수정은 expectedVersion을 요구하고 revision을 올린다. 원본값/수정이력을 보존하며 ActivityCorrected event를 outbox에 같은 transaction으로 기록한다.
- void는 물리삭제하지 않고 VOIDED로 전이한다. 중복요청과동시수정은멱등/409로 처리하고 과거detail을보존한다.
- planner는 correction/void를 eventId·aggregateVersion으로 처리한다. 이전event가나중에도착해최신기록을덮지않는다.
- 이미완료된후속회차는삭제하지않고 PLANNED회차만 재계산한다. 사람이처리해야할충돌은명시상태로남긴다. 대기UI를임의시간후성공으로바꾸지않는다.
- 지난기록은 날짜→Task/루틴→기존폼이며 실제수행일로저장한다. 일회성/기존활성/종료series/과거next/미래next/PARTIAL을PRD10.4표와대조한다.

## 월별 집계 정의

코드 전에 아래 구체화를 PRD와원본OpenAPI에기록한다. 프로젝트의 날짜는서울기준이다.

- 완료: 선택수행월의 COMPLETED Activity 수. VOIDED/PARTIAL 제외.
- 시간: 선택수행월의 COMPLETED/PARTIAL에 사용자가입력한실제시간합. VOIDED제외, 없는시간은추정하지않는다. 합산은초단위정수로하고화면에서분표시한다.
- 등록: 선택예정월의 DELETED제외 Task 수. 반복은실제로생성된회차만, 아직없는미래회차는세지않는다.
- Activity통계는activityAPI, 등록수는plannerAPI에서각각읽는다. 다른서비스DB직접조회금지. 프런트에서같은월·유형응답을조합하며한쪽실패를0으로표시하지않는다.
- 미래월은API에서도거부하고UI다음버튼비활성화. 실제현재월은서버Clock검증, 윤년·연말·수행일변경을포함한다.

## 화면

- 운동·공부·클라이밍최상단의선택월과두버튼, 둘째줄동일3칸완료/시간/등록중앙정렬. 최대820px중앙배치,위수평구분선없음. 오늘목록의체크형식을재사용한다.
- 세탭은월선택상태공유. 각API의query key에user/month/type을포함하고month변경중늦은응답이새월을덮지않도록한다.
- 기록수정·취소·지난저장성공은기존행과관련일/month query를갱신한다. 삭제후새목록아이템을만들어중복표시하지않는다.
- history는keysetpagination, 상세는지연조회한다. 긴목록전체를처음부터내려받지않는다.

## 테스트

1. 실제DB version/revision·수정이력·void보존·중복/동시요청·event원자성.
2. 실제Kafka correction/void순서역전·중복·consumer중단복구 후Task/summary정합성.
3. 과거완료의기존활성유지·종료미생성·과거next오늘이월·미래next유지·PARTIAL미생성·일회성미생성.
4. 월경계서울00시·윤년·VOIDED/PARTIAL·시간미입력·타입별등록·deleted제외·미래거부·correction날짜변경양쪽월갱신.
5. 실제브라우저지난기록→과거날짜선택복귀·수정/취소·월이동·세탭동일월·부분통계실패표시.

하위작업자생성/원격push/merge금지. apply_patch/PowerShell사용. 신규migration은각서비스최신다음. 의미있는회귀테스트·생성drift·build와docs/plans/task-08-report.md의실제증거를남기고scoped한글commit한다. 부모progress/다른brief는수정하지않는다.
