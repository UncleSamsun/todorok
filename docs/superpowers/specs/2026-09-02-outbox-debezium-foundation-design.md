# Outbox·Debezium·멱등 Consumer 기반 설계

## 목적

이슈 #20은 planner·activity의 DB transaction과 Kafka 발행 사이의 이중 쓰기를 제거하고, consumer가 중복 전달과 재시작에도 결과를 한 번만 반영하게 하는 메시징 기반을 만든다.

이 기반은 #2 인증 이후 #3 Task와 #6 Activity가 도메인 event를 안전하게 교환하기 위한 선행 작업이다. 이 이슈에서는 기반 table·공통 writer·inbox guard·Connect·topic·장애 검증까지만 구현하고 실제 도메인 listener는 후속 이슈가 소유한다.

## 범위

- planner·activity schema의 동일 구조 `outbox_event`
- planner·activity·notification schema의 동일 구조 `processed_event`
- 전체 event envelope를 원자적으로 저장하는 공통 outbox writer
- event ID를 transaction 안에서 선점하는 공통 inbox guard
- PostgreSQL `pgoutput`, publication 하나, persistent replication slot 하나
- Debezium 3.6.2.Final Kafka Connect worker 하나와 PostgreSQL connector 하나
- Outbox Event Router를 통한 topic routing과 JSON payload 확장
- Kafka domain topic·dead-letter topic의 보존 상한
- consumer retry·dead-letter 공통 factory
- Connect 중단·재시작, 중복 event ID, Kafka 복구, slot 유실 복구 테스트
- WAL·connector·consumer lag 점검 명령과 보존 정리 SQL

## 비범위

- Task·Activity·notification의 실제 event producer·listener 업무 로직
- 도메인별 projection과 상태 변경
- Schema Registry·Avro·Protobuf
- Connect worker 이중화와 Kafka multi-broker
- exactly-once라는 외부 보장
- 운영 scheduler·CloudWatch alarm 생성
- Toxiproxy 전체 장애 조합과 장기 soak test

## 고정 버전과 용량

- PostgreSQL `17.11-alpine`
- Apache Kafka `4.3.1` KRaft
- Debezium Connect `3.6.2.Final`
- Testcontainers `2.0.5`
- Connect worker heap 512MB, container memory 768MB
- PostgreSQL replication slot WAL 상한 2GB
- domain topic 7일 또는 partition당 1GB
- dead-letter topic 30일 또는 1GB
- outbox 7일, inbox 30일 보존

## 데이터 흐름

```text
planner 또는 activity transaction
  ├─ domain 변경
  └─ outbox_event INSERT
          │
          ▼ PostgreSQL WAL / pgoutput
Debezium PostgreSQL connector
  └─ Outbox Event Router
          │ key = aggregate_id
          │ value = 전체 EventEnvelope JSON
          ▼
todorok.task.v1 또는 todorok.activity.v1
          │
          ▼
consumer local transaction
  ├─ processed_event INSERT ON CONFLICT DO NOTHING
  ├─ 최초 event면 local 결과 반영
  └─ commit 후 offset commit
```

Kafka와 consumer는 at-least-once 전달을 전제로 한다. 중복 억제 기준은 Kafka offset이 아니라 envelope의 `eventId`다.

## Database schema

### outbox_event

planner와 activity migration V2가 자기 schema에 다음 table을 만든다.

```sql
create table outbox_event (
    id uuid primary key,
    aggregatetype varchar(80) not null,
    aggregateid varchar(255) not null,
    type varchar(100) not null,
    payload jsonb not null,
    occurred_at timestamptz not null,
    created_at timestamptz not null default now(),
    constraint outbox_payload_is_object
        check (jsonb_typeof(payload) = 'object')
);

create index outbox_event_created_at_idx on outbox_event(created_at);
```

