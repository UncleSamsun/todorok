# 내장 운동 프리셋과 private catalog import

2026-10-15 사용자 요청으로 푸시업·풀업은 항상 준비된 내장 프로그램으로 제공한다. 원문 게시글·사진·설명문을 서비스에 올리지 않고, 대조한 목표 횟수만 수치 프리셋으로 구성했다. 제품 화면에는 원문 링크나 자료 준비 안내를 노출하지 않는다.

## 내장 프리셋

- 정본 수치: `scripts/generate-builtin-programs.mjs`. 생성 결과는 `contracts/catalog/builtin/presets.json`, `catalogs.json` 및 activity-service의 `resources/catalog/builtin/*.json`이다.
- 푸시업 6주·세 가지 난이도·회차별 최소 목표: [Hundred Pushups 1주](https://hundredpushups.com/week1.html), [2주](https://hundredpushups.com/week2.html), [3주](https://hundredpushups.com/week3.html), [4주](https://hundredpushups.com/week4.html), [5주](https://hundredpushups.com/week5.html), [6주](https://hundredpushups.com/week6.html)의 수치를 2026-10-15 대조했다. 마지막 세트의 `+`는 최소 목표를 뜻하며 프리셋 화면에 유지한다.
- Recon Ron 38주·각 5세트: [전체 수치표](https://www.scribd.com/document/817585441/Recon-Ron-Pullup-Program)와 [별도 설명·표](https://demand-and-supply.tistory.com/11?category=870378)를 대조했다. 1주차 23회, 2주차 이후 주차별 합계 2회 증가, 38주차 98회 및 기존 사용자 지정 프로그램의 첫·마지막 목표와 일치함을 검사한다. 주 3회 편성은 기존 PRD의 3~5회 중 기본값이며, 동일 주차 목표를 세 회차에 적용한다.
- 처음 지정한 Naver 두 원문은 여전히 도구 열람이 차단돼 직접 대조하지 못했다. 위 독립 자료로 동일 이름의 프로그램 수치를 확인한 것이며 Naver 원문 접근 성공으로 기록하지 않는다.
- `SOURCE_BASED` provenance를 사용한다. `BuiltinProgramCatalogBootstrap`은 시작 시 네 개 불변 catalog를 idempotent import한다. 수치를 바꿀 때는 기존 버전을 수정하지 않고 새 catalog version을 만든다.
- 프로그램 박스는 프로그램 전용 주차·회차·세트를 바로 연다. 프로그램은 `program-enrollments`로 편성하며 일반 자유운동 Task 생성으로 대체하지 않는다. 이름·종류 선택은 자유운동에만 제공한다.
- 현재 enrollment 엔진은 시작 시 결정한 난이도 catalog를 고정한다. 공식 푸시업 과정의 중간 최대 횟수 재검사에 따른 자동 난이도 변경·최종 연속 100회 검사는 구현 완료로 주장하지 않는다.

### 주차·회차 선택과 복원

처음 편성할 때 `startWeek`와 `startSession`을 함께 저장한다. 기존 프로그램에서 다른 위치를 편성하면 `POST /program-enrollments/{enrollmentId}/sessions`에 commandId·주차·회차·expectedRevision을 보낸다. 같은 미완료 현재 회차는 기존 일정을 재사용하며 같은 command 재시도도 추가 일정을 만들지 않는다. 마지막 저장 위치와 실제 완료 뒤 다음 위치는 DB에 저장되고 최근 진행 순서로 조회한다.

V12 migration은 기존 이력에 기본 시작 회차1·진행 epoch1을 부여한다. 사용자가 명시적으로 다른 위치로 이동할 때 새 진행 epoch를 만든다. 기존 세션·기록·일정은 삭제하지 않고, 이전 epoch의 늦은 완료는 해당 이력에만 반영해 새 위치를 되돌리지 않는다. 선택한 회차부터 성공/실패에 따른 다음 회차·주차 진행을 계산한다. 이 변경은 운영 배포 전 로컬 검증 단계다.

## 추가 private catalog

내장 프리셋 외 사용자 원본을 별도 주입하는 경우 아래 절차를 사용한다. 합성 fixture는 합성으로 유지하며 실제 프로그램으로 이름만 바꾸지 않는다.

## 시작 전 확인

각 입력마다 다음 증거를 private 운영 기록에 보관한다.

- 원문 URL, 작성자 또는 권리자, 확인 일시와 원문 version
- 재배포·변형·서비스 제공 허용 여부를 뒷받침하는 이용 조건 또는 서면 허가
- 원문을 구조화한 사람이 원문과 대조한 기록
- private JSON의 SHA-256 checksum과 `source.kind: PRIVATE_VERIFIED`

PRD에 기록된 두 Naver 원문은 자동 열람이 robots 정책으로 차단돼 있다. 이는 허가 거절의 증거는 아니지만, 2026-09-08 현재 이용 범위를 확인하는 증거도 아니다. 확인 전에는 이 문서의 import 절차를 실행하지 않는다.

2026-10-12 후속 자동 열람도 두 원문 모두 non-retryable 오류로 실패했다. 이용 조건·작성자 확인이나 허가 확보로 판정하지 않았으며 실제 catalog를 임의로 복사하지 않았다.

## Private input 형식

각 파일은 [`contracts/catalog/program-v1.schema.json`](../../contracts/catalog/program-v1.schema.json)을 만족해야 한다. 실제 입력에는 프로그램명, 원문 링크, 적용 조건, 주의사항, 주차·회차·세트와 checksum을 포함한다. `catalogKey`·`version`이 같은 다른 checksum은 import가 거절되며, 기존 enrollment는 이전 version을 계속 고정 참조한다.

## Compose 주입

private 파일을 컨테이너 내부의 절대 경로에 읽기 전용으로 마운트하는 배포 전용 override를 만든다. override와 원본 JSON은 저장소에 추가하지 않는다.

```yaml
services:
  activity-service:
    volumes:
      - type: bind
        source: /absolute/host/path/private-catalogs
        target: /run/private-catalogs
        read_only: true
```

환경 변수 `TODOROK_PROGRAM_CATALOG_INPUTS`에 쉼표로 구분한 컨테이너 안의 절대 경로를 넣는다.

```text
TODOROK_PROGRAM_CATALOG_INPUTS=/run/private-catalogs/pushup-v1.json,/run/private-catalogs/pullup-v1.json
```

activity-service는 시작 시 빈 값이면 아무 catalog도 import하지 않는다. 값이 있으면 모든 경로가 절대 경로·읽을 수 있는 일반 파일인지 확인하고, 한 파일이라도 틀리면 조용히 건너뛰지 않고 시작을 실패시킨다. 각 파일은 기존 importer의 schema, checksum, 연속 주차·회차와 세트 합계 검증을 통과해야 한다.

## 확인과 되돌림

1. 새 private JSON을 읽기 전용으로 마운트한 상태에서 activity-service를 재기동한다.
2. 로그인한 계정으로 `GET /api/activity/v1/programs`를 호출해 key/version/checksum과 화면 표시 metadata를 확인한다.
3. 같은 파일로 재기동해 `UNCHANGED`가 되는지, 바뀐 checksum이 충돌하는지 private staging DB에서 확인한다.
4. 원본 교체가 필요하면 새 version으로 추가한다. 기존 key/version 파일을 바꾸거나 enrollment·session을 직접 수정하지 않는다.
5. 문제가 있으면 환경 변수와 mount를 제거하고 activity-service를 재기동한다. 이미 import된 catalog는 불변이므로 삭제가 아니라 새 version 또는 별도 운영 판단이 필요하다.

## 완료 후 권장 다음 일정 (2026-10-15)

사용자는 다음 날 고정 생성 대신 프로그램의 권장 간격을 요청했다. `ProgramSchedulePolicy`는 설치된 푸시업 3난이도와 풀업 주 3회 변형에 회차 간 2일·2일·주차 경계 3일 간격을 적용한다. 실제 기록 수행일의 Asia/Seoul 날짜를 기준으로 계산하고, 오래된 기록이면 다음 날짜를 오늘보다 과거로 만들지 않는다. 날짜 단위의 휴식일 배치이며 시각 단위 48시간 예약은 아니다. 기타 기존 catalog는 종전 하루 간격을 유지한다. 불변 v1 catalog의 checksum/목표 수치를 교체하지 않는다.

푸시업 공식 [FAQ](https://hundredpushups.com/questions.html)와 [1주차](https://hundredpushups.com/week1.html)는 주 3회와 회복일을 안내한다. 풀업은 현재 앱에 설정된 38주·주 3회 변형의 cadence를 유지한다. [1981년 Recon Ron 원문 재인쇄](https://sustainableevolution.wordpress.com/wp-content/uploads/2013/06/recon-ron-pullup.pdf)의 매일 수행·2주 step 방식은 다른 표이므로 해당 원문의 빈도와 같다고 주장하지 않는다.

ActivityService에서 완료·정정 시 실제 수행일을 프로그램 서비스에 전달한다. 최종 회차 완료에는 추가 일정이 없다. 동일 outcome/activity 재처리를 건너뛰어 주차 경계 재전송에서 다음 cycle이 중복 생성되지 않게 했다. 프런트 활동 저장 후 owner별 program-enrollments 캐시도 무효화한다. 로컬 합성 API도 완료/다음 날짜/회차/재전송을 구현했고 테스트 데이터는 별도 ignored 디렉터리에서 검증한다. 회사 서버 반영은 하지 않았다.
