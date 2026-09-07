# 작업06 날짜별 메모와 할 일 메모

## 구현

- V6 `daily_note`는 `(user_id,date)` 기본키, 20,000자 DB 제약, content와 version을 저장한다. 기존 V1~V5는 수정하지 않았다. Task/series에도 길이 제약을 추가했다.
- JWT principal 소유자로 GET/PATCH `/notes/{date}`를 처리한다. 없는 메모는 `{date,content:"",version:null}`, 빈 내용으로 저장된 메모는 숫자 version으로 구분한다.
- 최초 저장은 `INSERT ... ON CONFLICT DO NOTHING RETURNING`, 이후 저장은 `UPDATE ... WHERE version=? RETURNING`이다. 동시 최초 INSERT를 트랜잭션 오류 없이 409로 바꾸고 응답은 해당 쓰기가 실제 저장한 버전이다.
- OpenAPI 원본과 Java/TypeScript 생성물을 동기화했다. 7.24.0 Spring 생성기의 required nullable 속성에 무조건 `@NotNull`을 붙이는 템플릿 문제를 planner 전용 `beanValidation.mustache`의 `isNullable` 조건으로 수정했다. 다른 필드의 validation은 유지된다. 생성 템플릿의 개행 때문에 기존 생성 DTO에 빈 줄 변화도 포함된다.
- Task 생성/수정의 note 계약을 추가했다. Task와 series 수정에서 note 생략은 현재 값을 유지하고 빈 문자열은 명시적 비움이다. 현재 회차는 할 일 수정에서, 다음 회차는 기존 반복 설정의 메모에서 수정한다. DailyNote와 별도 저장이다.
- 목록 아래 한 줄 미리보기를 누르면 같은 화면에서 textarea가 열린다. 입력 revision·전송 revision·처리된 응답 revision·서버 version을 구분하고 500ms debounce로 저장한다. 응답 순서가 바뀌어도 최신 buffer나 cache version을 뒤로 돌리지 않는다.
- 날짜별 초안은 화면 메모리에 유지한다. 날짜를 바꾼 후 과거 응답은 해당 날짜 cache만 갱신한다. 계정 generation 변경 시 component를 재생성한다. 저장 시작·응답 처리는 현재 session generation도 직접 확인하므로 React unmount 전에도 이전 계정의 debounce 요청과 cache 갱신을 막는다.
- 409/network 오류에서 초안과 실패 표시를 유지한다. 수동 재시도에서 GET으로 최신 version만 읽고 현재 초안을 PATCH한다. 서버 내용을 자동 병합하지 않는다. clean 초안만 새 Query 데이터에 맞추고 dirty/saving/error 입력과 expected version은 보존한다.
- 공유 validation은 길이 검사뿐이다. 직렬 queue, localStorage 초안, 오프라인 영구 저장을 추가하지 않았다.

## RED / GREEN

- 실제 PostgreSQL HTTP RED: `dailyNotesAreVersionedIsolatedAndValidateLimits`, `concurrentFirstNoteWriteHasOneWinner`가 미구현 endpoint로 2/2 실패했다.
- 첫 구현도 최초 null 버전을 `@NotNull`이 거부해 400이었다. 생성 템플릿을 고친 뒤 같은 HTTP 2/2가 통과했다.
- `StickyNote.test.tsx` RED는 미구현 component import 실패였다. 최종 fake timer 테스트 12개가 통과했다. 499ms 미전송/500ms 전송, 연속 입력, 응답 역전, 이전 성공 후 최신 dirty 상태, 날짜 전환, 409/network 재시도, logout 늦은 응답, 이전 실패 무시, 재조회 실패, clean refetch/dirty 보존, unmount 전 session 변경 직후 요청·응답 차단을 검증한다.
- 전체 `TaskHttpIntegrationTest` 17/17 통과 (2026-09-07 17:48 KST): 반복/이월 회귀와 메모 테스트 포함. 메모 검증은 미작성, 최초/빈 내용, 20,000/20,001자, 다른 사용자, stale/null version, 잘못된 날짜, null content, 8개 동시 최초 쓰기 중 정확히 한 성공·나머지 409, Task/series 생략 보존·명시 비움·다음 회차 유지·DailyNote 분리를 포함한다.
- 웹 `pnpm --dir apps/web test --maxWorkers=2`: 9 files, 39/39 통과. 이후 저장 시작 generation guard와 테스트 하나를 추가한 마지막 변경은 `StickyNote.test.tsx` 12/12 및 production build로 검증했다. 부하 중 기존 recurrence 첫 lazy route 대기 1초가 초과되는 문제가 있어 그 초기 대기만 10초로 늘렸다. 기능 assertion은 그대로다.
- `pnpm build:web`: TypeScript와 production/PWA build 통과.
- `pnpm test:contracts`: 5/5 통과. `pnpm contracts:check`: 원본과 생성 계약 일치.