column 이름은 Debezium Outbox Event Router의 기본 field와 맞춘다. `id`는 `EventEnvelope.eventId`, `type`은 `EventType`, `occurred_at`은 DB audit용 업무 발생 시각, `payload`는 payload fragment가 아니라 전체 `EventEnvelope` JSON이다. Kafka message value가 현재 JSON Schema 계약과 바로 일치하게 하기 위해서다. Kafka record timestamp는 Debezium capture 시각을 사용하고 정확한 업무 시각은 envelope의 `occurredAt`을 기준으로 한다.

`aggregatetype`은 `task` 또는 `activity`, `aggregateid`는 aggregate UUID 문자열이다. 같은 aggregate ID를 Kafka key로 사용해 한 partition 안의 순서를 유지한다.

outbox row는 INSERT만 허용한다. UPDATE API를 제공하지 않으며 Connect 설정은 outbox UPDATE를 fatal 오류로 취급한다. 삭제는 보존 작업에서만 수행하고 Outbox Event Router가 delete event를 내보내지 않게 한다.

### processed_event

세 서비스 migration V2가 다음 table을 만든다.

```sql
create table processed_event (
    event_id uuid primary key,
    event_type varchar(100) not null,
    processed_at timestamptz not null default now()
);

create index processed_event_processed_at_idx on processed_event(processed_at);
```

consumer는 local 결과를 쓰는 transaction에서 `processed_event`를 먼저 INSERT한다. insert count가 1이면 업무 반영을 계속하고 0이면 이미 처리한 event이므로 성공으로 종료한다. 업무 반영이 rollback되면 inbox INSERT도 함께 rollback되어 다음 전달에서 다시 처리할 수 있다.

## 공통 messaging-support library

새 Gradle module `libs:messaging-support`는 Spring JDBC와 Jackson 3만 의존하며 Kafka listener나 domain type을 소유하지 않는다.

### OutboxEventWriter

```java
public interface OutboxEventWriter {
    void append(String aggregateType, String aggregateId, EventEnvelope<?> event);
}
```

`JdbcOutboxEventWriter`는 현재 transaction의 DataSource를 사용해 `outbox_event`를 INSERT한다. `aggregateType`, `aggregateId`, envelope 필수 값이 비어 있으면 DB 접근 전에 `IllegalArgumentException`을 던진다. Jackson 직렬화 실패는 transaction을 실패시키며 JSON 문자열을 수동 조합하지 않는다.

동일 event ID 재사용은 primary key 위반으로 transaction 전체를 실패시킨다. producer의 idempotency command 처리는 후속 domain 이슈가 맡는다.

### InboxEventGuard

```java
public interface InboxEventGuard {
    boolean claim(UUID eventId, String eventType);
}
```

`JdbcInboxEventGuard`는 다음 SQL의 update count가 1일 때만 true를 반환한다.

```sql
insert into processed_event(event_id, event_type)
values (?, ?)
on conflict (event_id) do nothing;
```

이 class는 transaction을 열거나 commit하지 않는다. listener의 local 결과와 같은 `@Transactional` boundary 안에서 호출해야 한다.

### ConsumerFailureHandlerFactory

```java
public interface ConsumerFailureHandlerFactory {
    CommonErrorHandler create(KafkaOperations<Object, Object> operations, String deadLetterTopic);
}
```

factory는 `DefaultErrorHandler`와 `DeadLetterPublishingRecoverer`를 구성한다. 고정 backoff 1초, 총 3회 재시도 후 `todorok.dead-letter`로 보낸다. dead-letter record에는 원본 topic·partition·offset과 예외 class header를 유지한다. payload 본문은 application log에 남기지 않는다.

Debezium source connector의 transform·serialization 오류에는 `errors.tolerance=none`을 적용한다. source record를 건너뛰면 outbox와 Kafka 사이에 조용한 유실이 생기므로 source 측 dead-letter로 대체하지 않는다.

## PostgreSQL logical replication

PostgreSQL container는 다음 값을 command argument로 받는다.

```text
wal_level=logical
max_wal_senders=1
max_replication_slots=1
max_slot_wal_keep_size=2048MB
```

