# 작업09 연결 경계 사전 확인

기준: 작업08 UI `53c031a`. 작업09 구현 착수·완료 보고가 아니라 기존 코드와 다음 요구사항의 차이를 기록한다.

## 확인된 현재 상태

- PRD §12.1–12.2와 AC-4는 사용자 정의 다섯 입력 형식, 제한 없는 항목 추가·정렬, 선택 입력, 관계형 정의와 과거 JSONB snapshot 보존을 요구한다.
- `contracts/openapi/activity-v1.yaml`의 `StudyDetail.values`와 `snapshot`은 현재 자유 object다. `ActivityDetailStore.save`는 전달받은 값을 그대로 직렬화한다. 작업09에서 소유자·template version·field type 검증과 서버 생성 snapshot으로 연결해야 한다. 클라이언트가 임의로 보낸 snapshot을 신뢰하는 상태를 최종 기능으로 유지하면 안 된다.
- `StudyFields.tsx`는 과목·집중 시간만 표시한다. 템플릿 편집기뿐 아니라 최초 기록 폼, 수정 폼, 과거 상세 표시를 함께 연결해야 한다.
- `QuickAdd.tsx`에는 템플릿/카테고리 선택이 없다. 일정 생성→기록에서 선택한 카테고리가 유지되는 연결이 필요하다. planner가 activity DB를 직접 읽는 방식은 사용하지 않는다.
- 운동 세트와 클라이밍 라운드는 이미 관계형 테이블에 저장한다. 작업09의 확장 때문에 기존 세트/라운드와 기록 수정 이력을 자유 JSON 하나로 대체하지 않는다.
- DESIGN §9의 추가 흐름은 관리와 일정 생성을 분리한다. 카테고리/템플릿 저장은 Task를 만들지 않고, `+ 추가`에서 기존 정의를 선택하고 날짜·반복을 지정한다. 공부는 정의된 항목·단위를 미리 보여주고 오늘 목록에도 카테고리·기록 기준을 표시한다. 운동도 별도 템플릿에서 항목·단위를 추가/삭제/정렬한다.
- 현재 TaskScheduled v1 payload는 `additionalProperties:false`이며 taskId/taskType/scheduledDate/status만 담는다. 일정에 템플릿 연결을 추가한다면 planner 계약·Task 저장·반복 복제·event 원본/생성물·activity 참조 projection을 함께 검토해야 한다. 프런트만 대응표를 갖는 방식은 새 로그인·다른 기기·반복 다음 회차에서 연결을 잃는다.

## 실행 전 확정할 계약 경계

1. 템플릿 identity와 immutable version을 분리한다. 수정 경쟁과 archive 뒤 신규 선택을 어떻게 거부하는지 HTTP/DB 테스트에 포함한다.
2. 일정에 연결되는 template identity와 기록 당시 고정되는 version/snapshot의 책임을 분리한다. 새 버전 생성이 과거 기록의 표시·수정 기준을 바꾸지 않아야 한다.
3. 모든 사용자 정의 값은 선택 입력이다. 미입력 키 없음, 숫자 0, 체크 false를 각각 보존한다. 낯선 field ID·중복 ID·타입 불일치·음수 시간은 구체적인 오류로 거부한다.
4. 숫자·시간·짧은 텍스트·체크·긴 메모의 형식과 단위를 원본 OpenAPI에서 정하고 생성물을 재생성한다. 항목 개수 제한과 전송 크기 제한을 혼동하지 않는다.
5. 기존 템플릿 없는 기록과 기존 Study 자유 object의 이전 정책을 명시한다. 기존 사용자 데이터를 삭제하거나 임의로 새 정의에 맞춰 바꾸지 않는다.
6. 과거 snapshot의 단일 SQL 조회 일관성, revision 이력, command 멱등 재전달, 월 시간 집계와의 연결을 보존한다. 자유 시간 필드를 무조건 합산하면 실제 수행 시간을 중복 계산할 수 있으므로 집계용 실제 시간과 구분한다.

## 검증 흐름

카테고리 생성·필드 정렬 → 일정 추가 → 실제 입력만 기록 → 템플릿 새 버전 → 과거 기록의 원래 이름·형식·단위 유지 → 원본 version에 따른 기록 수정 → archive 후 신규 선택 차단.

서버 계약/저장 경계를 먼저 검증하고, 그 인터페이스가 확정되면 세 도메인 UI와 위 실제 브라우저 흐름을 연결한다. 작업08의 열린 검증이 끝나기 전 작업09 구현을 병렬 착수하지 않는다.

## 09B 실행 경로 확인

현재 `services/activity-service/src/main/resources/application.yml`의 servlet context-path는 `/api/activity/v1`이며 기존 controller는 상대 경로를 구현한다. 상세설계의 내부 경로 `/internal/activity/v1/template-selections`를 그대로 controller에 붙이면 예상한 URL이 되지 않는다.

09B에서는 공개 API 경로를 바꾸지 않고 내부 경로를 같은 context의 `/internal/template-selections`로 두는 안을 우선 검토한다. 실제 URL은 `/api/activity/v1/internal/template-selections`다. Nginx의 일반 Activity proxy보다 먼저 이 내부 경로를 외부에서 차단하고, 앱에서도 별도 서비스 인증 chain으로 검증해야 한다. 사용자 JWT와 서비스 JWT의 decoder·audience·scope·principal 처리를 혼용하지 않는다. 최종 계약·설계 문구와 실제 HTTP/Nginx 경로 검증을 함께 갱신한 뒤 구현한다.