## 실제 브라우저

- 새 `bootJar`, `migrationBootJar`, `bootstrapBootJar`로 `todorok/planner-service:task06` 이미지를 생성했다. 이미지 digest `sha256:082d7dfac41e4680124a35cbdffecbbb07b872f9eb8f9f5ad8add53e770562f2`.
- `.local/task06/browser-smoke.mjs`는 별도 Compose 프로젝트 `todorok-task06-browser`, PostgreSQL 17, V6 migration, 임시 인증키/계정, nginx 5186, 실제 Chromium을 사용한다.
- 첫 실행에서 로그인, 날짜별 최초 저장, SQL 내용 확인, 다른 날짜/복귀, 새로고침, offline 초안/재시도, 직접 DB version을 올린 실제 409/초안/재시도, 현재 Task 메모 저장을 확인했다. 반복 설정 안내 문구를 클릭한 locator 오류로 후반부가 중단되어 summary를 지정해 재실행했다.
- 두 번째·세 번째 실행에서도 위 DailyNote 흐름은 통과했고 반복 메모 수정까지 저장됐다. Task 완료 후 달력 pointer click이 날짜를 변경하지 못해 후반부가 중단됐다. 이 전체 스크립트의 terminal PASS는 주장하지 않는다. 짧은 `.local/task06/series-browser-smoke.mjs`의 첫 후속 실행은 DOM button click으로 다음 회차 메모·별도 날짜 메모·4개 화면을 확인하고 `REAL_BROWSER_TASK06_SERIES_PASS`로 종료했다.
- 실제 pointer를 별도 확인했다. metadata 출력의 브라우저 변수 오타를 고친 최종 실행에서 `scrollIntoView(center)` 후 hit-test가 원하는 date button을 가리켰다 (`inside=true`, `disabled=false`, x=413.03125, y=181, width=61.03125, height=54, scrollY=0). 이후 실제 `click`과 선택 날짜 `2026-09-08` assertion이 통과했다. `before-calendar-pointer.png`에 클릭 전 화면을 남겼다. 앞선 자동 클릭 실패의 당시 hit target은 기록되지 않았으므로 원인을 단정하지 않지만, 명시적으로 위치를 정렬한 실제 pointer 동작과 다음 회차 메모 유지는 확인했다.
- `.local/task06/390-light.png`, `390-dark.png`, `1440-light.png`, `1440-dark.png`를 생성했다. 모바일 밝은 화면과 데스크톱 어두운 화면을 직접 열어 확인했으며, 각 viewport의 가로 overflow assertion도 통과했다. 모바일은 세로 스크롤로 메모 편집 영역을 이용한다.
- 최종 pointer 실행도 `REAL_BROWSER_TASK06_SERIES_PASS`와 exit 0으로 종료했고 브라우저·테스트 Compose/volume을 정리했다. 최종 source에는 session generation guard가 포함됐으며 이 빌드에서 후속 브라우저 검증을 수행했다. DailyNote 오류·재접속 전체 흐름은 앞선 빌드에서, 마지막 generation guard는 focused 테스트 12/12에서 검증했다.

## 다음 작업 경계

완료 기록 메모는 작업07 Activity Record에 연결할 범위다. 이번 작업은 Activity 구현을 변경하지 않았다. 미저장 초안은 화면 수명 동안만 유지되므로 브라우저 종료·새로고침 전 저장 실패 초안의 복구는 제공하지 않는다. 이는 요청한 queue/오프라인 저장 제외 범위다.
