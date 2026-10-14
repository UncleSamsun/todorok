# 출시 증거와 남은 확인

기준: `docs/PRD.md` AC-1–9, `docs/plans/functional-progress.md`. 개발 검증과 공개 출시 판정은 별개다. 아래 실제 조건이 확보되지 않은 항목을 통과로 기록하지 않는다.

| 항목 | 필요한 증거 | 현재 판정 |
| --- | --- | --- |
| 일정·기록·반복·공부·운동·클라이밍 | HTTP→DB→Kafka 왕복 및 브라우저 핵심 흐름 | 기존 작업01–10 근거 보존, 후속 변경 영향 회귀 필요 |
| Task 알림 | 명시 시간·선택, 22–07 만료, +30분 deadline, 변경·취소·중복·역순·장애 복구 | 로컬 구현·notification33개·planner31개·실제 Kafka 왕복 통과 |
| 실제 푸시 | 운영 VAPID 키와 사용자 실제 브라우저 구독, 수신·클릭·해제 | 실제 iPhone 확인 필요 |
| 타이머 | iPhone 홈 화면 PWA, 잠금/백그라운드 복귀·Wake Lock·부분 기록 저장 | 실제 iPhone 확인 필요 |
| PWA 업데이트 | 설치된 이전 빌드에서 새 SW 활성화 후 lazy 화면 이동 | 자동 guard 검증 완료, 실제 설치 기기 검증 필요 |
| 자료 이용 범위 | 운동표·크림프 원본 출처와 사용 조건 증거, 실제 import checksum | 증거 미확보, 합성 데이터와 구분 |
| 배포·복구 | migration 실패 시 기존 runtime 유지, 빈 격리 DB 복원·서비스 왕복 | 실패 job의 runtime 보존·암호화/격리 fixture 복원 통과. 실제 운영 데이터·로그인/CDC 복구는 대기 |
| 운영 적합성 | 실제 4GB 환경 p95·lag·WAL·swap·OOM, 7일 관찰 | 실행 환경·관찰 기간 필요 |

2026-10-14 회사 서버 SSH 접속과 K3s 권한을 확인하고 전용 todorok-test namespace에서 배포를 진행했다. planner V12·activity V11·notification V6 migration, Kafka 토픽과 outbox publication/slot을 실측했다. 최종 readiness·HTTPS smoke 결과는 `docs/plans/2026-10-14-company-server-deploy.md`에 기록하며 짧은 smoke를 실기기·장기 운영 판정으로 해석하지 않는다.

## 실제 iPhone 확인 순서

1. HTTPS 주소를 Safari에서 열고 홈 화면에 추가한다. 기종·iOS·빌드·검증 날짜를 기록한다.
2. 로그인·앱 재시작·세션 갱신·로그아웃, 오늘/과거 일정과 기록 저장·수정·실패 재시도를 확인한다.
3. 설정의 계정 알림을 켠 뒤 별도 기기 연결 버튼으로 권한을 요청한다. 거절/재시도/구독 해제도 확인한다.
4. 요약·명시 Task 알림을 각각 수신하고 클릭이 앱의 오늘 화면으로 이동하는지 확인한다. push provider 수락 응답과 실제 표시를 구분한다.
5. 크림프 타이머를 시작하고 백그라운드·화면 잠금·복귀·일시정지·중단·부분 기록·완료 저장을 확인한다. 미지원 Wake Lock을 성공으로 표시하지 않는다.
6. 이전 빌드의 열린 앱을 유지한 채 새 빌드를 배포하고 아직 열지 않았던 lazy 화면으로 이동한다. 새로고침 반복·빈 화면·입력 유실을 기록한다.

실제 운영에는 별도 계정·HTTPS·백업 키 보관과 출처 조건 확인이 필요하다. 테스트용 키·합성 catalog·짧은 smoke를 운영 증거로 사용하지 않는다.
