# MVP 남은 로컬 구현·검증 계획

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans. 기존 사용자 지시대로 현재 세션에서 구현을 끝까지 수행한다.

**Goal:** 명시적 Task 리마인더를 API·outbox·notification·화면으로 연결하고 배포/복구 검증과 출시 증거를 준비한다.
**Architecture:** planner가 Task 시간·알림 선택을 소유한다. 별도 `task-reminder` outbox 이벤트로 notification의 버전 projection과 예약을 갱신하고 기존 lease worker로 발송한다. 타 서비스 DB 직접 접근·동기 호출은 추가하지 않는다.
**Tech Stack:** 기존 Java 25, Spring Boot, PostgreSQL, Kafka/Debezium, React/TypeScript.
**Spec:** `docs/PRD.md` AC-8/9, `2026-09-07-mvp-functional-implementation.md` 작업13–16.

## Global Constraints

- 서울 기준 명시 시간만 예약. 기본 OFF. 22–07시 만료, 예정+30분 마감.
- 기존 미커밋 작업 보존. 외부 배포·유료 자원 생성은 준비 결과와 구분한다.
- 실제 iPhone·자료 사용 권한·7일 관찰은 로컬 smoke로 대체 판정하지 않는다.

## Review Focus

- 역순/중복 이벤트가 완료·삭제된 Task를 되살리지 않아야 한다.
- 같은 사용자의 같은 시각 서로 다른 Task는 독립 예약이어야 한다.
- 재시도 시점이 22시로 넘어가면 전송하지 않아야 한다.
- 예약 변경이 발송 lease를 무효화하고 성공 구독은 다시 보내지 않아야 한다.
- 요약 예약 검색이 미래 Task 예약 때문에 누락되면 안 된다.
- 기존 버전에서 저장한 생성 command의 fingerprint가 선택하지 않은 신규 필드 때문에 바뀌면 안 된다.

## 실행

- [x] Task API 시간·reminder 입력, 응답, optimistic version·outbox HTTP 회귀 RED→GREEN (30개+직렬화 호환성1개).
- [x] notification 버전 projection·예약 교체/취소·quiet hours·deadline 실제 PostgreSQL 회귀 RED→GREEN (전체33개). 재활성화 동시 경합·요약 왕복 포함.
- [x] 생성/수정 화면 연결 및 저장 실패 초안/재시도 테스트 (영향31개).
- [x] 실제 Kafka 전달·notification 중단 중 일정 저장·복구 검증.
- [ ] PWA 실기기 배포 전환·실제 운영 환경의 브라우저/로그인·복구 검증. 로컬 UI132개·preload guard·실제 asset404·배포 실패 보호/백업 fixture 리허설은 완료.
- [x] 영향 suite, 계약 drift, production build·예산, Compose·실제 Nginx, 최종 증거와 vault 갱신.

## 실행 판정

- Ruling: 반복 일정은 시간/알림을 묵시적으로 상속하지 않는다. 각 Task에서 지정한다 — AC-9의 명시 선택을 보존한다.
- Ruling: 계정 재활성화 시 미래 명시 예약만 복구하고 이미 지난 OFF 알림은 소급하지 않는다 — 예상치 못한 과거 알림 몰아 보내기를 피한다.
- Ruling: 기존 사용자 미커밋 변경과 지정 workspace를 그대로 보존한다. 별도 worktree 이동·원격 push/merge/운영 배포는 수행하지 않는다.
- 실제 Nginx 3개, 암호화/변조 검사1개·격리 PostgreSQL 복원1개·실패 migration의 runtime 보존1개 통과.
- 최종 리뷰의 재활성화/조기 만료·Task 변경 잠금 경합, 요약 왕복과 이전 command fingerprint 호환성을 보완·검증했다. 마지막 독립 재검토에서 중요한 추가 결함은 발견되지 않았다.
- Kafka/Connect 통합 검증은 새 PC의 Connect 이미지 다운로드 대기를 중단한 뒤 이미지를 준비했다. 캐시된 이미지로 다시 실행한다. 이 중단을 기능 통과/실패로 판정하지 않는다.
- 캐시된 이미지의 실제 Kafka 왕복 검증은 통과했다. 최종 웹132개는 Windows worker 시작 시간 제한을 피하기 위해 정상 threads pool에서 23+109개로 나눠 모두 확인했다. VM pool 실험의 전역 mock/realm 실패는 성공 근거에서 제외한다.
- 실제 배포 대상은 회사 서버라는 사용자 후속 확인을 기록했다. 현 기기 SSH 키·agent 부재로 원격 접속은 실행하지 않았고, 배포 플랫폼 확인·키 복원과 실기기/운영 증거는 후속 조건이다.
