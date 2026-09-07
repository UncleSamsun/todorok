# 기록 템플릿·카테고리 상세설계

작성일: 2026-09-07. 상태: 승인된 작업09 범위의 실행 설계. §14의 권장 정책을 채택하고 PRD §12.3에 반영했다. 제품 구현·검증 완료를 뜻하지 않는다.

기준: 제품 코드 `dd544ea`, 진행 장부 `282fd02`, `docs/plans/task-09-integration-notes.md`, 전체 기능 계획 §09, PRD §12.1–12.2·AC-4, DESIGN §9.2–9.4. 프로젝트 옵시디언 MOC와 2026-09-07 기능 구현 세션의 작업08 완료·작업09 설계 착수 기록도 확인했다. 확정된 결정은 부모 작업에서 vault에 함께 기록한다.

## 1. 결과와 범위

사용자는 공부 카테고리와 자유 운동·자유 행보드·클라이밍 세션의 기록 템플릿에 항목을 추가·삭제·정렬한다. 정의 저장은 일정을 생성하지 않는다. `+ 추가`에서 기존 정의를 선택하고 날짜·반복을 지정하면 일정에 연결되며, 기록 시 실제 입력한 값만 저장한다. 이후 정의가 바뀌어도 과거 기록과 수정 화면은 원래 정의를 사용한다.

작업09는 기반 관리 API만으로 완료하지 않는다. 관계형 정의, 값 검증, 일정·반복·이벤트 연결, 세 도메인 기록 폼·수정·상세·지난 기록·오늘 목록, 실제 통합 검증을 모두 포함한다. 일반 할 일에는 템플릿이 없으며 기존 즉시 완료 동작을 유지한다. 프로그램 엔진과 내장 클라이밍 타이머 자체 구현은 작업10·12의 범위다. 이번 작업에서 프로그램 선택을 동작하는 것처럼 표시하지 않는다.

## 2. 저장 방식 대안

| 방식 | 장점 | 비용·결함 | 판단 |
| --- | --- | --- | --- |
| 관계형 정의·불변 버전, 공부 값은 검증한 JSONB, 운동·클라이밍 값과 세트·라운드는 관계형 | PRD의 저장 경계와 일치한다. 과거 정의와 소유권을 외래 키로 보장하고 도메인 구조를 보존한다. | 정의 검증과 JSONB 변환을 서버에서 구현해야 한다. | 권장 |
| 모든 자유 값을 관계형 형식별 행으로 저장 | DB 제약·필드별 검색이 균일하다. | 공부 JSONB 요구를 바꾸며 모든 조회의 조인·집계 복잡도가 증가한다. | 채택하지 않음 |
| 템플릿과 모든 도메인 상세를 자유 JSON 문서 하나에 저장 | 최초 구현이 짧다. | 관계형 정의·세트·라운드 요구를 충족하지 못하고 소유권·타입·수정 경계가 흐려진다. | 채택하지 않음 |

## 3. 식별자와 버전의 고정 시점

서로 다른 세 버전을 혼용하지 않는다. `templateRevision`은 현재 템플릿 관리 경쟁 제어 번호, `templateVersion`은 불변 정의 번호, Activity `version`은 기록 수정 revision이다.

| 대상 | 저장·고정 값 | 이후 정의 변경의 영향 |
| --- | --- | --- |
| RecordTemplate | identity `templateId`, owner, domain, kind | identity·owner·domain·kind는 변경 불가. 이름·필드 변경은 새 version |
| TemplateVersion | `(templateId, templateVersion)`, 이름, 필드 이름·형식·단위·순서 | 생성 뒤 UPDATE·DELETE 금지 |
| Task 또는 series 신규 선택 | 서버가 승인한 `bindingId`, `templateId`, `selectedTemplateVersion` | 선택 당시 버전은 등록 미리보기의 근거이며 기록 버전은 아님 |
| 반복의 다음 Task | series의 동일 binding·identity·선택 당시 버전을 복사 | 재로그인·이월·skip·다음 회차에도 연결 유지 |
| 새 Activity 최초 저장 | 해당 identity의 현재 version을 트랜잭션 안에서 확정, 서버 snapshot 저장 | 이후 정의 변경의 영향 없음 |
| Activity correction | 기존 Activity의 templateId·templateVersion·snapshot을 그대로 사용 | 현재 정의나 현재 보관 상태로 갈아끼우지 않음 |

기록 폼은 서버가 돌려준 현재 version을 `expectedTemplateVersion`으로 제출한다. 저장 시 현재 version과 다르면 409로 거절하고 초안을 보존한다. 사용자가 최신 항목을 확인한 뒤 다시 저장한다. 폼을 열었다는 이유로 DB 버전을 고정하거나 GET이 데이터를 변경하지 않는다. PRD의 “이후 기록부터 적용”은 새 Activity가 처음 저장되는 시점으로 해석한다. 과거 날짜로 새 기록을 추가해도 현재 정의를 사용하고, 이미 저장한 과거 Activity 수정은 원래 정의를 사용한다.

기록 저장은 template identity 행에 잠금을 잡고 현재 version 비교부터 Activity·값·snapshot·outbox 삽입까지 같은 activity 트랜잭션에서 수행한다. 정의 변경·보관도 같은 행을 잠그므로 새 정의와 최초 기록 저장의 선후가 명확하다. 멱등 command 재전달은 먼저 기존 결과를 찾아 반환하므로 그 사이 정의가 바뀌어도 새 version으로 재해석하지 않는다.

