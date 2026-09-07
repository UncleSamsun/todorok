# 작업09B2 템플릿 기록 값·스냅샷·기존 데이터 보존

현재 B1 보완 `241f9d9`와 수정 범위 재리뷰가 완료됐다. 기준 설계는 `docs/superpowers/specs/2026-09-07-record-templates-design.md` §3–6,9–13, PRD §12다.

실행 순서는 사용자 흐름을 따른다. 먼저 공부의 관리→일정 선택→값 기록→정의 변경 후 과거 기록 유지 흐름을 백엔드와 UI까지 연결한다. 이후 자유 운동·클라이밍에 같은 규칙을 적용한다. 아래 전체 데이터·검증 요구는 유지하되 이번 공부 흐름에서 사용하지 않는 도메인 구현을 선행 확대하지 않는다. 백엔드 준비만으로 사용자 기능 완료를 선언하지 않는다. 단계별 상태·검증은 `functional-progress.md` 한 곳에 기록한다.

## 목표

B1의 `TEMPLATE_RECORD_NOT_READY` 거절 경계를 실제 검증·저장으로 교체한다. 연결 Task에 정의된 선택 입력값만 저장하고, 최초 기록 당시의 버전과 전체 정의 snapshot을 서버가 고정한다. 기존 기록 수정과 과거 자료를 깨뜨리지 않는다. 웹 관리·추가·동적 입력 UI는 다음 C단계지만 생성 API client의 빌드는 유지한다.

## 계약과 검증

- 원본 OpenAPI에서 입력 DTO와 응답 DTO를 분리한다. 요청으로 templateId/snapshot/legacy JSON을 받지 않는다. 생성 요청의 `expectedTemplateVersion`은 연결 Task에 필수, 미연결 Task에는 금지다. template identity·owner·domain은 서버 TaskReference/binding에서 결정한다.
- FieldInput은 fieldId/type 및 정확한 타입의 값 하나를 갖는다. NUMBER/비음수 정수 초 TIME/SHORT_TEXT/CHECK/MEMO를 설계 규칙대로 검증한다. 미입력 키 없음과 0·false를 구별한다. 빈 목록도 유효하다. 중복 ID·unknown/다른 template의 ID·타입 불일치·null·혼합 값 멤버·음수 시간·안전 정수 범위 초과를 거절한다.
- 항목 개수 제한을 새로 넣지 않는다. 요청 전송 한계와 타입·문자열 한계를 구분한다. 모르는 JSON 속성·중복 JSON 키와 숫자/문자열 coercion도 실제 HTTP에서 확인한다.
- snapshot은 templateId/version/이름/domain/kind 및 당시 모든 필드의 이름/type/unit/order를 포함한다. 입력하지 않은 항목의 정의도 유지해 과거 수정 폼에서 사용할 수 있어야 한다.
- 새 Activity는 template 현재 행의 잠금 아래 expected version을 비교하고 저장한다. 폼을 연 뒤 정의가 바뀌면409·부분 저장 없음. 이미 승인된 보관 template의 Task는 마지막 버전으로 기록 가능하다.

## 트랜잭션과 멱등성

현재 Activity 생성은 owner/command advisory lock → 기존 fingerprint 응답 → TaskReference 잠금 순서다. 동적 template version 검증을 기존 성공 command replay보다 앞에 두지 않는다. 과거 성공 요청이 정의 변경·보관 뒤에도 현재 Activity revision을 재전달하는 기존 의미를 유지한다.

Template 관리·binding 승인·Activity create/correct·TaskReference 갱신의 잠금 순서를 명시하고 역순 교착이 없는지 검토한다. 정의 잠금·기록/값/snapshot/outbox 저장은 같은 activity 트랜잭션이다. correction은 원래 version으로만 검증하고 원래 정의나 identity를 갈아끼우지 않는다. 교체 전 전체 revision 이력을 저장하며 void는 자료를 삭제하지 않는다. 서버 간 DB join이나 외부 호출을 이 저장 트랜잭션에 넣지 않는다.

## 저장과 일관된 읽기

