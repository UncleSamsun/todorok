# 작업09A 템플릿 기반 중요 리뷰

범위: `d2422ba..314d0d8`. 판정: 수정 필요. Critical/Minor 없음, Important 1건. 단계 B/C는 분리된 후속 범위로 판정했다.

후속 판정: `9fe9fe6`의 실제 HTTP·주입 mapper·DB 불변 회귀를 재리뷰해 기존 Important를 오탐으로 철회했다. 런타임은 누락 fields를 null로 역직렬화하고 Bean Validation의 fields/NOT_NULL로 거절한다. 명시적 빈 배열은 성공한다. 제품 수정은 없으며 최종15/0/0/0·assemble 증거와 함께 09A 요구·품질을 승인했다. 아래는 최초 지적의 이력이다.

## Important — 필수 fields 누락과 명시적 빈 배열 혼동

- 생성 `CreateTemplateVersionRequest.java:34`, `CreateTemplateRequest.java:39`가 fields를 빈 ArrayList로 초기화한다.
- `TemplateService.java:238`은 null만 거부하므로 JSON에서 fields를 생략해도 빈 정의로 처리된다.
- 기존 필드가 있는 템플릿의 새 버전 요청에서 fields가 누락되면 현재 버전이 빈 정의로 전진할 수 있다. required 계약을 위반하며 명시적인 항목 삭제와 구분되지 않는다.

## 수정 라운드 1

생성물을 수동 편집하지 않는다. 템플릿 입력 경계나 정식 생성 설정으로 해결하며 기존 Activity 경로를 바꾸지 않는다. `fields: []`는 허용하는 요구를 유지한다.

실제 HTTP 회귀에 create와 새 version 각각 `fields` 누락·null → 400, 명시적 빈 배열 → 성공을 포함한다. 거절 뒤 template/version/field/management command가 변하지 않는지 확인한다. 기존 필드가 있는 템플릿을 fixture로 써야 빈 정의로 전진하는 결함을 검출할 수 있다.

집중 회귀의 RED→GREEN과 관련 build를 확인하고, 생성 설정을 변경한다면 재생성·drift·계약 왕복도 검사한다. `task-09-template-foundation-report.md`에 보완 범위·명령·테스트 결과를 추가한다. 전체28개를 변경과 무관하게 반복하지 않는다. 부모 ledger/vault 편집과 push/merge는 하지 않는다.

## 확인된 강점

owner·command 잠금/최초 응답 replay, template revision 잠금, 불변 정의·FK, 단일 SQL 응답 snapshot, keyset 소유권, 경로 한정 엄격 입력, 실제 DB·HTTP·프록시 검증은 요구와 부합한다. 위 입력 경계가 이번 리뷰의 유일한 중요 차단 사유다.