## 4. 정의 모델과 입력 규칙

템플릿의 `domain`은 `STUDY | WORKOUT | CLIMBING`, `kind`는 각각 `STUDY_CATEGORY | FREE_WORKOUT | FREE_HANGBOARD | CLIMBING_SESSION`이다. 서버는 허용된 domain-kind 조합과 Task 유형을 검사한다. 다른 도메인으로 옮기려면 새 정의를 만든다.

`FieldDefinition`은 `fieldId`, `name`, `type`, `unit`, `position`으로 구성한다. type은 아래 다섯 개다. 이름은 비어 있지 않은 최대 120자, 단위는 선택 가능한 최대 40자다. 단위는 표시용이며 자동 환산 규칙이 아니다. 체크·텍스트·메모에는 단위를 받지 않는다. 시간은 전송·저장 시 초 단위이고 단위 표시는 초·분·시간 중 하나로 제한해 폼에서 변환한다. 시간 단위를 생략하면 서버가 초로 정규화한다. 숫자의 단위는 쪽·문제·회·kg·km 등 사용자 문자열이다.

| type | 입력 API의 값 멤버 | 저장 값 | 검증·빈 값 |
| --- | --- | --- | --- |
| NUMBER | `numberValue` | JSON number / 관계형 numeric | 유한한 JSON 숫자만 허용. 문자열 숫자·NaN·Infinity 불가. 0 보존 |
| TIME | `timeSeconds` | 비음수 정수 | 정수 초, JavaScript 안전 정수 범위 안. 음수·소수 초·문자열 불가. 0 보존 |
| SHORT_TEXT | `textValue` | 문자열 | 최대 120자. 공백뿐이면 미입력으로 정규화 |
| CHECK | `checked` | boolean | 미설정·true·false를 구별. false는 명시한 값 |
| MEMO | `memoValue` | 문자열 | 최대 20,000자. 공백뿐이면 미입력으로 정규화, 실제 내용의 줄바꿈·공백은 보존 |

API 입력은 `FieldInput[]`로 받는다. 각 원소는 `fieldId`, `type`과 형식에 맞는 값 멤버 정확히 하나만 가진다. nullable 값·빈 원소·다른 값 멤버 혼합·중복 fieldId는 거부한다. 예를 들어 `{fieldId: A, type: NUMBER, numberValue: 0}`와 `{fieldId: B, type: CHECK, checked: false}`는 둘 다 저장한다. 비어 있는 필드는 배열에 포함하지 않으며 빈 배열도 유효하다. 공부 DB의 `values_json`은 검증·정규화 뒤 `{fieldId: scalar}` 객체로 만든다. 임의 중첩 객체·배열을 허용하지 않는다.

정의 저장 입력도 순서 있는 `fields[]`를 받는다. 서버가 배열 순서로 연속 position을 부여하며 클라이언트 position은 받지 않는다. 새 필드는 새 UUID를 사용하고 중복 ID, 타 템플릿에 속한 ID, 이전에 제거된 ID의 재사용을 거부한다. 같은 ID는 이전 current version에 있던 항목의 이름·단위·순서 변경에만 유지할 수 있다. 형식 변경은 새 fieldId로 생성한다. 이 규칙으로 오래된 숫자가 새 체크 값으로 오인되는 것을 막는다. 단위 변경도 기존 값 자동 환산을 하지 않는다.

필드 개수에 `maxItems`, UI 추가 버튼 제한, DB 개수 제한을 넣지 않는다. 전송 제약은 템플릿 관리 경로에서 실제 읽는 UTF-8 본문을 1 MiB(1,048,576바이트)까지 허용하는 것으로 제안한다. chunked 전송도 framing을 제외한 본문 바이트를 스트림에서 세고 Content-Length만 신뢰하지 않는다. 압축 Content-Encoding은 지원하지 않아 identity 외 인코딩은 415로 거부한다. 프록시와 앱의 제한을 일치시키고 크기 초과는 공통 Problem Details의 413으로 응답하도록 설정·검증한다. 이는 항목 수 제한이 아니다. 문자열 길이·숫자 표현·본문 크기 초과를 묵시적으로 잘라 저장하지 않는다. 기존 Activity 경로의 제한을 기반 단계에서 일괄 변경하지 않으며 기록 경로에는 단계 B에서 동일 전송 계약을 연결한다. 목록은 페이지를 나누되 한 version의 필드 조회 결과는 잘라내지 않는다. 정의 전체가 크더라도 조회 응답을 요청 크기 기준으로 잘라내지 않는다.

## 5. DB 소유권과 제약

신규 테이블은 activity 스키마 소유다. planner 계정에 activity 테이블 SELECT 권한을 추가하지 않는다.

