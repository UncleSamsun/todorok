# 템플릿 선택 승인 운영

내부 POST 주소는 `/api/activity/v1/internal/template-selections`이며 공개 Nginx에서는 내부 경로와 하위 경로를404로 차단한다. planner에서 activity로 직접 요청한다. activity 컨테이너 포트는8082다.

## 키와 설정

RSA2048비트 이상 별도 키 쌍을 준비한다. private key는 PKCS8 PEM, public key는 X509 PEM이다. 사용자 access-token 키를 재사용하면 설정 또는 승인 요청이 거절된다. private key는 planner에만, public key는 activity에만 전달한다. 파일을 저장소·이미지·로그에 넣지 않는다.

기존 `scripts/New-LocalAuthSecrets.ps1 -Directory .local/template-service`로 별도 디렉터리에 개발용 키를 만들 수 있다. 이미 파일이 있으면 덮어쓰지 않고 거절한다.

호스트에서 `TODOROK_TEMPLATE_SERVICE_PRIVATE_KEY_FILE`, `TODOROK_TEMPLATE_SERVICE_PUBLIC_KEY_FILE`에 각 파일의 절대 경로를 설정하고 다음 override를 사용한다.

```powershell
docker compose --env-file .env -f infra/docker/compose.yml -f infra/docker/compose.template-service.yml up -d
```

이 override는 호스트 파일을 읽기 전용 Docker secrets로 마운트하고 서비스에는 `file:/run/secrets/...` resource 위치를 전달한다. 직접 Java로 실행할 때는 같은 환경변수에 `file:C:/.../private.pem` 또는 `file:/.../public.pem` 같은 Spring resource 위치를 사용한다. 기본 Compose만 사용하는 기존 기동에는 추가 secret 파일이 필요하지 않으며, 연결 생성은503으로 fail-closed한다.

planner의 `TODOROK_TEMPLATE_SERVICE_BASE_URL` 기본값은 `http://activity-service:8082/api/activity/v1`이다. 내부 통신은 신뢰된 서비스 네트워크에서 수행하고 외부 ingress로 라우팅하지 않는다. 해당 URL 변경은 운영 구성 변경이며 요청자가 지정할 수 없다.

## 순서와 복구

1. 별도 키를 배치하고 activity의 V6 migration을 적용한다. 먼저 v1/v2 소비자와 내부 보안 체인을 배포한다.
2. Connect 설정의 planner 전용 null 보존 SMT를 먼저 반영한다. `CONNECTOR_CONFIG_UPDATE=true`로 의도한 설정 갱신을 허용하고 기존 connector 등록 절차를 실행한다. activity 원본 topic용 SMT는 기존 null 생략 동작을 유지한다. 두 SMT 모두 원본 topic에 명시적 predicate를 적용하므로 이미 라우팅한 메시지를 중복 변환하지 않는다.
3. planner V9 migration을 적용하고 승인 client 및 v2 producer를 배포한다. 기존 topic은 그대로 유지한다.
4. Nginx 내부 차단과 공개 사용자 API 정상 응답을 확인한다. 브라우저에서 service JWT나 내부 승인 요청을 만들지 않는다.
5. 공개 Task/series 생성으로 내부 승인→일정 outbox→activity projection→record-template GET을 검증한다.

기존 connector 갱신은 검토한 설정 파일을 포함한 등록 이미지를 다시 빌드해 수행한다. replication slot·publication·기존 outbox는 그대로 둔다.

```powershell
docker compose --env-file .env -f infra/docker/compose.yml run --rm --build -e CONNECTOR_CONFIG_UPDATE=true connect-init
```

planner 이벤트는 v2가 요구하는 명시적 null을 보존하며, ActivityCompleted/Corrected/Voided v1의 null 생략은 기존대로다. topic 이름이나 eventId를 바꾸지 않으므로 이전 이벤트와 inbox 중복 방지 의미도 유지한다.

내부 JWT issuer/subject=`todorok-planner`, audience=`todorok-activity-internal`, scope=`template:select`, method=`POST`, path=`/api/activity/v1/internal/template-selections`, 최대 수명60초를 사용한다. ownerId/requestId/targetType/targetId 및 실제 전송 본문 SHA-256 fingerprint를 확인한다. 서비스 principal과 사용자 UUID principal은 별도 SecurityFilterChain에서 처리한다.

승인 응답 유실, planner 최종 저장 실패, 최종 응답 유실에는 동일 commandId와 원래 요청을 다시 보낸다. PENDING command·이미 발행된 binding·inbox/outbox를 지우지 않는다. template archive 뒤에도 이미 승인된 요청은 원래 binding으로 복구된다. 승인 전 archive된 새 선택은409다. 확정409의 내용을 수정하려면 새 commandId를 사용한다.

연결 기록 쓰기는09B2 완성 전 `TEMPLATE_RECORD_NOT_READY` 409로 거절한다. 이를 풀기 위해 templateLink를 제거하거나 STANDARD 기록으로 우회하지 않는다. record-template GET은 현재 전체 정의를 읽으며 선택 당시 미리보기 version과 새 기록 version을 혼동하지 않는다.

키 교체는 새 승인을 잠시 중단하고 두 서비스의 짝을 함께 교체한다. 기존 Task/binding은 키 변경에 의존하지 않으며 계속 조회된다. 토큰·키·요청 본문을 진단 로그에 출력하지 않는다.
