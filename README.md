# TODOROK (토도록)

달력 기반 할 일 관리에 운동·공부·클라이밍 수행 기록을 연결하는 iPhone 우선 PWA입니다.

GitHub: https://github.com/UncleSamsun/todorok

현재 단계는 기반 구현과 MVP 기술·디자인 검토 완료입니다. 구현 기준은 [PRD](docs/PRD.md), [디자인 시스템](DESIGN.md), [MVP 구현 계획](docs/plans/2026-09-01-mvp-implementation-plan.md)을 따릅니다.

## 현재 결정

- 제품명: 토도록 / TODOROK
- 클라이언트: iPhone 우선 반응형 PWA, 노트북 웹 지원
- 웹: React 19.2 + TypeScript + Vite 8
- 향후 모바일: Expo 기반 React Native, 도메인·API·검증·디자인 토큰 공유
- 백엔드: Spring 기반 최소 MSA
- 서비스: planner / activity / notification
- 메시징: Kafka KRaft 단일 브로커
- 데이터베이스: PostgreSQL 단일 인스턴스, 서비스별 스키마·계정 분리
- Persistence: Spring Data JPA·Flyway
- 이벤트 전달: Kafka Connect·Debezium outbox와 consumer inbox
- 배포: AWS Lightsail 4GB + Docker Compose + Nginx
- 상태: PRD 1.4.2, 디자인 시스템 1.2.19와 전체 MVP 구현 계획 승인 완료

## 개발 원칙

- 요구사항 변경은 [PRD](docs/PRD.md)를 먼저 수정합니다.
- 기능 하나를 이슈 하나와 Pull Request 하나로 관리합니다.
- `main`·`develop` 직접 커밋을 금지합니다.
- 상세 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 따릅니다.

## API·이벤트 계약

- `contracts/openapi/`가 planner·activity REST API의 원본입니다.
- `contracts/events/`가 서비스 간 이벤트의 원본이고 `contracts/fixtures/`에 정상·오류 예시가 있습니다.
- `pnpm contracts:generate`로 Spring API interface·DTO와 TypeScript Fetch client를 생성합니다.
- `pnpm contracts:check`로 저장소의 생성물이 원본과 일치하는지 검사합니다.
- `services/*/src/generated`와 `packages/api-client/src/generated` 파일은 직접 수정하지 않습니다.

## PostgreSQL migration

- 각 서비스는 자기 `src/main/resources/db/migration`과 PostgreSQL schema만 소유합니다.
- 일반 서비스는 Flyway를 실행하지 않고 Hibernate schema를 검증합니다.
- 같은 서비스 image의 `/app/migration.jar`가 migration을 먼저 완료한 뒤 `/app/app.jar`가 시작됩니다.
- 로컬 전체 환경은 `docker compose --env-file .env -f infra/docker/compose.yml up --build`로 실행합니다.
- 기동 순서는 `postgres → migration 3개 → application 3개 → nginx`입니다.

## 메시징 운영 점검

- `node scripts/messaging-health.mjs`는 Connect와 task 상태, replication slot·WAL, topic 보존 설정, consumer lag, outbox·inbox 건수와 가장 오래된 record 나이를 JSON으로 출력합니다. 수치나 출력 형식을 읽을 수 없으면 정상으로 간주하지 않고 exit code 1로 끝납니다. 기본 Compose 환경 파일은 `.env`이며 다른 파일은 `MESSAGING_ENV_FILE`로 지정합니다.
- outbox·inbox는 자동 삭제하지 않습니다. `infra/docker/postgres/maintenance/inspect-messaging-retention.sql`은 schema별 건수와 가장 오래된 record만 조회합니다. CDC snapshot과 dead-letter 재처리 범위를 증명하는 watermark를 도입하기 전에는 데이터를 지우지 않습니다.
- 기존 connector 설정이나 비밀번호를 재적용할 때만 `CONNECTOR_CONFIG_UPDATE=true`로 `connect-init`을 한 번 실행합니다. 플래그가 없으면 connector 설정 drift를 보고하고 변경하지 않습니다.
- `postgres-provision`은 새 DB와 기존 volume 모두에서 역할·schema·최소 권한을 idempotent하게 조정합니다. 기존 비밀번호는 기본적으로 바꾸지 않습니다.
- DB 비밀번호를 회전할 때는 `.env`의 새 값을 저장하고 `DATABASE_CREDENTIAL_UPDATE=true`와 `CONNECTOR_CONFIG_UPDATE=true`를 함께 설정한 상태에서 Compose를 기동합니다. provision과 connector 갱신이 모두 성공한 뒤 두 플래그를 `false`로 되돌립니다. connector 갱신만 실패했다면 DB 비밀번호를 다시 바꾸지 않고 같은 새 값으로 `connect-init`을 재실행합니다.
- Kafka 장애 복구는 broker 정상 응답을 먼저 확인한 뒤 connector와 task를 재시작하고, health 결과가 `healthy` 또는 원인이 확인된 `warning`인지 검증합니다.
- Connect worker를 교체해도 Kafka internal topic의 config·offset과 PostgreSQL slot에서 이어서 처리합니다. 통합 테스트는 기존 worker container를 제거하고 새 worker로 이벤트 수신을 확인합니다.
- replication slot을 재생성한 경우 connector를 중지하고 slot이 없는 동안 저장된 outbox까지 `snapshot.mode=when_needed`로 다시 발행됐는지 event ID로 확인합니다. consumer inbox가 중복 반영을 막으므로 inbox 기록을 먼저 정리하지 않습니다.