- `record_template(id, user_id, domain, kind, current_version, revision, archived_at, created_at)`: 최초 revision=0이며 최초 definition version=1과 구별한다. owner 목록 인덱스. current_version은 자기 version만 참조한다. 보관은 archived_at과 revision만 변경한다. 물리 삭제·보관 해제 기능은 이번 범위에 없다.
- `template_version(template_id, version, name, created_at)`: 복합 PK, 이름도 버전에 속한다. 최초 version은 1, 새 저장은 +1이다. 부모 잠금과 예상 revision 비교를 통해 동시 새 version 한 건만 커밋한다. 자기 current와 field를 같은 트랜잭션에서 저장한다.
- `template_field_identity(template_id, field_id, created_version, type)`: fieldId의 소속·원래 형식을 유지한다. field_id 전역 unique. 제거 후 재사용도 이 이력으로 검출한다.
- `template_field_definition(template_id, version, field_id, name, type, unit, position)`: version·field identity FK, PK `(template_id, version, field_id)`, unique `(template_id, version, position)`, position 비음수, type-unit 제약. version·field identity·definition의 수정/삭제를 DB 트리거 또는 권한으로 차단한다.
- `template_selection_binding(id, user_id, template_id, selected_version, target_type, target_id, request_id, request_fingerprint, created_at)`: target_type은 TASK 또는 SERIES. unique `(user_id, request_id)`, unique `(target_type, target_id)`. 승인 행과 선택 당시 version은 불변이며 템플릿을 물리 삭제하지 않는다. 다른 소유자·다른 대상·다른 요청 내용으로 승인 재사용을 거부한다.
- `activity_record`: nullable `template_id`, `template_version`, `template_snapshot JSONB`, `template_binding_id`, `detail_format`을 추가한다. templateId/version은 함께 없거나 함께 있고, 둘 다 있으면 자기 version FK를 가진다. snapshot에는 schemaVersion·template 이름/domain/kind·version·모든 필드 정의가 들어간다. 없는 값의 정의도 snapshot에는 남아 수정 폼에서 선택 입력할 수 있다.
- `study_detail.values_json`: 새 템플릿 기록은 위 규칙으로 서버가 만든 scalar map. 새 기록의 snapshot은 activity_record의 공통 컬럼을 유일 원본으로 사용한다. 기존 `study_detail.snapshot`은 이행 기간의 legacy 원본으로 남긴다.
- `activity_field_value(activity_id, template_id, template_version, field_id, type, number_value, time_seconds, text_value, checked, memo_value)`: 운동·클라이밍의 자유 항목 값. Activity의 복합 unique 키와 definition에 FK를 걸어 다른 기록 버전의 필드를 연결하지 못하게 한다. PK `(activity_id, field_id)`, type별 값 컬럼 정확히 하나만 non-null. 행이 없으면 미입력이다.
- 기존 `workout_detail/workout_set`, `climbing_detail/climbing_round`를 계속 사용한다. 세트·라운드 자유 JSON 대체는 금지한다. 자유 항목은 활동 전체의 부가 기록이고 기존 세트 횟수·무게·시간이나 라운드 시도·완등을 저장하는 별도 행을 없애지 않는다. 세트·라운드의 순서·0·false는 기존 관계형 제약을 유지한다.
- `activity_revision_history`는 기존 불변 이력 정책을 유지한다. 교체 전 header·typed detail·templateId/version/snapshot·자유 값·legacy 원본을 포함한 전체 revision을 같은 트랜잭션에 남긴다.

planner에는 `task`와 `task_series`의 nullable bindingId/templateId/selectedTemplateVersion 및 서버가 승인한 표시용 이름·필드 요약을 추가한다. 로컬 필드는 함께 NULL 또는 함께 존재해야 한다. identity 링크는 생성 후 변경 불가로 한다. 날짜·제목·반복 규칙 편집으로 카테고리를 바꾸지 않으며 다른 카테고리 일정은 새로 추가한다. activity의 `task_reference`도 같은 링크와 seriesId를 저장한다. 서비스 간 FK나 직접 join은 만들지 않는다.

## 6. API와 생성 계약

아래는 구현할 정확한 인터페이스 제안이다. 공개 계약은 `contracts/openapi/activity-v1.yaml`, planner 요청·응답은 `planner-v1.yaml`, 내부 API는 신규 `contracts/openapi/template-internal-v1.yaml`이 원본이다. 공용 schema는 하나의 원본을 `$ref`로 참조하고 생성 설정·생성 Java/TypeScript·재생성 drift를 함께 변경한다. 생성 타입을 수동으로 덧대지 않는다. `FieldInput`과 응답 snapshot은 자유 object 대신 닫힌 명시적 멤버를 사용하며 생성기가 처리하지 못하는 조합 검사는 서비스에서 강제하고 직렬화 왕복으로 확인한다.

| 메서드·경로 | 요청 | 응답·정책 |
| --- | --- | --- |
| POST `/api/activity/v1/templates` | commandId, name, domain, kind, fields | 201 TemplateResponse; owner는 인증에서 가져옴. Task/outbox 일정 이벤트 없음 |
| GET `/api/activity/v1/templates` | domain, kind 선택, includeArchived 기본 false, cursor, limit | 200 페이지. 필드 개수·목록 개수와 별개. 타 owner 제외 |
| GET `/api/activity/v1/templates/{templateId}` | 없음 | 200 identity·archived·revision·current version·전체 fields. 보관된 정의도 owner는 조회 가능 |
| GET `/api/activity/v1/templates/{templateId}/versions/{templateVersion}` | 없음 | 200 불변 버전. 이전 버전·보관된 버전도 owner 조회 가능 |
| POST `/api/activity/v1/templates/{templateId}/versions` | commandId, expectedRevision, name, fields | 201 새 version과 새 revision. archived는 409 |
| POST `/api/activity/v1/templates/{templateId}/archive` | commandId, expectedRevision | 200 보관 상태. 이미 처리된 동일 command는 같은 결과 |
| GET `/api/activity/v1/tasks/{taskId}/record-template` | 없음 | owner TaskReference·binding 검증 후 templateId, current templateVersion, archived, 이름, fields. template 없는 기존 Task는 link 없음. projection 미도착은 409 TASK_NOT_READY |
| POST `/internal/activity/v1/template-selections` | requestId, ownerId, targetType, targetId, taskType, templateId, expectedTemplateVersion | 201 불변 binding·선택 당시 version·서버 표시 metadata. 내부 서비스 전용 |

