# 작업08 UI 중요 리뷰 수정 라운드 1

필수 입력: `task-08-summary-review.md`의 Important 1–7 및 Minor 44px, 원 요구 `task-08-summary-ui-brief.md`, 구현 보고 `task-08-summary-ui-report.md`. 기준 UI HEAD `3e8fe6d`이며 이후 DB 테스트 보강 커밋은 보존한다.

## 작업 원칙

- 7건을 하나의 보완 작업으로 처리하되 각 원인을 RED 회귀로 확인한 뒤 수정한다. 예전 정상 저장만 반복해 새 경계의 증거로 삼지 않는다.
- 예상 버전·원본 날짜/시간·초안을 편집 세션의 같은 snapshot으로 유지한다. 조회 데이터 갱신으로 초안을 자동 rebase하지 않는다. 성공/명시적 재열기에서만 새 기준으로 전환한다.
- 확정 400과 409, 네트워크/5xx 불확실을 구별한다. 불확실한 작업의 초안과 원 요청을 함부로 버리지 않으며, 사용자에게 현재 결과와 충돌을 명시한다. 초안을 다른 revision 결과로 오인해 지우면 안 된다.
- PENDING→APPLIED/CONFLICT 관찰은 기록 생성에서 검증된 session/generation/revision 규칙을 재사용한다. 복사한 effect가 별도 lifecycle 버그를 만들지 않도록 한다.
- 완료 Task의 현재 COMPLETED Activity를 찾아 기존 상세로 보낸다. 목록 첫 페이지나 오늘 수행일에 있을 것이라 가정하지 않는다. 필요한 경우 기존 list API에 owner-scoped taskId 필터를 계약부터 추가하는 작은 확장을 검토한다. 임의 UUID, localStorage 대응표, 최신 첫 행 추정으로 연결하지 않는다. 계약 확장이 필요한 경우 부모에게 해당 인터페이스와 서버 재빌드 필요를 알린다.
- 원본 실제 시간의 초·소수초는 메모만 수정할 때 그대로 보존한다. 시간 편집 여부를 명확히 추적하고, 부분 입력이 완전한 기존 구간을 소리 없이 지우지 않게 한다.
- planner 등록 집계는 생성·수정·삭제·반복 등 기존 mutation 경로와 함께 무효화되도록 키 책임을 정리한다. Activity에서 planner DB를 읽는 방식은 금지한다.

## 검증·인계

리뷰 문서의 보완 검증 행렬에 이름이 있는 회귀 테스트를 대응시킨다. focused 실행 후 최종 웹 suite 한 번과 build/drift(계약 변경 시)를 실행한다. runtime `http://localhost:5188`은 부모 소유이며 host `apps/web/dist`를 nginx에 mount한다. frontend build 후 reload로 실제 UI검증하되 구버전chunk문제와 제품동작문제를 구분한다. 서버 변경 시 부모와 빌드/재기동을 조율한다.

기존 `.local/task08-runtime/browser-ui.mjs`와 `mobile-check.mjs`가 허용된 native browser 패턴이다. 비밀값은 내부에서만 읽고 출력하지 않는다. 기록 수정 보고서에 실제 명령과 결과/새캡처를 추가한다. 최종 검증 후 본인파일만 한글 scoped commit. 하위에이전트/push/merge 금지. 부모 ledger/vault는 편집하지 않는다.

UI의 다른 기록 생명주기 결함이 같은 경로에서 드러나면 재현과 범위를 보고하고 해결한다. 이 작업을 계기로 작업09–16의 기능이나 PWA 설계 전체로 확장하지 않는다. 최종 재리뷰는 이전 Important 항목과 보완 diff의 새 결함에 한정한다.
