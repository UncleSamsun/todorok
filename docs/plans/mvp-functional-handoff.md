# MVP 기능 구현 인계

기준 브랜치: `feature/mvp-functional`  
현재 workspace: `C:\workspace\todorok`
2026-10-12 재개·이동 인계: [구현 및 workspace 인계](2026-10-12-workspace-handoff.md). 아래 과거 검증 근거는 당시 기능 worktree에서 실행했다.

## 2026-10-14 회사 서버 배포

https://211.45.127.56/todorok/ 에 todorok-test K3s 격리 배포를 완료했다. SSH mjkim, 이미지5개 Actions 성공·Public pull, migration12/11/6, runtime5개+StatefulSet2개 Ready, Debezium RUNNING과 실제 HTTPS smoke11개를 확인했다. 초기 계정은 ACL 제한 ignored .local/company-server/private/access.json에만 보관한다. 초기 실DB와 앱 키 암호화·외부 PC 보관, network-none DB 복원에서 계정·owner·ACL을 검증했다.

[2026-10-14 회사 서버 배포 결과](2026-10-14-company-server-deploy.md). 원래 feature/mvp-functional 수정은 보존하고 별도 관리 worktree의 codex/company-server-deploy에서 스냅샷을 게시했다. 실제 iPhone/provider·catalog 이용 범위·자동 백업/복구한 앱 로그인/CDC·장기 운영 검증은 남는다.

## 구현 상태

- 작업 01~10: 로그인·달력·Task·반복·메모·Activity·수정 이력·공부/자유 운동/클라이밍 기록과 합성 운동 프로그램 엔진까지 구현·검증했다.
- 작업 11: private catalog input bootstrap, 출처 metadata UI, runbook까지 준비했다. 실제 원문 import는 이용 범위 증거가 없어 보류한다.
- 작업 12: timestamp 기반 크림프 타이머와 `COMPLETED`/`PARTIAL` Activity 저장, Wake Lock·음성·진동 adapter를 구현했다. 실제 iPhone 검증이 남아 있다.
- 설정/PWA: 계정 theme, 기본 OFF/08:00 요약 설정, 설치 안내, 이전 lazy chunk 복구와 번들 예산 검증을 구현했다. 2026-10-12 후속 재개에서 복구 guard를 빌드별로 분리하고 재시도 실패의 오류 전파·storage 차단 처리를 보완했다. 관련 테스트 8개 통과, 실제 설치 PWA 배포 전환은 검증 대기다.
- 알림: preference는 planner outbox→Debezium→Kafka→notification inbox로 전달된다. 활성화 시 다음 Asia/Seoul 요약을 예약하며, 요약 시간 변경은 이전 `PENDING`을 취소해 새 예약 한 건만 남긴다.

## 마지막 검증 증거

- `NotificationScheduleServiceTest`와 `NotificationPersistenceIntegrationTest`가 통과했다.
- local Compose runtime `todorok-task09b-smoke`에서 09:35 preference event를 전달한 뒤 해당 사용자의 `notification_delivery`는 `CANCELED|2`, `PENDING|1`이었다.
- 마지막 기능 수정 커밋: `e234cfe fix(notification): 요약 시간 변경의 중복 예약을 취소`.

## 다음 작업

1. VAPID subscription API·일일 요약·Task 개별 리마인더의 실제 provider·iPhone 수신을 검증한다. Task 시간·선택, quiet hours·마감, 재활성화·동시 경합과 기존 생성 command 호환성, 실제 Kafka·웹132개·build/drift 검증을 마쳤다. 자동 검증과 실전 수신 검증은 구분한다.
2. 이용 범위가 확인된 운동 catalog 입력을 private mount로 import하고 checksum/version을 검증한다.
3. 타이머의 실제 브라우저·iPhone 흐름을 확인한다.
4. 배포 준비·실기기·최종 통합 검증을 진행한다.

실제 배포 대상은 회사 서버이며 2026-10-14 새 SSH 키 등록 후 K3s 격리 배포를 완료했다. 기존 단일 VM Compose 준비와 실제 서버 배포 방식을 구분한다. 운영 준비 도구와 격리 백업/실패 migration 검증은 `docs/runbooks/deployment.md`, `backup-restore.md`, `release-checklist.md`를 참고한다.

## 작업 규칙

- 이미 통과한 기반·계약·이전 리뷰는 이유 없이 반복하지 않는다. 다음 사용자 흐름에 필요한 영향 테스트와 실제 핵심 흐름만 실행한다.
- 외부 운동 원문은 이용 범위 증거 전까지 repository에 복사하거나 public fixture로 추가하지 않는다.
- `.superpowers/`는 사용자 소유 untracked 파일이다. add, delete, commit하지 않는다.
- `develop`의 별도 문서 변경과 원격 divergence를 이 기능 브랜치에 섞지 않는다.