TemplateResponse는 `templateId, domain, kind, archived, revision, currentVersion`을 포함하며 currentVersion은 `templateVersion, name, fields`를 가진다. 관리 command마다 owner·commandId unique와 요청 fingerprint를 저장한다. 같은 command/동일 요청은 최초 응답, 다른 요청은 409 COMMAND_REUSE다. 낙관적 실패에는 새 commandId와 최신 expectedRevision으로 사용자가 검토한 변경을 다시 제출한다. 서버가 사용자의 충돌 초안을 덮어쓰지 않는다.

템플릿 목록의 limit은 기본 20, 최소 1, 최대 100이다. `(created_at DESC, id DESC)` keyset을 owner 조건과 domain/kind/archive 필터 안에서 적용한다. cursor는 createdAt·id·필터를 표현하는 불투명 문자열이며 디코딩·형식·필터 불일치 오류는 400이다. cursor의 경계값과 무관하게 owner 조건은 인증에서 다시 적용한다. offset 페이지나 전역 목록을 조회한 뒤 클라이언트에서 owner를 거르는 방법은 사용하지 않는다. 기반 단계의 unknown property·중복 JSON key 거부는 템플릿 관리 요청 경로에 한정한다. 기존 Activity 역직렬화를 공용 설정 변경으로 함께 강화하지 않고 단계 B의 호환 전환에서 별도로 적용한다.

planner의 CreateTaskRequest/CreateSeriesRequest에는 선택적인 `templateSelection: {templateId, expectedTemplateVersion}`와 `commandId`를 추가한다. 연결된 요청에서는 commandId가 필수다. 기존 template 없는 생성은 호환을 유지한다. 일반 Task에 templateSelection은 400. TaskResponse/SeriesResponse에는 서버의 `templateLink: {bindingId, templateId, selectedTemplateVersion, name, fieldSummary}`를 반환하고 프런트가 저장한 대응표는 사용하지 않는다. fieldSummary는 선택 당시 version의 필드 이름·단위로 만든 최대 240자 표시 문자열이며 전체 정의는 version 조회 API에서 가져온다. 표시 요약 길이가 정의 또는 입력 항목 개수를 제한하지 않는다. 편집 API에는 templateSelection을 추가하지 않는다.

CreateActivityRequest에는 선택적인 `expectedTemplateVersion`을 추가한다. 이는 폼에서 얻은 현재 version이며, TaskReference에 링크가 있으면 필수, 링크가 없으면 금지다. templateId는 클라이언트로부터 받지 않고 TaskReference로 결정한다. domain detail에는 `fields: FieldInput[]`를 추가하고 형식별 기존 세트·라운드도 계속 받는다. 응답은 `template: {templateId, templateVersion, snapshot}`과 검증된 필드 값을 반환한다. `snapshot`은 별도 응답 schema이며 생성/수정 요청 schema에는 존재하지 않는다.

CorrectActivityRequest는 기존 expectedVersion과 편집할 detail만 받고 templateId·templateVersion·snapshot 변경은 받지 않는다. 서버는 원래 기록 version에서 정의를 찾고 같은 field ID의 값만 교체한다. 전체 교체 의미는 기존과 같아 fields에서 빠진 값은 삭제되지만, legacy 불명 자료는 §10 규칙으로 별도 보존한다. 읽기/쓰기 DTO를 분리해 `readOnly` 표시만으로 요청 거부를 대신하지 않는다. 알려지지 않은 JSON 속성과 중복 JSON 키도 파싱 단계에서 거부한다.

공통 오류 계약의 `code`, `retryable`, field 오류 표현을 사용한다. field 오류에는 해당 fieldId와 오류 위치를 넣되 타 owner 자료는 노출하지 않는다. 오류 응답도 원본 계약에 포함한다.

## 7. 신규 선택 승인과 서비스 장애·경쟁

단순히 planner가 템플릿을 GET한 후 DB에 쓰면 그 사이 archive가 완료될 수 있다. 따라서 activity가 영구 선택 승인을 발급한 시점을 신규 선택의 선형화 지점으로 삼는다. 이는 분산 트랜잭션이 아니며 다음 순서를 명시한다.

