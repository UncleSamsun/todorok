# Private program catalog import

실제 푸시업·풀업 표는 공개 저장소·Docker 이미지·테스트 fixture에 넣지 않는다. 공개 검증은 합성 `program-v1` fixture만 사용하며, 실제 원문은 이용 범위가 확인된 뒤 private JSON으로만 주입한다.

## 시작 전 확인

각 입력마다 다음 증거를 private 운영 기록에 보관한다.

- 원문 URL, 작성자 또는 권리자, 확인 일시와 원문 version
- 재배포·변형·서비스 제공 허용 여부를 뒷받침하는 이용 조건 또는 서면 허가
- 원문을 구조화한 사람이 원문과 대조한 기록
- private JSON의 SHA-256 checksum과 `source.kind: PRIVATE_VERIFIED`

PRD에 기록된 두 Naver 원문은 자동 열람이 robots 정책으로 차단돼 있다. 이는 허가 거절의 증거는 아니지만, 2026-09-08 현재 이용 범위를 확인하는 증거도 아니다. 확인 전에는 이 문서의 import 절차를 실행하지 않는다.

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