- 새 Flyway migration으로 nullable template 연결, 공통 snapshot, detail_format과 형식별 값 저장을 추가한다. 기존 migration/history를 수정하지 않는다.
- 공부 값은 검증 후 scalar map JSONB로 저장한다. 운동·클라이밍 자유값은 타입별 컬럼을 가진 관계형 행으로 저장하고, Activity의 정확한 template version/field에 FK를 건다. type에 맞는 값 컬럼 하나만 존재해야 한다.
- 기존 workout_set과 climbing_round는 계속 관계형이다. 자유 항목 때문에 세트·라운드를 무검증 JSON으로 바꾸지 않는다.
- ActivityService의 단일 statement header/detail snapshot 읽기에 template snapshot과 자유값을 포함한다. 상세·목록·멱등 응답에서 다른 revision의 정의나 값이 섞이지 않아야 한다. 최신 정의를 읽어서 과거 응답에 붙이지 않는다.
- 시간 집계는 실제 구간 우선, 없으면 기존 도메인 정규 시간 fallback을 유지한다. 자유 TIME 필드를 자동 합산하지 않는다. 수정·void의 기존 일정 동기화와 이전/새 날짜·월 의미도 보존한다.

## 기존 데이터 이행

기존 Activity는 LEGACY로 분류하고 모든 원본 값·snapshot·typed detail·history를 유지한다. 기존 Study의 임의 JSON을 필드 정의로 추정하지 않는다. 응답 전용 legacyStudyPayload와 provenance로 읽기만 제공한다. 새 입력은 이 자유 JSON을 거부한다.

legacy correction은 기존 검증 가능한 항목만 수정하고 저장돼 있는 알 수 없는 values/snapshot은 서버가 그대로 보존한다. nested object/array/null/0/false/빈 object/낯선 키를 migration 전후 및 correction 이후에 비교한다. 기존 template 없는 Task의 새 기록은 STANDARD typed 기본 기록을 지원한다. 새로운 template 기록은 TEMPLATE로 구별한다. 이행 때문에 기존 기록이 사라지거나 다른 template에 자동 연결되면 안 된다.

## 필수 증거

1. 실제 HTTP 다섯 타입의 생성·상세·목록·수정·void 왕복, 누락/0/false/타입/ID/중복키 오류와 실패 후 행·outbox 불변.
2. 실제 DB 최초 저장 대 정의 변경 경합 양방향, 성공 command 재전달 대 정의 변경/보관, stale expectedTemplateVersion409, 원래 version correction 및 immutable 이력.
3. 실제 조회 경합에서 header·typed detail·template snapshot·값이 한 revision인지 확인한다. 기존 작업08 barrier를 필요한 범위로 확장한다.
4. legacy migration 데이터와 history의 의미 동등성, 수정 후 임의 자료 보존, 새 무검증 쓰기 거절, STANDARD 호환.
5. 세 도메인 시간 중복 합산 방지·기존 구간 정밀도·PARTIAL/VOIDED 규칙.
6. B1의 연결 기록409 fixture를 실제 template 값 저장→Kafka APPLIED→반복 다음 회차 연결 유지로 교체한다. crafted 이벤트만으로 사용자 기록 성공을 대신하지 않는다.
7. 원본 생성·Java/TS 왕복·drift·영향 서비스/패키지/웹 build. 새 API 준비만으로 전체09 완료를 주장하지 않는다.

별도 완료 보고서는 만들지 않는다. `docs/plans/functional-progress.md`의 공부 기록 흐름 구역에 인터페이스·잠금 순서·마이그레이션/기존자료 증거·실제 명령과 결과·남은 UI 연결을 한 번에 기록한다. 이 구역의 작성 중에는 부모가 같은 문서를 동시에 수정하지 않는다. 유일한 구현 worker로 PowerShell/apply_patch를 사용하며 본인 파일만 한글 scoped commit한다. vault 편집, 하위 agent, push/merge는 금지한다. 계약·서버 연결에 새로운 판단이 필요하면 근거와 함께 부모에게 알린다.