초기화 script는 `debezium_app` login role을 만들고 `REPLICATION` 권한을 부여한다. planner·activity schema 사용 권한과 현재·미래 outbox table SELECT 권한을 제공하되 INSERT·UPDATE·DELETE 권한은 주지 않는다.

서비스 migration이 끝난 뒤 one-shot `replication-init`이 관리자 계정으로 다음 resource를 idempotent하게 만든다.

- publication: `todorok_outbox`
- tables: `planner.outbox_event`, `activity.outbox_event`
- slot: `todorok_outbox_slot`, plugin `pgoutput`

publication 또는 slot의 이름·plugin이 다르면 자동 수정하지 않고 실패한다.

## Kafka Connect와 connector

Compose의 `connect` service는 `quay.io/debezium/connect:3.6.2.Final`을 고정한다. worker internal topic은 다음 세 개이며 replication factor는 1이다.

- `todorok.connect.configs`
- `todorok.connect.offsets`
- `todorok.connect.status`

worker key/value converter는 JSON schema를 포함하지 않는다. Connect health check는 `/connectors` REST 응답을 사용한다.

`connect-init` one-shot service가 `infra/docker/connect/register-connector.sh`을 실행해 connector가 없으면 생성하고, 같은 이름이 있으면 현재 config와 파일 config를 비교한다. 중요한 값이 다르면 임의 PUT으로 덮어쓰지 않고 실패해 설정 drift를 드러낸다.

connector 이름은 `todorok-postgres-outbox`이며 핵심 설정은 다음과 같다.

```properties
connector.class=io.debezium.connector.postgresql.PostgresConnector
tasks.max=1
key.converter=org.apache.kafka.connect.storage.StringConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
value.converter.schemas.enable=false
plugin.name=pgoutput
slot.name=todorok_outbox_slot
publication.name=todorok_outbox
publication.autocreate.mode=disabled
table.include.list=planner.outbox_event,activity.outbox_event
snapshot.mode=when_needed
transforms=outbox
transforms.outbox.type=io.debezium.transforms.outbox.EventRouter
transforms.outbox.route.by.field=aggregatetype
transforms.outbox.route.topic.replacement=todorok.${routedByValue}.v1
transforms.outbox.table.expand.json.payload=true
transforms.outbox.table.op.invalid.behavior=fatal
errors.tolerance=none
errors.log.enable=true
errors.log.include.messages=false
```

database password는 JSON 파일에 넣지 않고 register script가 환경 변수에서 config request에 주입한다.

## Kafka topic 정책

`kafka-init` one-shot service가 broker health 이후 다음 topic을 `--if-not-exists`로 만든다.

| Topic | Partitions | retention.ms | retention.bytes |
|---|---:|---:|---:|
| `todorok.task.v1` | 3 | 604800000 | 1073741824 |
| `todorok.activity.v1` | 3 | 604800000 | 1073741824 |
| `todorok.dead-letter` | 3 | 2592000000 | 1073741824 |

Connect internal topic은 compact 정책과 partition 1개를 사용한다. 기존 topic의 설정이 다르면 init script가 `kafka-configs.sh --alter`로 명시된 네 보존 값만 맞춘다.

## 기동 순서

```text
postgres healthy ── migration 3개 complete ── replication-init complete
       │                                      │
       └──────────────────────────────────── connect healthy
kafka healthy ── kafka-init complete ─────────┤
                                               └─ connect-init complete

planner·activity·notification service는
자기 migration + kafka-init 성공 뒤 시작
```

Connect·connect-init은 application 기동 gate가 아니다. Connect가 중단되어도 application transaction과 outbox 저장은 계속되고, 복구 뒤 밀린 event를 발행해야 한다. notification은 현재 outbox를 쓰지 않지만 향후 listener가 domain topic과 inbox를 사용하도록 kafka-init만 기다린다.

## 보존과 상태 점검

`infra/docker/postgres/maintenance/prune-messaging.sql`은 다음 조건을 만족할 때만 outbox를 삭제한다.

