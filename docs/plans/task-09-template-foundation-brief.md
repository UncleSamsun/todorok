# 작업09 템플릿 정의 기반 구현 인계

작성일: 2026-09-07. 상태: 상세설계 검토를 마친 첫 구현 하위 단계의 요구사항. 구현·검증 완료 보고가 아니다.

## 목표와 선행 기준

`docs/superpowers/specs/2026-09-07-record-templates-design.md`의 단계 A만 구현한다. 작업09 전체 요구는 `docs/superpowers/plans/2026-09-07-mvp-functional-implementation.md` §09이며 기반 관리 완료를 전체 작업09 완료로 표시하지 않는다. 제품 기준은 `dd544ea`, 진행 장부는 `282fd02`다. 실행 시 실제 HEAD·변경 파일을 다시 확인하고 다른 작업의 수정은 보존한다.

먼저 상세설계 §3–6의 버전·형식·관리 API와 §12–14의 검증·결정을 읽는다. PRD §12/AC-4와 DESIGN §9.2의 정의 저장과 일정 생성 분리는 불변 요구다. 아래 구현 경계에서 해결할 수 없는 서비스 연결 문제가 나오면 다음 단계로 기록하고 임시 JSON 또는 클라이언트 대응표로 대체하지 않는다.

## 편집 범위

- `services/activity-service/src/main/java/io/todorok/activity/template/`: RecordTemplate, TemplateVersion, FieldDefinition 및 field identity, 관리 서비스·controller·repository·validation.
- activity의 다음 번호 Flyway migration: 정의와 관리 command 멱등 테이블만 추가. 기존 migration 파일을 수정하지 않는다.
- `contracts/openapi/activity-v1.yaml` 및 필요한 공용 참조 schema: 아래 관리 API와 타입 원본.
- 기존 생성 설정이 가리키는 activity Java/TypeScript 생성물, 필요한 API client export, 계약 fixture·생성 직렬화 검증.
- 위 범위의 실제 DB/HTTP 테스트와 필요한 본문 크기·오류 매핑 설정. 프록시 본문 제한을 다루면 해당 경로의 전송 제약만 변경한다.

기존 ActivityService·ActivityDetailStore·StudyDetail 입력 계약·Task/series·이벤트·웹 UI·legacy 데이터는 이 단계에서 바꾸지 않는다. 공용 FieldInput을 후속단계에서 사용할 예정이라는 이유로 아직 연결되지 않은 기록 쓰기를 추가하지 않는다. 기반 설계에 필요한 닫힌 FieldDefinition/TemplateVersion schema부터 생성한다.

## 구현할 관리 계약

공개 base는 `/api/activity/v1`이며 owner는 인증 컨텍스트에서만 얻는다.

| API | 필수 입력 | 결과 |
| --- | --- | --- |
| POST `/templates` | commandId, name, domain, kind, fields | 최초 version=1·revision=0 반환, 201 |
| GET `/templates` | domain/kind 선택, includeArchived 기본 false, cursor/limit | 소유자 페이지 목록 |
| GET `/templates/{id}` | id | identity·archived·revision·전체 currentVersion |
| GET `/templates/{id}/versions/{version}` | id/version | 이름·순서 포함 불변 과거 정의 |
| POST `/templates/{id}/versions` | commandId, expectedRevision, name, fields | 새 version과 revision, 201 |
| POST `/templates/{id}/archive` | commandId, expectedRevision | 보관 상태, 200 |

템플릿 생성·새 버전·archive에 owner+commandId unique와 fingerprint를 저장한다. 동일 command/동일 payload는 최초 응답, 다른 payload는 409 COMMAND_REUSE다. 사용자 미소유와 없는 ID는 모두 404. 예상 revision 실패·보관 후 수정은 409다. archived 목록 기본 제외와 과거 version 읽기 허용을 동시에 확인한다. 정의 저장에서 Task 생성·일정 이벤트가 없어야 한다.

목록 limit은 기본 20, 최소 1, 최대 100이고 `(created_at DESC, id DESC)` keyset을 인증 owner 조건 및 domain/kind/archive 필터 안에서 적용한다. cursor는 createdAt·id·필터를 표현하며 디코딩·형식·필터 불일치 오류는 400이다. cursor에서 owner를 신뢰하지 않고 인증 owner 조건을 항상 적용한다.

## DB와 validation 규칙

상세설계의 `record_template`, `template_version`, `template_field_identity`, `template_field_definition`을 구현한다. 관리 command 저장에는 owner, commandId, fingerprint, 최초 status/response를 보관한다. 정상 저장과 command 결과 저장은 같은 트랜잭션이다. template의 current_version FK는 최초 version 삽입 순서와 맞추고 DEFERRABLE 제약 등 명시적인 방법으로 원자성을 보장한다. 템플릿만 존재하거나 필드 일부만 저장되는 중간 결과를 커밋하지 않는다.

template identity·owner·domain·kind는 불변이다. 관리 서비스에서 identity 행 잠금과 expectedRevision 비교를 사용하고 version·field는 추가만 한다. DB에서도 과거 version/field update·delete를 막는다. template의 이름은 current_version으로 읽는다. 보관은 identity 상태를 갱신하며 definition을 삭제하지 않는다.

