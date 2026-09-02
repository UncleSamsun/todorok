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
- 상태: PRD 1.4.0, 디자인 시스템 1.2.11과 전체 MVP 구현 계획 승인 완료

## 개발 원칙

- 요구사항 변경은 [PRD](docs/PRD.md)를 먼저 수정합니다.
- 기능 하나를 이슈 하나와 Pull Request 하나로 관리합니다.
- `main`·`develop` 직접 커밋을 금지합니다.
- 상세 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 따릅니다.