- slot `todorok_outbox_slot` 존재
- slot active
- `pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn)`이 16MB 이하

조건이 아니면 exception으로 종료한다. 조건이 맞으면 planner·activity에서 `created_at < now() - interval '7 days'`를 삭제하고 세 schema에서 `processed_at < now() - interval '30 days'`를 삭제한다. scheduler 연결은 #14에서 수행한다.

`scripts/messaging-health.mjs`는 다음 값을 JSON으로 출력하고 limit 위반 시 exit code 1을 반환한다.

- Connect connector와 task state
- replication slot active 여부와 retained WAL bytes
- domain·dead-letter topic retention config
- consumer group lag

WAL retained bytes 1.5GB는 warning, 2GB는 critical이다. connector 또는 task가 RUNNING이 아니면 critical이다.

## 테스트 전략

### 1. messaging-support PostgreSQL 통합 테스트

- domain marker와 outbox append가 함께 commit
- 예외 발생 시 domain marker와 outbox가 함께 rollback
- 같은 event ID claim 두 번 호출 시 true, false
- claim 뒤 local update가 실패하면 rollback 후 다시 true
- outbox payload가 event JSON Schema를 통과
- 7일·30일 보존 SQL의 경계 시각

### 2. Kafka consumer 기반 테스트

- 동일 event ID를 서로 다른 offset으로 두 번 전달해 local 결과 한 번
- 처리 예외를 3회 재시도한 뒤 dead-letter 한 건
- dead-letter header에 원본 위치와 예외 class 포함
- consumer 재시작 뒤 inbox가 중복 적용 차단

### 3. 전체 CDC 왕복 테스트

별도 `tests:messaging-integration` Gradle module이 PostgreSQL 17.11, Kafka 4.3.1, Debezium Connect 3.6.2.Final container를 같은 network에서 실행한다.

- planner outbox INSERT → `todorok.task.v1` envelope 수신
- activity outbox INSERT → `todorok.activity.v1` envelope 수신
- 같은 aggregate ID event 순서 유지
- Connect 중단 중 INSERT → 재시작 후 누락 없이 수신
- Kafka 일시 중단 뒤 Connect 복구 → 누락 없이 수신
- Connect 중단, slot 삭제·재생성, `when_needed` snapshot → 중복 가능하지만 event ID 보존
- outbox 이외 table 변경이 topic에 나타나지 않음

통합 테스트는 container reuse와 host의 기존 Compose volume을 사용하지 않는다. 타임아웃은 조건 polling으로 처리하고 고정 sleep을 사용하지 않는다.

## 실패 처리

- domain transaction rollback: outbox row 없음
- outbox ID 충돌: producer transaction 실패
- Connect 중단: slot이 WAL 보존, 재시작 후 offset부터 재개
- Kafka 중단: Connect task 실패 또는 backoff, broker 복구 후 재개
- malformed payload: connector fail-fast, 운영자 수정 전 진행하지 않음
- consumer 업무 오류: 3회 재시도 후 dead-letter
- duplicate delivery: inbox false, local 결과 생략
- inbox와 local 결과 rollback: 다음 delivery에서 재시도
- slot retained WAL 2GB 도달: PostgreSQL이 추가 WAL 보존을 제한하므로 critical 경보와 Connect 복구가 필요
- slot 유실: connector 중지 상태에서 slot 재생성, `when_needed` snapshot, inbox dedupe

## 수용 기준 대응

- DB commit 이후 Kafka 발행: outbox writer와 전체 CDC 왕복 테스트
- Connect·Kafka·consumer 재시작과 중복 전달: 장애 통합 테스트와 inbox transaction 테스트
- WAL·topic·outbox·inbox 상한: PostgreSQL command, topic init, prune SQL, health script
- Connect 중단·재시작: persistent slot과 offset internal topic 검증
- dead-letter 이동: consumer failure handler Kafka 테스트
- slot 유실 복구: slot 재생성·snapshot·event ID 중복 검증