1. planner는 인증 owner와 commandId로 생성 요청을 짧은 로컬 트랜잭션에 등록한다. `planner_creation_command(owner_id, command_id, fingerprint, target_type, target_id, state, result)`에 대상 UUID를 미리 정하고 PENDING을 저장한다. 같은 command와 같은 payload의 재요청은 동일 targetId를 재사용하고, 내용·owner·targetType 변경은 허용하지 않는다.
2. 외부 호출 동안 planner의 Task/series 잠금을 유지하지 않는다. planner가 내부 선택 승인 API를 호출한다. 브라우저는 bindingId를 직접 제출하거나 임의 ownerId로 이 API를 호출할 수 없다. 내부 경로는 공개 프록시로 노출하지 않고 planner 전용 서비스 서명·audience·scope 검증을 사용한다. actor ownerId는 인증한 planner의 서명된 요청에서만 받는다. 서비스 인증 키는 사용자 access-token 키와 분리한다.
3. activity는 이미 같은 requestId의 binding이 있으면 fingerprint·owner·대상·domain을 비교해 최초 승인을 반환한다. 새 요청이면 template owner를 확인하고 identity 행 잠금 아래 archived=false 및 expected version 일치를 검사한 후 binding을 커밋한다. 타 owner·존재하지 않는 template는 모두 404. archive가 먼저 커밋했으면 409 TEMPLATE_ARCHIVED이고 승인 행을 만들지 않는다.
4. planner는 응답이 요청 target·owner·domain·template와 일치하는지 확인한 후 로컬 트랜잭션에서 생성 command를 잠그고 Task 또는 series·첫 Task·outbox·완료 결과를 함께 커밋한다. 동시에 같은 요청이 들어오면 이 잠금·unique로 한 결과만 생긴다.
5. 단계3 뒤 archive가 완료되어도 먼저 승인된 같은 생성 요청은 완료할 수 있다. 단계4가 실패하면 activity의 승인만 남을 수 있다. 이 승인에는 Activity·Task 생성 효과가 없고 등록 수에도 포함되지 않는다. 같은 command 재시도만 승인된 대상에 재사용할 수 있다. 다른 Task·series·사용자·도메인·변경한 payload로 전용할 수 없다. 자동 삭제나 만료는 이번에 넣지 않는다. 향후 정리에는 planner 존재 확인과 재시도 안전 기준이 필요하다.

activity 중단·내부 timeout은 503 TEMPLATE_SERVICE_UNAVAILABLE이며 planner는 Task·series·일정 outbox를 생성하지 않는다. PENDING 생성 요청은 보존하고 사용자가 동일 command로 재시도한다. 승인 응답 유실도 동일 requestId로 복구한다. planner 최종 응답 유실은 완료 command 결과를 재전달한다. 확정 거절 후 수정한 요청은 새 command를 사용한다. 승인 전에 실패한 요청과 승인 후 보관된 요청을 구별할 수 있어야 한다.

보관 후 기존 Task 기록·기존 series 다음 회차에는 새 선택 승인 API를 호출하지 않는다. Task/series의 서버 binding이 그 권한의 근거다. 기존 series archive만이 기존 정책대로 다음 회차 생성을 중지한다. 템플릿 archive를 series archive로 확대하지 않는다. 기존 Task 이월·skip·과거 완료 후 다음 회차·재개는 현재 날짜 및 unique 정책에 따라 같은 링크를 보존한다. 새 series를 만드는 행위는 신규 선택이므로 보관된 템플릿으로는 거부한다.

## 8. 이벤트·projection 연결

현재 TaskScheduled v1은 닫힌 payload이므로 기존 v1 schema를 확장한 척 payload만 바꾸지 않는다. `task-scheduled`, `task-changed`, `task-rolled-over`의 v2 schema·fixture·생성 event 모델·등록/검증 코드를 추가한다. v2 payload에는 기존 필드와 `seriesId` 및 `templateLink`가 있고 둘은 없는 경우 명시적 null이다. templateLink는 bindingId/templateId/selectedTemplateVersion을 담는다. 표시 snapshot을 클라이언트에서 이벤트로 복사하지 않는다. `series-changed` v2에도 같은 링크를 포함한다.

기존 topic과 event type을 유지하되 envelope version=2로 구분한다. 소비자가 v1·v2를 모두 읽도록 먼저 배포하고 그 다음 producer를 전환한다. EventJson·schema registry·생성물·Kafka fixture도 같이 갱신한다. v1은 template 없는 과거 이벤트로 해석한다. 이미 연결된 TaskReference에 늦게 도착한 v1이나 link 변경 v2가 연결을 지우거나 다른 binding으로 바꾸지 못하게 한다.

ActivityEventConsumer는 새 TaskReference를 만들 때 같은 activity DB의 binding을 검증한다. owner/template/version이 일치하고 TASK binding은 targetId=taskId, SERIES binding은 targetId=payload.seriesId여야 한다. Task 유형과 template domain도 일치해야 한다. 이후 이벤트에는 동일한 immutable 링크만 허용한다. aggregateVersion 순서·inbox 중복 방지·DELETED 종단 처리도 유지한다. malformed link, 잘못된 owner, 잘못된 series로 된 이벤트는 projection을 만들지 않고 기존 재시도·격리 정책으로 처리한다.

TaskEvents.publish의 모든 명령 경로, SeriesService.generate, Task.occurrence, 수동 생성, 이월, skip, 완료 후 다음 회차, 과거 회차 복구에서 링크를 전달한다. series 수정으로 이미 생성된 Task의 링크를 갱신하지 않는다. 새 로그인은 TaskResponse와 record-template API에서 링크를 다시 읽으므로 클라이언트 저장소가 없어도 복구된다.

ActivityCompleted/Corrected/Voided의 일정 동기화 의미는 유지한다. completionSummary는 서버 snapshot과 검증 값에서 만든다. 모든 필드 값을 요약에 넣지 않고 제한된 표시 요약만 전달한다. 상세는 Activity에서 조회한다. 현재 작업08의 command 멱등·revision 비교·APPLIED/CONFLICT 확인·단일 완료 기록 unique 정책을 약화시키지 않는다.

