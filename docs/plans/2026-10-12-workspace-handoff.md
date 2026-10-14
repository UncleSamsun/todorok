# 2026-10-12 구현 및 workspace 인계

작업 경로는 `C:\workspace\todorok`이다. 이전 경로는 `C:\Users\minjoun\Documents\Codex\2026-10-12\new-chat\todorok`이며 사용자 요청에 따라 현재 실행 중인 검증을 끝낸 뒤 전체 저장소를 이동한다. 브랜치는 `feature/mvp-functional`, 기준 커밋은 `32e8d72`다. 이번 수정은 커밋하지 않은 상태로 보존한다.

## 이번 구현

- Java 25를 `C:\Users\minjoun\Documents\Codex\tools\java25\jdk-25.0.4.1+1`에 준비하고 사용자 JAVA_HOME을 설정했다. 이미 열린 셸은 `$env:JAVA_HOME=[Environment]::GetEnvironmentVariable('JAVA_HOME','User'); $env:Path="$env:JAVA_HOME\bin;$env:Path"`를 적용한다.
- Nginx 경로 검사 기대를 실제 설정과 맞추고, 웹 인증 fixture·날짜 고정·cold import 대기 시간을 수정했다.
- notification OpenAPI, 기기 구독 HTTP API, AES-GCM 암호화 저장, 소유권·provider 검증·구독 수 제한·폐기를 구현했다.
- 일일 요약 발송 worker에 DB lease, 구독별 결과, 실패 재시도, 마감 만료, 다음 예약을 추가했다. 공개 provider 호출은 시간 제한과 redirect 금지를 적용한다.
- 설정의 사용자 클릭 권한 요청과 기기 연결·해제, PWA push 표시와 안전한 오늘 화면 이동을 연결했다.

## 로컬 push 설정

기본 설정은 push 비활성이다. 키 생성은 `scripts/New-LocalPushSecrets.ps1`을 명시적으로 실행하며 기존 키를 덮어쓰지 않는다. 생성 위치는 Git에서 제외된 `.local/push`이고 암호화 키는 별도 안전한 백업이 필요하다. 이번 검증에서는 운영 비밀키를 생성하거나 공개 provider로 실제 발송하지 않았다.

Compose 실행 시 기존 환경 파일을 사용하고 `infra/docker/compose.yml`에 `infra/docker/compose.push.yml`을 추가한다. `PUSH_SUBJECT`에 실제 연락처 mailto 또는 HTTPS URL을 설정한다. 각 secret 파일 위치는 override의 `PUSH_*_KEY_FILE`로 지정할 수 있다. 이 옵션을 켜기 전에 notification DB migration을 적용한다.

## 남은 검증과 기능

- 실제 iPhone의 홈 화면 PWA에서 권한·푸시 수신·타이머·Wake Lock 검증.
- Task 개별 리마인더(마감+30분, 조용한 시간)의 별도 구현. 이번 worker 검증은 일일 요약에 해당한다.
- 외부 운동 자료 이용 권한 확인 및 실제 catalog import.
- 열린 PWA 업데이트 중 옛 lazy chunk 실패 복구, 전체 서비스 통합 및 배포 검증.

검증 결과와 폴더 이동 전후 보존 비교는 Obsidian `50_SessionLogs/2026-10-12-todorok-java25-notification-workspace-move.md`에 기록한다. 실행 로그와 파일 해시 manifest는 이전 채팅 폴더의 `work/`에 유지하며 비밀값 본문은 기록하지 않는다.
