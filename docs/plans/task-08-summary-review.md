# 작업08 월 API·기록 UI 중요 리뷰

검토 범위 `a3a58ff..3e8fe6d`. 읽기 전용 독립 검토. 판정: 요구 미충족 / 수정 필요. Critical 없음. 서버 월 SQL은 owner/type/서울 반개구간·상태별 시간·header 우선·다중행 중복방지와 정합하다.

## Important — 수정 라운드 1 대상

1. `ActivityRecordPage.tsx:27,42`: 초기 폼은 v4인데 background GET v5 이후 저장이 최신 record.data.version을 읽어 v4 초안을 v5로 덮어쓴다. 폼 초기화 시 expectedVersion을 같은 snapshot으로 고정해야 한다.
2. `ActivityRecordPage.tsx:24,34–36,73–74`: 수정/취소 PENDING 폴링 및 일반 수정 화면 SyncStatus가 없고 APPLIED/CONFLICT를 관찰하지 못한다. VOIDED 수동 조회 후에도 APPLIED revision의 관련 캐시 갱신이 없다. Today calendar/range도 누락됐다. revision별 상태·수동 확인·APPLIED 한 번 처리와 이전/새 날짜·월 갱신을 연결해야 한다.
3. `DomainPage.tsx:33`, `TodayPage.tsx:149`: COMPLETED Task도 taskId 신규 POST 폼으로 보낸다. 완료 Task는 기존 기록 상세로 연결해야 한다.
4. `ActivityRecordPage.tsx:48`: 확정400 실패 후 입력을 바로잡아도 이전 snapshot으로 재전송한다. 확정 실패와 불확실 응답을 구분하고 확정 실패 뒤 새 입력을 요청으로 구성해야 한다.
5. `ActivityRecordPage.tsx:56`: 불확실 응답 sameResult가 시간·status를 비교하지 않고 version>expectedVersion만 확인한다. 다른 writer의 시간만 다른 revision을 성공으로 오인해 초안을 버릴 수 있다. 정확한 요청 결과만 인정해야 한다.
6. `DomainPage.tsx:21`, `TodayPage.tsx:137`: planner CRUD의 calendar 무효화가 별도 calendar-summary에 닿지 않아 이미 방문한 도메인 등록 수가 낡은 값에 머문다. 생성·삭제·수정 후 등록 집계를 갱신해야 한다.
7. `ActivityRecordPage.tsx:14`: 시·분만 복원해 메모 수정 시 기존 실제 시간의 초 정밀도를 잃는다. 10:00:59→10:01:01의2초가10:00→10:01의60초가 된다. 시간을 수정하지 않았으면 원본 timestamp를 유지해야 한다.

## Minor

- `styles.css:508`: 요약 재시도 버튼 min-height32px는 모바일44px 요구보다 작다. 이번 UI 보완에서 범위 내 단순 수정으로 함께 처리한다.

## 증거 범위

- DB 윤년/서울 경계/read-only 행렬은 별도 검증 담당이 보강 중이다.
- PWA old-chunk fallback은 작업14 필수 항목으로 추적하며 이번 수정 라운드에서 SW 설계를 확대하지 않는다.

## 라운드 1 결과

`e88b885` 보완을 독립 재리뷰해 중요 항목 1–7과 44px 터치 영역을 모두 해결한 것으로 판정했다. 새 Critical/Important 문제는 없었다. 원본 편집 기준 고정, 공통 APPLIED 처리와 낮은 revision 보호, 완료 기록 cursor 탐색, 확정·불확실 요청 분리, 정확한 timestamp/revision 비교, planner 등록 캐시 갱신, 원본 시간 보존을 각각 확인했다. 이 판정은 UI 범위이며 별도 DB 테스트 검증 2건은 포함하지 않는다.

## 보완 검증 요구

- v4 폼→background v5→저장 expectedVersion4·409초안 유지.
- PATCH/void PENDING→APPLIED 및 CONFLICT; revision당 갱신1회; 로그아웃/화면전환 뒤늦은응답 무시.
- COMPLETED Task 실제상세 조회와 POST미호출; 해당목록이 페이지분할되어도 정확한task기록을찾기.
- 400후수정입력재전송, 409초안유지, 5xx/네트워크불확실때명시확인; startedAt/endedAt다른revision은성공으로오인하지않기.
- planner Task생성/삭제후이미방문한도메인등록수갱신.
- 메모만수정시기존초/소수초구간유지; 시간직접편집시명시입력반영.
- 실제브라우저에서완료Task열기→수정→상태반영과등록추가→요약갱신을확인하고44px모바일재시도버튼검증.