## 9. 조회 일관성과 시간 집계

ActivityService의 단일 SQL header/detail snapshot 조회에 template snapshot과 관계형 자유 값을 함께 포함한다. 읽는 중 별도 쿼리로 최신 템플릿을 붙이지 않는다. 목록·상세·중복 command 재응답도 같은 revision의 template·값·세트·라운드를 반환해야 한다. correction은 교체 전 이력을 보존하고 원래 template version으로 검증한 뒤 현재 상세를 교체한다. void도 이력·연결을 삭제하지 않는다.

시간 합계는 실제 수행 시간을 한 번만 센다. 작업08의 우선순위인 실제 시작·종료 구간 우선, 없으면 도메인 정규 시간(운동 세트 시간 합/공부 durationMinutes/클라이밍 durationSeconds), 모두 없으면 0을 유지한다. 자유 TIME 필드는 월 시간 합계에 더하지 않는다. 예를 들어 실제 구간 30분과 `집중 시간 20분`, `휴식 10분`이 있어도 월 합계는 30분이다. 자유 필드만 입력했다면 그 값은 상세에 표시하고 월 시간에 자동 합산하지 않는다. 기록 폼의 공통 실제 시간 입력과 자유 항목은 서로 값을 자동 복사하지 않는다.

시간 필드 이름이나 단위가 같다고 집계 역할을 추론하지 않는다. 프로그램·타이머의 정규 수행 시간 연결은 해당 후속 작업에서 기존 시간 소스 하나로 전달한다. 임의 필드의 집계 지정 기능은 이번 범위가 아니다. correction·void의 이전/현재 수행 날짜 및 월 캐시 무효화, revision 최신 결과만 반영하는 보호, 서울 월 경계 정책을 유지한다.

## 10. 기존 데이터 무손실 이전

현재 Study values와 snapshot은 검증되지 않은 object이므로 이를 사실인 정의로 승격하거나 키 이름으로 형식을 추측하지 않는다. 마이그레이션은 확장 → 이중 형식 읽기 → 새 쓰기 전환으로 진행한다.

1. nullable 링크·불변 정의·관계형 값 테이블·detail_format을 추가한다. 기존 활동은 LEGACY로 분류하고 header, values_json, snapshot, subject, durationMinutes, 세트·라운드, revision history를 그대로 둔다. 필드가 없는 정상 기록도 보존한다. 기존 history JSON을 수정하지 않는다.
2. 새 API 읽기는 `LEGACY | TEMPLATE | STANDARD`를 구분한다. LEGACY 응답에는 응답 전용 `legacyStudyPayload: {values, snapshot}`으로 원래 JSONB 자료를 반환한다. 이 응답 영역만 arbitrary JSON을 허용하며 새 입력 형식으로 재사용하지 않는다. 서버가 생성하지 않았다는 provenance도 함께 표시한다. 과거 값은 읽기 가능한 key/value와 필요 시 원문 보기로 제공하고 HTML로 실행하지 않는다.
3. legacy correction은 기존 subject·duration·메모·수행 시간·세트·라운드처럼 의미가 검증되는 항목만 수정한다. old values/snapshot은 서버가 보관 중인 값을 그대로 보존하고 request로 대체하지 않는다. 해당 자유 자료를 새 template로 자동 연결하지 않는다. 사용자가 기존 기록을 수정해도 모르는 필드가 사라져서는 안 된다.
4. 배포 이후 template 없는 기존 Task의 새 기록은 STANDARD로 저장하고 기존 typed 기본 폼을 허용한다. 임의 Study values/snapshot 새 쓰기는 400으로 거부한다. 새 연결 Task에는 TEMPLATE 경로를 사용한다. 현재 template 없는 기존 Task나 series에 임의 카테고리를 추정해 채워 넣지 않는다.
5. 기존 프런트가 읽은 snapshot을 그대로 PATCH하는 경우 명시적 오류를 주며 이전 데이터를 바꾸지 않는다. 사용자 초안이 남은 경우 최신 폼에서 검증된 필드만 다시 저장하도록 안내하고 원문 초안은 버리지 않는다. 읽기 계약과 클라이언트 전환을 먼저 준비한 다음 새 입력 거부를 켠다.

보존의 단위는 기존 JSONB 내용·기존 기록 의미다. 이미 JSONB가 정규화한 원래 JSON의 키 순서나 입력 바이트 재현을 약속하지 않는다. 이전 검증 fixture에는 중첩 객체·배열·null·0·false·낯선 키·빈 object·잘못된 옛 snapshot을 넣고 전후 의미 동등성과 correction 후 잔존을 확인한다. 원래 데이터 또는 이전 history를 삭제하는 migration은 허용하지 않는다.

## 11. 프런트 연결