domain-kind 조합은 STUDY/STUDY_CATEGORY, WORKOUT/FREE_WORKOUT, CLIMBING/FREE_HANGBOARD 또는 CLIMBING_SESSION이다. 이름은 1–120자(공백만 금지), 단위는 선택 최대 40자다. NUMBER는 자유 단위, TIME은 초/분/시간 표시 단위이며 생략 시 서버가 초로 정규화한다. 나머지는 단위 금지다. fields=[]도 허용하고 fields 개수 상한은 없다.

정의 입력 배열의 순서를 서버 position으로 저장한다. 중복 fieldId·타 template의 ID·제거된 ID 재사용은 거절한다. 유지한 ID는 현재 version에 존재해야 하며 type을 바꾸지 못한다. type 변경은 새 ID로 추가한다. 숫자·시간·짧은 텍스트·체크·긴 메모의 다섯 정의를 모두 저장한다. 알 수 없는 속성과 중복 JSON 키 거부는 템플릿 관리 요청 경로에 한정하며 공용 파서 설정으로 기존 Activity 계약에 영향을 주지 않는다.

전송 제약 제안은 템플릿 경로에서 실제 읽는 UTF-8 본문 1 MiB(1,048,576바이트)다. chunked도 framing을 제외한 실제 본문을 세고 Content-Length만으로 판단하지 않는다. identity 외 Content-Encoding은 415로 거절한다. 프록시와 앱 제한을 맞추고 초과 시 공통 Problem Details의 413을 반환한다. maxItems로 대체하지 않으며 기존 Activity 경로의 크기 정책은 이 단계에서 바꾸지 않는다.

## 검증 완료 조건

1. 실제 PostgreSQL migration·재실행 경계·FK·unique·불변 version/field 제약을 확인한다. 과거 version 수정/삭제 시도는 DB에서 실패해야 한다.
2. 실제 HTTP로 다섯 형식·단위·순서·빈 fields·많은 fields를 저장하고 GET으로 동일 순서를 확인한다. 이름 변경 뒤 과거 version의 이름도 보존되는지 확인한다.
3. owner 격리·목록 keyset/limit/cursor400·다른 domain/kind·중복/다른/제거된 fieldId·type 변경·공백 이름·부적절 단위·TIME 생략 기본값·본문 초과413·chunked 경계·Content-Encoding415·unknown property·중복 JSON key를 확인한다. 모든 실패에 부분 저장이 없어야 하고 기존 Activity 요청 동작에 영향이 없어야 한다.
4. 동시 두 수정, 수정과 archive를 실제 트랜잭션으로 경합시켜 예상 revision 성공 한 건만 남는지 확인한다. 같은 command 두 요청은 동일 version·동일 최초 응답, 다른 payload 재사용은 409인지 확인한다.
5. archive 후 새 version 거부·active 목록 제외·owner의 과거 조회 허용을 확인한다. 동일 command 재전달을 archive 이후에도 최초 응답으로 복구한다.
6. 생성 원본→Java/TypeScript 생성→직렬화 왕복→재생성 drift gate를 통과한다. 생성물 수동 편집이나 handwritten 대체 DTO를 넣지 않는다.
7. 변경 파일 diff에서 기존 Activity·Task·이벤트·legacy 데이터를 변경하지 않았는지 확인하고, 기반 관리만 완료됐음을 보고한다.

현재 문서는 실행할 검증 목록이지 통과 증거가 아니다. 실제 실행 명령·결과·실패/미실행·파일 경로를 반환하고 승인되지 않은 push·merge를 수행하지 않는다. 검증에 실패하면 완료로 표현하지 않는다.

## 다음 단계에 넘길 복잡한 연결

아래는 설계와 서비스 간 상태 전이를 별도 검토하는 단계 B의 책임이다. 기반 관리 구현자가 임의로 범위를 축소하거나 선행 구현하지 않는다.

- 내부 선택 승인 API와 서비스 인증, template_selection_binding, planner 생성 command·중복·승인 잔존·장애 복구·archive 경쟁.
- Task/series identity 연결, preview version, 반복·이월·과거 회차 연결, 이벤트 v2와 v1 호환, task_reference에서 binding 소유권 검증.
- 기록 최초 저장에서 현재 version 고정, 동시 템플릿 수정 충돌, 서버 snapshot 생성, 원래 version으로 correction.
- Study 자유 값의 서버 검증 JSONB 전환, 운동·클라이밍 typed 관계형 값과 세트·라운드 보존, legacy 자유 object 무손실 읽기·수정 보존.
- 단일 SQL snapshot 조회, revision history, command 재전달, 실제 시간 중복 집계 방지.

그 다음 단계 C에서 관리 UI·`+ 추가`·세 도메인 폼·수정·상세·지난 기록·오늘 목록을 연결한다. 세 단계의 모든 완료 조건과 실제 통합 시나리오를 충족해야 전체09 완료다.
