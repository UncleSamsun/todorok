# 배포·migration 실패 대응

현재 배포 대상은 회사 `ai-server`의 K3s이며 전용 namespace는 `todorok-test`, 초기 HTTPS 주소는 `https://211.45.127.56/todorok/`다. 2026-10-14 mjkim SSH와 kubectl 권한을 실측했다. 과거 AWS Lightsail 목표 및 아래 Compose 도구는 단일 VM 검증 경로로 보존한다.

## 회사 K3s 배포

`company-server-images.yml`이 별도 `codex/company-server-deploy` 브랜치에서 애플리케이션 5개를 빌드한다. GitHub Actions의 `image-*` artifact만 다운로드해 component→GHCR digest JSON으로 합친다. 공개 저장소의 패키지는 사용자 선택에 따라 Public으로 설정하고 익명 pull을 확인한다. 다른 앱의 인증 정보는 재사용하지 않는다.

```powershell
node scripts/render-company-deployment.mjs C:\release\images.json C:\release\manifests https://211.45.127.56 RELEASE_ID 10.43.0.10
```

기존 namespace가 있다면 `app.kubernetes.io/part-of=todorok` 소유권을 확인한다. public manifest를 server dry-run으로 검증하고, 별도 ACL 제한 로컬 디렉터리에 `generate-company-secrets.mjs`로 한 번 생성한 secret.json을 mode 600 서버 release 디렉터리에 배치한다. 명령 출력·Git·vault에 비밀값을 남기지 않는다. 기존 secret은 덮어 생성하지 않는다.

`deploy-company-server.py RELEASE_DIRECTORY`는 foundation·secret·PostgreSQL/Kafka → 역할 → migration 3개 → 토픽·publication/slot → Connect·connector → bootstrap → runtime → HTTPS route 순으로 실행한다. 각 job은 의존 서비스 TCP 준비를 기다리고 실패 시 다음 단계로 넘어가지 않는다. PVC가 자신의 namespace에 속하는지 확인한 후 PV reclaim 정책만 Retain으로 변경한다. 기존 SyncDoc/VODA, 호스트 포트, 호스트 마운트와 sudo는 사용하지 않는다.

단일 Kafka의 KRaft controller는 `127.0.0.1:9093`으로 통신해 readiness 이전 Service 의존을 피한다. Nginx 동적 DNS upstream은 `notification-service.todorok-test.svc.cluster.local`과 CoreDNS를 사용한다. `/todorok/` UI와 서비스 워커는 해당 범위에 한정하고 API의 고유 `/api/{planner,activity,notification}/` 경로는 그대로 사용한다.

이미지 digest, manifest 수정 버전과 성공 job, DB history, connector 상태, 실제 HTTPS 로그인·생성·기록·CDC 및 기존 서비스 상태를 배포 기록에 남긴다. 실기기 push와 운영 백업 복원·장기 관찰은 별도 검증이다.

## 사전 조건

- CI에서 테스트·계약 drift·production build·번들 예산·migration 검사를 통과한 이미지 digest를 준비한다. 운영 서버에서 소스를 빌드하지 않는다.
- planner/activity/notification runtime와 migration 이미지가 같은 소스 digest인지 확인한다. 이전 이미지 digest와 Compose 설정도 보관한다.
- DB 백업을 암호화하고 외부 복사·격리 복구를 확인한다. `backup-restore.md` 참조.
- 인증 서명 키·서비스 간 인증 키·push VAPID/암호화 키는 Docker secret 또는 접근 제한 파일로 mount한다. `.env.example`의 예시 비밀번호를 운영에 사용하지 않는다.
- VM 보안 그룹은 공개 80/443, 제한된 관리 접속만 허용한다. DB/Kafka/Connect는 외부 공개하지 않는다. TLS 인증서와 자동 갱신을 준비한다.

## 배포 순서

