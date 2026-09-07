# 작업09B1 중요 리뷰 — 수정 라운드 1

검토 범위 `ce1cfb1..8ed2d4a`. Critical 0 / Important 2 / Minor 0. 요구·품질은 아래 보완 전 승인 보류다. B2/C 분리와 연결 기록409는 명세에 맞는다.

## 1. 중복 JSON 키로 공개 생성 정수 검사 우회

`TemplateCreationBodyAdvice.java:37`의 tree는 마지막 `templateSelection:null`을 보고 검사하지 않는다. generated Task/Series DTO의 `@JsonSetter(nulls=SKIP)`은 앞에 있던 선택 객체를 유지한다. 앞 객체의 expectedTemplateVersion이 소수1.5 또는 문자열1이어도1로변환된다.

리뷰는 같은 Jackson3.1.5/annotations2.21과 컴파일된 DTO로 `guard=false, Task=1, Series=1`을 확인했다. 이 단계에서 실제 HTTP는 아직 실행하지 않았으므로 회귀로 먼저 재현한다.

최소 보완: 해당 생성 advice의 JSON 읽기에 중복 키 거부를 적용한다. 원본 문자열 본문에 선택 객체와 뒤쪽 null 키를 중복해 Task·Series 각각의 실제HTTP400과command/Task/series/outbox불변을검증한다. 정상정수1·중복없는기존생성양성대조를유지하고생성물수동편집/전역mapper변경은하지않는다.

## 2. null 링크 이벤트가 기존 TASK binding의 series identity 변경

`ActivityEventConsumer.java:101`은 기존 series_id가 non-null일 때만 변경을 막고123행은null이면채운다. 기존TASK binding/series_id=null에서 v2가같은owner/task/type에templateLink:null,seriesId:다른UUID를주면binding검증없이series_id가채워진다. 낮거나같은aggregateVersion에서도이metadata추가가실행된다.

이후record-template GET의TASK binding조건이깨지고정상v2재전달도실패한다. 기존binding이있으면null까지포함한series identity를검사해야한다. 검증된미연결projection에대한정상metadata추가와구분한다.

실제DB회귀: TASK binding+null series row를만든뒤잘못된v2의낮은/같은/높은version모두거절되고기존row와inbox가변하지않음을확인한다. 정상후속event/GET이계속성공해야한다. 기존SERIES binding,늦은v1/정상v2metadata복원회귀를유지한다. 실제Kafka검증이필요하면이변경경계만집중실행하고기존전체16개를무조건반복하지않는다.

## 수행·보고

제품원인별RED→최소수정→관련HTTP/DB/Kafka회귀와build를진행하고 `task-09-template-binding-report.md`에라운드1의정확한명령·결과·이전검증과코드시점을추가한다. 수정diff는두지적과새로발생한결함만재리뷰한다. 부모ledger/vault/원격push/merge/하위agent는수행하지않는다. 보고서와본인파일만한글scopedcommit한다.