- `features/templates/TemplateEditor.tsx`: 이름, 다섯 형식, 형식별 단위, 추가·삭제·정렬, 저장·보관. 필드 개수 제한 없음. 드래그 외 위/아래 이동 버튼과 키보드 조작을 제공한다. 정의 저장 뒤 `+ 추가`의 원래 날짜·반복 초안을 유지한 채 새 template 선택으로 돌아간다. 저장 시 Task 생성 요청을 호출하지 않는다.
- `features/templates/RecordFields.tsx`, `packages/validation/src/record-fields.ts`: 생성 계약 타입을 사용한다. `Map<fieldId, 입력 상태>`는 빈 값·0·false를 구별하며 CHECK는 미설정/예/아니오 또는 체크와 지우기를 제공한다. DOM의 기본 false를 미입력인 것처럼 일괄 저장하지 않는다.
- `QuickAdd.tsx`, `TaskEditor.tsx`, `RecurrenceFields.tsx` 및 도메인 추가 진입: 도메인·자유 유형에 맞는 active 정의를 조회하고 정의 이름·단위·순서를 먼저 보여준다. GENERAL은 템플릿 미선택. 관리 화면 왕복 시 일정 draft를 보존한다. templateSelection과 commandId는 서버 요청까지 전달한다.
- `TaskGroups.tsx`와 도메인 오늘 목록: TaskResponse의 서버 연결 metadata로 `알고리즘 · 문제 수·소요 시간`, `자유 운동 · 횟수 기록` 등을 표시한다. 등록 당시 기준을 표시하고, 기록 폼은 최신 정의를 로드해 바뀐 항목을 보여준다. 과거 완료 행·상세는 Activity snapshot을 사용한다.
- `RecordPage.tsx`, `StudyFields.tsx`, `WorkoutFields.tsx`, `ClimbingFields.tsx`: record-template을 읽은 후 사용자 정의 필드와 기존 세트·라운드 및 실제 시간 입력을 연결한다. 템플릿 없는 기존 Task는 기본 typed 폼. projection 미도착은 재조회 가능한 준비 상태다. 없는 템플릿으로 조용히 저장하지 않는다.
- `ActivityRecordPage.tsx`, `CorrectionDrafts.tsx`, `CompletedTaskRecord.tsx`: GET으로 받은 해당 revision snapshot으로 수정 폼과 상세를 구성한다. fields 값이 없어도 snapshot의 전체 정의를 편집 폼에 보여준다. 조회 재검증 때문에 사용자가 편집 중인 revision·정의를 갈아끼우지 않는다.
- `DomainPage.tsx`의 지난 기록: 날짜→Task/루틴 선택→같은 기록 폼→과거 날짜의 오늘 화면 흐름을 유지한다. 새 과거 일정이 필요하면 동일 템플릿 선택 API를 사용한다. 기존 보관된 정의의 Task를 선택한 기록은 허용한다.

정의 편집·archive 409는 최신 정의와 자기 초안을 모두 남기고 재검토한다. 기록 중 새 version 409에서는 동일 ID·동일 type·동일 unit의 값만 새 폼으로 옮길 후보로 보여준다. 제거·형식·단위 변경된 값은 별도 “이전 입력”에 남기고 자동 폐기·자동 환산하지 않는다. 사용자가 적용을 확인해야 새로운 요청 commandId를 만든다. 불확실한 저장 응답에서는 기존 commandId와 payload를 유지해 기존 작업07·08의 중복 방지 정책을 따른다. 로그아웃·계정 전환 시 템플릿 캐시·초안도 owner 경계로 정리한다.

## 12. 상태·오류·검증 행렬

| 상황 | 기대 결과 | 필수 검증 경계 |
| --- | --- | --- |
| 다섯 형식 생성·이름/단위 변경·정렬 | 새 version만 증가, 이전 정의 불변 | 실제 DB 제약·HTTP·생성 직렬화 |
| fields=[] / 많은 항목 / 1 MiB 초과 | 빈 정의 허용 / 개수 상한 없음 / 413·부분 저장 없음 | UI 추가·API·프록시 바이트 경계 |
| 누락·0·false·공백 문자열 | 누락만 비저장, 0·false 보존 | validator·DB readback·수정 왕복 |
| 다른 owner/template field·중복 ID·unknown field | 404 또는 400, 아무 기록도 생성하지 않음 | HTTP·DB rollback |
| 잘못된 type·혼합 멤버·음수 시간·null·중복 JSON key | 400 FIELD_VALUE_INVALID 등 | 실제 HTTP 파서부터 저장 경계 |
| 동시 정의 수정 / 수정과 archive | 하나의 예상 revision만 성공, 패자는 409 | 동시 실제 DB 트랜잭션 |
| 새 선택과 archive 경쟁 | 먼저 커밋한 승인만 유효, archive 선행이면 거부 | 내부 API·planner 통합 |
| 승인 성공 뒤 planner 실패·timeout·응답 유실 | Task 없음 또는 한 건, 동일 command 복구, 잔존 승인 재사용 제한 | 장애 주입·DB/outbox 개수 |
| 타 owner/domain/target로 binding 재사용 | 거부, 정보 노출 없음 | 내부 인증·이벤트 projection |
| 새 version 생성 중 기록 저장 | 저장 시 version 비교, 구 폼은 409 또는 먼저 저장된 원 version | 두 트랜잭션 경쟁 |
| snapshot 위조·다른 templateVersion 제출 | request 속성 거부 또는 expected version 충돌 | 원본 schema·HTTP·서버 생성 snapshot 비교 |
| archive 후 신규 Task/series 선택 | 409, 승인 없는 새 일정 생성 없음 | 실제 서버·브라우저 |
| archive 후 기존 Task·series 다음 회차·이월·과거 완료 | 기존 binding 유지, 기록 허용, 반복 unique 유지 | planner→outbox→실제 소비자→Activity |
| 이벤트 중복·역순·v1/v2 혼재 | 연결 유지, 낮은 version이 덮지 않음 | schema fixture·Kafka·DB |
| 새 로그인·새 기기 상태 / 최초 정의 저장 | 서버에서 선택 복구 / Task 수 불변 | 브라우저·서버 조회 |
| 템플릿 변경·보관 뒤 correction | 원래 이름·type·unit·순서·version 유지, 값만 수정 | 상세/목록/멱등 재응답·revision history |
| correction과 상세/목록 조회 경합 | 한 revision의 header·정의·값·세트·라운드 | 작업08 단일 SQL 경합 검증 확장 |
| 자유 TIME 둘+실제 구간 / legacy 정규 시간 | 합계 중복 없음, 기존 fallback 유지 | 세 도메인·서울 월 경계·void/correction |
| legacy 자유 object 이행·수정 | 원문 JSONB와 history 보존, 신규 무검증 쓰기 차단 | migration fixture·HTTP·DB 비교 |
| 모바일·데스크톱·키보드·라이트/다크 | 항목 추가/정렬/지우기·긴 이름/메모·오류 복구 가능 | 390px·768px 이상 실제 화면 |

