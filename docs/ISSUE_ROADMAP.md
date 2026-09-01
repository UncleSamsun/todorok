# TODOROK 기능 이슈 로드맵

이 문서는 GitHub에 발행한 기능 이슈의 순서와 의존성을 관리한다.

| 순서 | 이슈 | 기능 이슈 | PRD | 선행 |
|---:|---|---|---|---|
| 1 | [#1](https://github.com/UncleSamsun/todorok/issues/1) | 모노레포·3개 Spring 서비스·React PWA·공유 클라이언트 패키지·PostgreSQL·Kafka 기반 | §16~20 | 없음 |
| 2 | [#17](https://github.com/UncleSamsun/todorok/issues/17) | MVP 구현 전 기술 결정과 실행 계획 확정 | 전체 | #1 |
| 3 | [#24](https://github.com/UncleSamsun/todorok/issues/24) | 디자인 시스템 및 핵심 UI 기준 문서 확정 | §8.3, §10~11, §19.4, AC-1~2 | #17 |
| 4 | [#18](https://github.com/UncleSamsun/todorok/issues/18) | OpenAPI·이벤트 Schema·공통 오류 계약 | §16.4, §16.6, §20 | #17 |
| 5 | [#19](https://github.com/UncleSamsun/todorok/issues/19) | Spring Data JPA·Flyway·PostgreSQL 통합 테스트 기반 | §16.7, §17.3, §20 | #17 |
| 6 | [#20](https://github.com/UncleSamsun/todorok/issues/20) | Kafka Connect·Debezium outbox·consumer inbox 기반 | §16.4~16.5, §19.2~20 | #18, #19 |
| 7 | [#2](https://github.com/UncleSamsun/todorok/issues/2) | 사용자 인증·회전형 refresh session·서비스 간 JWT 검증 | §16.2, §16.6, §18 | #18, #19 |
| 8 | [#3](https://github.com/UncleSamsun/todorok/issues/3) | 달력 summary·일반 Task CRUD·오늘 화면 | §8~10, §19.2, AC-1 | #2, #18, #19, #24 |
| 9 | [#4](https://github.com/UncleSamsun/todorok/issues/4) | Task series·반복 일정·자동 이월·지난 기록·reopen | §9.1, §10.2~10.4, AC-3 | #3 |
| 10 | [#5](https://github.com/UncleSamsun/todorok/issues/5) | 할 일 메모·완료 메모·스티키노트 | §11 | #3 |
| 11 | [#6](https://github.com/UncleSamsun/todorok/issues/6) | 공통 Activity Record·Task projection·planner 이벤트 연동 | §9.2, §16.4~16.5, AC-2 | #3, #20 |
| 12 | [#7](https://github.com/UncleSamsun/todorok/issues/7) | 공부 카테고리와 version형 자유 기록 템플릿 | §12, AC-4 | #6 |
| 13 | [#8](https://github.com/UncleSamsun/todorok/issues/8) | 불변 운동 catalog·등록·세션·진급·재계산 | §13.1, §13.4, AC-5 | #6 |
| 14 | [#9](https://github.com/UncleSamsun/todorok/issues/9) | 푸시업 100개 6주 private catalog 등록 | §13.2~13.4 | #8 |
| 15 | [#10](https://github.com/UncleSamsun/todorok/issues/10) | Recon Ron 풀업 private catalog 등록 | §13.3~13.4 | #8 |
| 16 | [#11](https://github.com/UncleSamsun/todorok/issues/11) | 순수 상태 기계 기반 클라이밍 타이머·부분 기록 | §14, AC-6 | #6, #24 |
| 17 | [#12](https://github.com/UncleSamsun/todorok/issues/12) | 오늘 요약·선택형 웹 푸시·방해 금지·재시도 | §16.2, AC-8~AC-9 | #2, #3, #20 |
| 18 | [#13](https://github.com/UncleSamsun/todorok/issues/13) | iPhone PWA 설치와 온라인 저장 오류 처리 | §15, AC-7 | #3, #6, #24 |
| 19 | [#14](https://github.com/UncleSamsun/todorok/issues/14) | AWS Lightsail·migration·Nginx·ECR·S3 백업 배포 | §17~18 | #19, #20 |
| 20 | [#15](https://github.com/UncleSamsun/todorok/issues/15) | 4GB 메모리·WAL·부하·복구·8GB 승급 판정 | §17.2~17.4, §19~20 | #14 |

## 발행 규칙

- 새 기능 이슈는 `.github/ISSUE_TEMPLATE/feature.yml`을 사용한다.
- 각 이슈에는 표의 PRD 섹션과 수용 기준을 그대로 연결한다.
- 의존 이슈가 완료되기 전에는 후속 이슈를 구현 시작 상태로 옮기지 않는다.
- 구현 중 요구사항이 바뀌면 해당 기능 이슈의 첫 커밋에서 PRD를 먼저 변경한다.
