# TODOROK 기능 이슈 로드맵

이 문서는 GitHub에 발행한 기능 이슈의 순서와 의존성을 관리한다.

| 순서 | 이슈 | 기능 이슈 | PRD | 선행 |
|---:|---|---|---|---|
| 1 | [#1](https://github.com/UncleSamsun/todorok/issues/1) | 모노레포·3개 Spring 서비스·React PWA·공유 클라이언트 패키지·PostgreSQL·Kafka 기반 | §16~20 | 없음 |
| 2 | [#2](https://github.com/UncleSamsun/todorok/issues/2) | 사용자 인증·세션·서비스 간 JWT 검증 | §16.2, §18 | #1 |
| 3 | [#3](https://github.com/UncleSamsun/todorok/issues/3) | 달력·일반 Task CRUD·오늘 화면 | §8~10, AC-1 | #2 |
| 4 | [#4](https://github.com/UncleSamsun/todorok/issues/4) | 반복 일정·자동 이월·지난 기록 | §10.3~10.4, AC-3 | #3 |
| 5 | [#5](https://github.com/UncleSamsun/todorok/issues/5) | 할 일 메모·완료 메모·스티키노트 | §11 | #3 |
| 6 | [#6](https://github.com/UncleSamsun/todorok/issues/6) | 공통 Activity Record와 planner 이벤트 연동 | §9.2, §16.4~16.5, AC-2 | #3 |
| 7 | [#7](https://github.com/UncleSamsun/todorok/issues/7) | 공부 카테고리와 자유 기록 템플릿 | §12, AC-4 | #6 |
| 8 | [#8](https://github.com/UncleSamsun/todorok/issues/8) | 운동 커리큘럼 엔진·등록·진급·반복 | §13.1, AC-5 | #6 |
| 9 | [#9](https://github.com/UncleSamsun/todorok/issues/9) | 푸시업 100개 6주 프로그램 데이터 | §13.2~13.4 | #8 |
| 10 | [#10](https://github.com/UncleSamsun/todorok/issues/10) | Recon Ron 풀업 프로그램 데이터 | §13.3~13.4 | #8 |
| 11 | [#11](https://github.com/UncleSamsun/todorok/issues/11) | 클라이밍 10라운드 타이머·부분 기록 | §14, AC-6 | #6 |
| 12 | [#12](https://github.com/UncleSamsun/todorok/issues/12) | notification-service·웹 푸시·재시도 | §16.2, AC-8 | #2, #3 |
| 13 | [#13](https://github.com/UncleSamsun/todorok/issues/13) | PWA 설치·오프라인 조회·재전송 큐 | §15, AC-7 | #3, #6 |
| 14 | [#14](https://github.com/UncleSamsun/todorok/issues/14) | AWS Lightsail·Nginx·ECR·S3 백업 배포 | §17~18 | #1 |
| 15 | [#15](https://github.com/UncleSamsun/todorok/issues/15) | 4GB 메모리 부하·복구·8GB 승급 판정 | §17.2~17.3, §19~20 | #14 |

## 발행 규칙

- 새 기능 이슈는 `.github/ISSUE_TEMPLATE/feature.yml`을 사용한다.
- 각 이슈에는 표의 PRD 섹션과 수용 기준을 그대로 연결한다.
- 의존 이슈가 완료되기 전에는 후속 이슈를 구현 시작 상태로 옮기지 않는다.
- 구현 중 요구사항이 바뀌면 해당 기능 이슈의 첫 커밋에서 PRD를 먼저 변경한다.