오류 코드는 최소 `VALIDATION_FAILED`, `FIELD_VALUE_INVALID`, `FIELD_ID_DUPLICATE`, `FIELD_UNKNOWN`, `TEMPLATE_VERSION_CONFLICT`, `TEMPLATE_ARCHIVED`, `COMMAND_REUSE`, `TASK_NOT_READY`, `TEMPLATE_SERVICE_UNAVAILABLE`, `PAYLOAD_TOO_LARGE`를 계약화한다. 타 owner 조회는 NOT_FOUND. 확정 입력·버전 오류는 retryable=false, projection 미도착·일시 서비스 장애는 true로 하고 사용자 재검토가 필요한 오류를 자동 무한 재시도하지 않는다.

## 13. 독립 구현 단계와 완료 판정

### 단계 A — 정의 기반 관리

관계형 template/version/field identity·definition, 관리 OpenAPI와 생성물, 관리 command 멱등, owner·revision·archive·형식·단위·순서·본문 검증, 조회 API를 구현한다. 승인 binding이나 Task 이벤트를 먼저 임시 구현하지 않는다. 실제 DB/HTTP로 불변성·경쟁·소유권·생성 drift를 확인한 후 기반 단계만 완료 판정한다. 실행 인계는 `docs/plans/task-09-template-foundation-brief.md`다.

### 단계 B — 일정·기록·서비스 연결

내부 선택 승인 계약·서비스 인증·binding·planner 생성 command, Task/series 저장 및 응답, 이벤트 v2와 projection, Activity 생성/수정 검증·snapshot·관계형 자유 값, legacy 이행·일관된 조회·시간 집계를 연결한다. 원본 계약부터 작업하되 배포 순서와 v1 소비 호환을 확인한다. 실제 DB·HTTP·이벤트 왕복 및 장애·경쟁 행렬을 통과해야 한다. 이 단계는 새 서비스 연동과 상태 전이를 포함하므로 기반 관리와 별도 설계 검토·구현 gate로 수행한다.

### 단계 C — 관리·추가·기록 UI와 전체 통합

관리와 일정 생성 분리, 다섯 입력 폼, 자유 운동·자유 클라이밍 세트·라운드, 기존 기록/수정/상세, 새 로그인·반복 연결, archive와 충돌 초안 보존을 구현한다. 필드 수 제한 없는 추가·정렬·선택 입력과 전송 오류를 실제 화면에서 확인한다.

전체 작업09 완료 E2E는 공부 카테고리 생성→항목 정렬→정의 저장 시 Task 0건→`+ 추가`와 반복 설정→재로그인→0/false와 일부 값만 기록→실제 이벤트 APPLIED→다음 회차 동일 identity→새 version→원래 기록 상세와 correction의 옛 snapshot 유지→archive→새 선택 차단·기존 회차 기록 허용 순서다. 운동·클라이밍도 custom 값과 기존 관계형 세트·라운드의 저장/수정 왕복을 실행한다. GENERAL까지 네 Task 유형이 연결되며 기존 기록 취소·지난 날짜·월 집계·재시도 동작이 유지되는 증거를 합친 뒤 전체09를 완료한다.

각 단계의 구현 완료·검증 완료·전체09 완료를 구별해 장부에 남긴다. 아직 어떤 제품 파일·계약도 이 문서 작성으로 변경되지 않았고 검증을 실행하지 않았다.

## 14. 구현 전 판단할 결정

이 문서의 권장안을 그대로 채택할 수 있는 구체적 결정은 다음 네 가지다. 승인된 상위 기능을 줄이는 선택은 포함하지 않는다.

1. Task/series identity는 고정하고 새 Activity 저장 시 현재 version을 고정한다. 폼 도중 변경은 409와 초안 비교로 처리한다.
2. archive는 신규 선택만 막는다. 서버가 이미 승인한 Task/series 및 그 반복 후속 회차는 마지막 version으로 계속 기록할 수 있다.
3. archive 경쟁을 막기 위한 내부 승인 행의 잔존을 허용한다. 승인 뒤 planner 실패는 같은 command로 복구하며 분산 원자성을 약속하지 않는다.
4. 기존 무검증 Study JSON은 읽기와 보존을 지원하되 임의 값 편집을 새 typed 계약으로 가장하지 않는다. legacy correction은 검증되는 기존 항목만 바꾸고 불명 자유 자료를 서버가 그대로 보존한다.
