# MVP 기능 구현 인계

기준 브랜치: `feature/mvp-functional`  
기준 worktree: `C:\workspace\todorok-worktrees\functional`

## 구현 상태

- 작업 01~10: 로그인·달력·Task·반복·메모·Activity·수정 이력·공부/자유 운동/클라이밍 기록과 합성 운동 프로그램 엔진까지 구현·검증했다.
- 작업 11: private catalog input bootstrap, 출처 metadata UI, runbook까지 준비했다. 실제 원문 import는 이용 범위 증거가 없어 보류한다.
- 작업 12: timestamp 기반 크림프 타이머와 `COMPLETED`/`PARTIAL` Activity 저장, Wake Lock·음성·진동 adapter를 구현했다. 실제 iPhone 검증이 남아 있다.
- 설정/PWA: 계정 theme, 기본 OFF/08:00 요약 설정, 설치 안내, 이전 lazy chunk 복구와 번들 예산 검증을 구현했다.
- 알림: preference는 planner outbox→Debezium→Kafka→notification inbox로 전달된다. 활성화 시 다음 Asia/Seoul 요약을 예약하며, 요약 시간 변경은 이전 `PENDING`을 취소해 새 예약 한 건만 남긴다.

## 마지막 검증 증거

- `NotificationScheduleServiceTest`와 `NotificationPersistenceIntegrationTest`가 통과했다.
- local Compose runtime `todorok-task09b-smoke`에서 09:35 preference event를 전달한 뒤 해당 사용자의 `notification_delivery`는 `CANCELED|2`, `PENDING|1`이었다.
- 마지막 기능 수정 커밋: `e234cfe fix(notification): 요약 시간 변경의 중복 예약을 취소`.

## 다음 작업

1. VAPID subscription 등록과 delivery worker를 구현한다. 공개 API·구독 보호 저장·발송 claim/만료/재시도·실제 설정 UI 흐름을 한 묶음으로 검증한다.
2. 이용 범위가 확인된 운동 catalog 입력을 private mount로 import하고 checksum/version을 검증한다.
3. 타이머의 실제 브라우저·iPhone 흐름을 확인한다.
4. 배포 준비·실기기·최종 통합 검증을 진행한다.

## 작업 규칙

- 이미 통과한 기반·계약·이전 리뷰는 이유 없이 반복하지 않는다. 다음 사용자 흐름에 필요한 영향 테스트와 실제 핵심 흐름만 실행한다.
- 외부 운동 원문은 이용 범위 증거 전까지 repository에 복사하거나 public fixture로 추가하지 않는다.
- `.superpowers/`는 사용자 소유 untracked 파일이다. add, delete, commit하지 않는다.
- `develop`의 별도 문서 변경과 원격 divergence를 이 기능 브랜치에 섞지 않는다.
