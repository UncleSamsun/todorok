# 백업·격리 복구

프로덕션 DB 복원은 기존 데이터를 덮어쓰지 않는다. 새 VM·새 Compose 프로젝트·빈 볼륨에서 복구한 뒤 검증하고 전환한다. PostgreSQL 17의 custom-format dump와 `scripts/backup-archive.mjs`의 AES-256-GCM 파일을 사용한다. 실제 운영 백업/스케줄·외부 보관·서비스 로그인 복구 검증은 운영 환경에서 별도 실행해야 한다.

## 2026-10-14 초기 서버 백업 검증

회사 todorok-test의 실제 초기 DB dump와 앱 secret을 이 PC의 ACL 제한 ignored .local/company-server/private/initial-backup-20261014 에 AES-256-GCM으로 보관했다. 키는 별도 상위 backup.key 파일이며 본문을 Git·vault·로그에 남기지 않는다. 암호화 파일의 SHA-256 manifest와 복원 결과를 같은 private 디렉터리에 보관한다.

복호화 내용 일치와 별도 network-none PostgreSQL 17 복원에서 migration12/11/6, 초기 계정1개, planner_app 소유권·Debezium SELECT 권한을 검증했다. plaintext와 임시 복원 컨테이너는 제거했다. 서버 원본 데이터는 변경하지 않았다. 자동 백업 스케줄·보관/경보와 복원된 앱의 로그인·CDC는 별도 확인이 필요하다.

## 키와 보관

32바이트 난수의 base64 문자열 파일을 백업 키로 준비한다. 키는 Git·백업 파일과 분리하고 비밀 저장소에 보관한다. push 구독의 DB 암호문을 복호화할 `PUSH_ENCRYPTION_KEY`와 인증 서명 키도 별도 백업해야 한다. DB dump만으로 키는 복구되지 않는다.

운영 서버에서 매일 03:00 KST 백업, 일간 7개·주간 4개·월간 3개 보관을 기준으로 한다. 스케줄과 외부 저장소는 아직 설치하지 않았다. 백업 실패·외부 복사 실패·마지막 성공 26시간 초과를 모니터링하고 월 1회 새 격리 환경에서 복구한다. 실사용 전 최대 허용 데이터 손실과 복구 시간을 실제 결과로 확정한다.

## 백업 절차

1. DB 컨테이너에서 `pg_dump -Fc --no-publications --no-subscriptions`를 DB 관리 계정으로 실행한다. 비밀번호를 명령행이나 로그에 쓰지 않고 컨테이너의 기존 보안 환경/pgpass를 사용한다. 테이블 owner와 ACL을 보존하므로 복원 시 동일 서비스 role을 먼저 생성해야 한다.
2. 접근이 제한된 작업 디렉터리로 dump를 복사한 뒤 암호화한다. output은 새 파일이어야 하며 기존 파일을 덮어쓰지 않는다.

```powershell
node scripts/backup-archive.mjs seal --input C:\secure\db.dump --output C:\secure\db.dump.enc --key-file C:\secure-keys\backup.key
```

3. 암호화 파일의 SHA-256·생성 시각·PostgreSQL/앱 이미지 digest를 manifest에 기록하고 외부 저장소에 복사해 checksum을 대조한다. 평문 dump는 접근 권한을 제한하고 검증·보관 정책에 따라 제거한다. 키·평문·원문 구독값은 로그에 남기지 않는다.

## 복구 절차

1. 새 격리 DB와 서비스 role을 준비한다. 서비스 앱과 CDC connector는 아직 시작하지 않는다. `001-create-service-roles.sh`가 schema도 생성하므로 이미 생성된 빈 schema는 새 DB에서만 정리하거나 role 생성 단계를 분리한다. 기존 운영 DB에는 restore/clean을 실행하지 않는다.
2. 새 파일로 복호화한다. 잘못된 키·변조는 실패하고 최종 output을 남기지 않는다. 검증 중 평문 임시 파일은 같은 디렉터리에서 만들어지므로 이 경로도 접근을 제한한다.

```powershell
node scripts/backup-archive.mjs open --input C:\secure\db.dump.enc --output C:\secure\verified.dump --key-file C:\secure-keys\backup.key
```

3. 빈 DB에 `pg_restore --exit-on-error --single-transaction`으로 복구한다. role/owner/ACL을 보존한다. 운영 백업에서는 `--no-owner`·`--no-acl`을 사용하지 않는다. 격리 fixture 테스트의 해당 옵션은 서비스 권한 복구 증거가 아니다.
4. user·Task·Activity·구독/예약 수와 Flyway 버전, 로그인·일정 조회·기록 저장/수정·기기 구독 복호화를 확인한다. 기존 키 없이 암호문 복구를 성공으로 판정하지 않는다.
5. outbox publication·replication role 권한을 재구성한다. 새 slot과 Connect offset은 기존 offset을 무조건 재사용하지 않고 새 DB 기준으로 시작한다. inbox/aggregate version으로 replay의 멱등성을 확인한 뒤 운영 전환한다.

## 자동 검증

`node --test scripts/backup-archive.test.mjs scripts/backup-postgres.integration.test.mjs`

암호화·변조/다른 키 거절·기존 파일 보존과 별도 PostgreSQL 컨테이너의 세 schema fixture 복원을 검증한다. 실제 사용자 로그인·CDC 복구·운영 데이터 복원은 이 작은 fixture 검사에 포함하지 않는다.