`scripts/deploy-services.mjs`는 명시한 환경 파일과 Compose manifest를 검사하고 세 migration 성공 후에만 runtime을 교체한다. 아래 명령은 실제 배포를 실행하므로 검증된 대상·이미지 digest·백업을 준비한 뒤 운영 실행 단계에서 사용한다. 이번 로컬 검증에서는 별도 fixture 프로젝트에서 실패 경로만 실행했다.

```powershell
node scripts/deploy-services.mjs C:\secure\release.env C:\release\compose.yml C:\release\compose.release.yml
```

`node --test scripts/deploy-services.integration.test.mjs`는 실패한 migration job 뒤 기존 runtime 컨테이너 ID·실행 상태·이전 버전 label이 보존되는지 실제 Docker로 확인한다. 실제 DB migration·운영 deploy 성공을 대신하는 검사는 아니다.

1. 현재 서비스 health와 DB/Kafka/Connect 상태를 확인하고, 새 이미지 digest를 pull한다. 실행 중인 앱 컨테이너는 유지한다.
2. Compose 환경 파일·secret mount·메모리 제한을 검사한다. 설정 출력에는 secret 값이 포함될 수 있으므로 공유 로그에는 `docker compose ... config --quiet`만 사용한다.
3. planner→activity→notification migration을 `docker compose ... run --rm --no-deps <service>-migration`으로 순서대로 실행한다. 하나라도 실패하면 runtime 교체를 실행하지 않는다. migration 컨테이너의 종료 코드와 Flyway 상태를 기록한다.
4. 모든 migration이 성공한 뒤 runtime을 새 digest로 교체하고 health가 준비될 때까지 기다린다. Nginx의 upstream·새 자산·서비스 워커를 확인한다.
5. 로그인·일정 생성/완료·기록·알림 예약·outbox/inbox 왕복을 smoke로 확인한다. API에서 구독 원문·비밀키가 노출되지 않는지 확인한다.
6. 운영 p95·consumer lag·WAL·메모리/OOM·swap을 관찰한다. 7일 실제 관찰은 출시 체크리스트에서 별도 관리한다.

## 실패·롤백

Migration 실패 시 기존 runtime의 이미지·컨테이너를 유지한다. PostgreSQL의 transactional migration 실패는 해당 migration을 rollback하지만 앞서 성공한 다른 서비스의 migration까지 되돌리지 않는다. 그러므로 추가 컬럼·신규 테이블 같은 하위 호환 변경을 먼저 배포하고 파괴적 schema 변경은 별도 단계로 분리한다.

Runtime 교체 후 실패하면 이전 digest로 앱을 되돌린다. 이미 성공한 migration을 임의로 삭제하거나 Flyway history를 조작하지 않는다. 현재 Task 시간 컬럼/notification 신규 테이블은 추가 방식이지만 notification 예약 unique key 변경은 이전 worker의 SQL과 호환되지 않으므로 **notification V6 적용 후 이전 worker로 단독 rollback하면 안 된다**. 알림 worker를 비활성화하고 수정 버전으로 전진 복구한다. 일정·기록 서비스는 계속 제공한다.

DB 복원은 별도 빈 환경에서 실행한다. 운영 볼륨에 덮어쓰는 rollback을 자동화하지 않는다. 외부 전송 후 응답 유실에 따른 push 중복 가능성은 notification ID/tag로 줄이며 정확히 한 번으로 보고하지 않는다.

## 메모리와 출시 판정

`infra/docker/compose.yml`의 `mem_limit`과 실제 JVM/Kafka heap을 배포 manifest에 기록한다. Compose 상한의 합과 실제 resident memory는 다르며, 이 PC의 로컬 검증은 4GB VM의 부하 증거가 아니다. PRD의 4GB·7일 운영 목표와 8GB 승급 조건은 실제 운영 환경에서 판정한다. 확인되지 않은 성공 지표를 출시 완료로 바꾸지 않는다.
