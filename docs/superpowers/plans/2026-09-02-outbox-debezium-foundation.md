# Outbox·Debezium·멱등 Consumer 기반 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** planner·activity의 DB commit을 Debezium으로 Kafka에 전달하고 세 서비스 consumer가 event ID 중복을 안전하게 차단하는 메시징 기반을 구현한다.

**Architecture:** planner·activity transaction은 전체 `EventEnvelope` JSON을 표준 outbox table에 INSERT한다. 단일 Debezium PostgreSQL connector가 `pgoutput` slot과 Outbox Event Router로 domain topic에 전달하고, consumer는 local 결과와 `processed_event` claim을 같은 transaction에서 commit한다.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Spring JDBC, Spring Kafka 4.1.1, PostgreSQL 17.11, Apache Kafka 4.3.1, Debezium Connect 3.6.2.Final, Testcontainers 2.0.5, Docker Compose v2, Node.js 24.

**Spec:** `docs/superpowers/specs/2026-09-02-outbox-debezium-foundation-design.md`

## Global Constraints

- `outbox_event`는 planner·activity에만, `processed_event`는 세 서비스 모두에 둔다.
- outbox `payload`는 fragment가 아니라 전체 `EventEnvelope` JSON이다.
- outbox row는 INSERT만 허용하며 같은 event ID는 transaction을 실패시킨다.
- inbox claim은 consumer local 결과와 같은 transaction에서 실행하고 자체 transaction을 열지 않는다.
- delivery guarantee는 at-least-once이며 exactly-once라고 표현하지 않는다.
- Debezium source 오류는 `errors.tolerance=none`, consumer 업무 오류는 1초 간격 3회 재시도 후 dead-letter다.
- PostgreSQL slot은 `pgoutput`, `todorok_outbox_slot` 하나만 사용하고 WAL 상한은 2048MB다.
- domain topic은 7일·partition당 1GB, dead-letter는 30일·1GB다.
- H2, embedded Kafka, container reuse를 사용하지 않는다.
- 실제 Task·Activity projection listener와 업무 로직은 포함하지 않는다.
- 저장소와 Git 기록에 금지된 작성 주체·도구 이름을 남기지 않는다.

---

### Task 1: Outbox·Inbox V2 migration

**Files:**
- Create: `services/planner-service/src/main/resources/db/migration/V2__add_messaging_tables.sql`
- Create: `services/activity-service/src/main/resources/db/migration/V2__add_messaging_tables.sql`
- Create: `services/notification-service/src/main/resources/db/migration/V2__add_inbox_table.sql`
- Modify: `services/planner-service/src/test/java/io/todorok/planner/persistence/PlannerPersistenceIntegrationTest.java`
- Modify: `services/activity-service/src/test/java/io/todorok/activity/persistence/ActivityPersistenceIntegrationTest.java`
- Modify: `services/notification-service/src/test/java/io/todorok/notification/persistence/NotificationPersistenceIntegrationTest.java`

**Interfaces:**
- Consumes: 서비스별 Flyway schema와 PostgreSQL 17.11 Testcontainers.
- Produces: planner·activity `outbox_event`, 세 서비스 `processed_event`.

- [ ] **Step 1: migration 실패 테스트 작성**

planner와 activity persistence test에 다음 검사를 추가한다.

```java
@Test
void createsOutboxAndInboxConstraints() {
    assertThat(jdbc.queryForList(
            "select column_name from information_schema.columns "
                    + "where table_schema = 'planner' and table_name = 'outbox_event' "
                    + "order by ordinal_position",
            String.class)).containsExactly(
                    "id", "aggregatetype", "aggregateid", "type",
                    "payload", "occurred_at", "created_at");
    assertThat(jdbc.queryForObject(
            "select count(*) from information_schema.table_constraints "
                    + "where table_schema = 'planner' and table_name = 'processed_event' "
                    + "and constraint_type = 'PRIMARY KEY'",
            Integer.class)).isEqualTo(1);
}
```

activity test는 schema literal만 `activity`로 바꾼다. notification test는 `processed_event` column이 `event_id`, `event_type`, `processed_at` 순서인지 검증하고 `outbox_event`가 0개임을 검증한다.

- [ ] **Step 2: 실패 확인**

Run: `gradlew.bat :services:planner-service:test :services:activity-service:test :services:notification-service:test --tests '*PersistenceIntegrationTest' --no-daemon --no-configuration-cache --max-workers=1`

Expected: FAIL, V2 table이 없다.

- [ ] **Step 3: planner·activity V2 작성**

두 schema의 SQL은 table 이름과 column을 다음으로 고정한다.

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

create table processed_event (
    event_id uuid primary key,
    event_type varchar(100) not null,
    processed_at timestamptz not null default now()
);

create index processed_event_processed_at_idx on processed_event(processed_at);
```

- [ ] **Step 4: notification V2 작성**

notification V2는 위 `processed_event` table과 index만 만든다.

- [ ] **Step 5: migration test 통과**

Run: `gradlew.bat :services:planner-service:test :services:activity-service:test :services:notification-service:test --tests '*PersistenceIntegrationTest' --no-daemon --no-configuration-cache --max-workers=1`

Expected: PASS, planner·activity migration version은 3개, notification은 3개이며 table 소유권이 분리된다.

- [ ] **Step 6: 커밋**

```bash
git add services
git commit -m "feat(messaging): 서비스별 outbox와 inbox table 추가"
```

---

### Task 2: messaging-support module과 Outbox writer

**Files:**
- Modify: `settings.gradle.kts`
- Create: `libs/messaging-support/build.gradle.kts`
- Create: `libs/messaging-support/src/main/java/io/todorok/messaging/OutboxEventWriter.java`
- Create: `libs/messaging-support/src/main/java/io/todorok/messaging/JdbcOutboxEventWriter.java`
- Create: `libs/messaging-support/src/test/java/io/todorok/messaging/OutboxEventWriterIntegrationTest.java`
- Modify: `services/planner-service/build.gradle.kts`
- Modify: `services/activity-service/build.gradle.kts`

**Interfaces:**
- Consumes: `EventEnvelope<?>`, Jackson 3 `ObjectMapper`, transaction-bound DataSource.
- Produces: `OutboxEventWriter.append(String aggregateType, String aggregateId, EventEnvelope<?> event)`.

- [ ] **Step 1: outbox 원자성 실패 테스트 작성**

```java
@Testcontainers
class OutboxEventWriterIntegrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17.11-alpine");

    @Test
    void commitsDomainMarkerAndEnvelopeTogether() {
        var fixture = fixture();
        fixture.transaction.executeWithoutResult(status -> {
            fixture.jdbc.update("insert into domain_marker(id) values (?)", 1);
            fixture.writer.append("task", TASK_ID.toString(), envelope(EVENT_ID));
        });

        assertThat(fixture.jdbc.queryForObject(
                "select payload ->> 'eventId' from outbox_event where id = ?",
                String.class, EVENT_ID)).isEqualTo(EVENT_ID.toString());
        assertThat(fixture.jdbc.queryForObject(
                "select count(*) from domain_marker", Integer.class)).isEqualTo(1);
    }

    @Test
    void rollsBackDomainMarkerAndOutboxTogether() {
        var fixture = fixture();
        assertThatThrownBy(() -> fixture.transaction.executeWithoutResult(status -> {
            fixture.jdbc.update("insert into domain_marker(id) values (?)", 1);
            fixture.writer.append("task", TASK_ID.toString(), envelope(EVENT_ID));
            throw new IllegalStateException("rollback-probe");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(fixture.jdbc.queryForObject(
                "select count(*) from domain_marker", Integer.class)).isZero();
        assertThat(fixture.jdbc.queryForObject(
                "select count(*) from outbox_event", Integer.class)).isZero();
    }
}
```

`fixture()`는 container DataSource에 `domain_marker`와 Task 1의 outbox DDL을 실행하고 `JdbcTemplate`, `TransactionTemplate`, `JdbcOutboxEventWriter`를 반환한다. envelope는 고정 UUID·Instant와 `TaskScheduled` payload를 사용한다.

- [ ] **Step 2: 실패 확인**

Run: `gradlew.bat :libs:messaging-support:test --tests '*OutboxEventWriterIntegrationTest' --no-daemon --no-configuration-cache`

Expected: FAIL, module과 writer가 없다.

- [ ] **Step 3: module dependency 작성**

```kotlin
plugins { `java-library` }

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
    api(project(":libs:event-contracts"))
    implementation("org.springframework:spring-jdbc")
    implementation("tools.jackson.core:jackson-databind")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.postgresql:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

planner와 activity service는 `implementation(project(":libs:messaging-support"))`를 추가한다.

- [ ] **Step 4: Outbox writer 구현**

```java
public interface OutboxEventWriter {
    void append(String aggregateType, String aggregateId, EventEnvelope<?> event);
}
```

`JdbcOutboxEventWriter` constructor는 `JdbcTemplate`과 `ObjectMapper`를 받는다. `append`는 blank aggregate 값을 거부하고 `writeValueAsString(event)` 결과를 문자열 parameter로 전달한다. Jackson serialization 오류는 `IllegalStateException`으로 감싸 transaction을 실패시킨다.

```sql
insert into outbox_event(
    id, aggregatetype, aggregateid, type, payload, occurred_at
) values (?, ?, ?, ?, cast(? as jsonb), ?)
```

- [ ] **Step 5: validation과 duplicate 검사 추가**

test에 blank aggregate type·ID가 `IllegalArgumentException`, 같은 event ID 두 번 append가 `DuplicateKeyException`이며 transaction 전체가 rollback되는 case를 추가한다.

- [ ] **Step 6: writer test 통과**

Run: `gradlew.bat :libs:messaging-support:test --tests '*OutboxEventWriterIntegrationTest' --no-daemon --no-configuration-cache`

Expected: PASS, envelope JSON과 domain marker가 같은 transaction 결과를 가진다.

- [ ] **Step 7: 커밋**

```bash
git add settings.gradle.kts libs/messaging-support services/planner-service/build.gradle.kts services/activity-service/build.gradle.kts
git commit -m "feat(messaging): transaction outbox writer 추가"
```

---

### Task 3: Inbox guard와 transaction 중복 차단

**Files:**
- Create: `libs/messaging-support/src/main/java/io/todorok/messaging/InboxEventGuard.java`
- Create: `libs/messaging-support/src/main/java/io/todorok/messaging/JdbcInboxEventGuard.java`
- Create: `libs/messaging-support/src/test/java/io/todorok/messaging/InboxEventGuardIntegrationTest.java`
- Modify: `services/notification-service/build.gradle.kts`

**Interfaces:**
- Consumes: `processed_event(event_id, event_type, processed_at)`.
- Produces: `InboxEventGuard.claim(UUID eventId, String eventType): boolean`.

- [ ] **Step 1: inbox 실패 테스트 작성**

```java
@Test
void claimsEachEventOnlyOnce() {
    var fixture = fixture();
    assertThat(fixture.guard.claim(EVENT_ID, "TASK_CHANGED")).isTrue();
    assertThat(fixture.guard.claim(EVENT_ID, "TASK_CHANGED")).isFalse();
    assertThat(fixture.jdbc.queryForObject(
            "select count(*) from processed_event", Integer.class)).isEqualTo(1);
}

@Test
void rollbackAllowsRedelivery() {
    var fixture = fixture();
    assertThatThrownBy(() -> fixture.transaction.executeWithoutResult(status -> {
        assertThat(fixture.guard.claim(EVENT_ID, "TASK_CHANGED")).isTrue();
        fixture.jdbc.update("update local_counter set value = value + 1");
        throw new IllegalStateException("rollback-probe");
    })).isInstanceOf(IllegalStateException.class);

    assertThat(fixture.guard.claim(EVENT_ID, "TASK_CHANGED")).isTrue();
    assertThat(fixture.jdbc.queryForObject(
            "select value from local_counter", Integer.class)).isZero();
}
```

fixture는 `processed_event`와 값 0의 `local_counter`를 실제 PostgreSQL에 만든다.

- [ ] **Step 2: 실패 확인**

Run: `gradlew.bat :libs:messaging-support:test --tests '*InboxEventGuardIntegrationTest' --no-daemon --no-configuration-cache`

Expected: FAIL, inbox guard가 없다.

- [ ] **Step 3: guard 구현**

```java
public interface InboxEventGuard {
    boolean claim(UUID eventId, String eventType);
}
```

`JdbcInboxEventGuard`는 null UUID와 blank event type을 거부하고 다음 update count가 1인지 반환한다.

```sql
insert into processed_event(event_id, event_type)
values (?, ?)
on conflict (event_id) do nothing
```

세 서비스 build에 messaging-support dependency가 존재하도록 notification에도 추가한다.

- [ ] **Step 4: inbox test 통과**

Run: `gradlew.bat :libs:messaging-support:test --tests '*InboxEventGuardIntegrationTest' --no-daemon --no-configuration-cache`

Expected: PASS, duplicate는 false이고 rollback 뒤 redelivery는 true다.

- [ ] **Step 5: 커밋**

```bash
git add libs/messaging-support services/notification-service/build.gradle.kts
git commit -m "feat(messaging): transaction inbox 중복 차단 추가"
```

---

### Task 4: Consumer retry와 dead-letter

**Files:**
- Modify: `libs/messaging-support/build.gradle.kts`
- Create: `libs/messaging-support/src/main/java/io/todorok/messaging/ConsumerFailureHandlerFactory.java`
- Create: `libs/messaging-support/src/main/java/io/todorok/messaging/DefaultConsumerFailureHandlerFactory.java`
- Create: `libs/messaging-support/src/test/java/io/todorok/messaging/ConsumerFailureHandlerIntegrationTest.java`
- Create: `libs/messaging-support/src/test/java/io/todorok/messaging/IdempotentConsumerIntegrationTest.java`

**Interfaces:**
- Consumes: `KafkaOperations<Object, Object>`, dead-letter topic.
- Produces: `CommonErrorHandler` with 1초 간격 3회 retry와 dead-letter recoverer.

- [ ] **Step 1: Kafka 실패 테스트 작성**

Testcontainers `KafkaContainer("apache/kafka-native:4.3.1")`에 `source-topic`과 `todorok.dead-letter`를 만든다. Spring `ConcurrentMessageListenerContainer` listener는 항상 예외를 던지고 attempt counter를 증가시킨다.

```java
@Test
void retriesThreeTimesThenPublishesDeadLetter() {
    template.send("source-topic", "aggregate-1", "broken-event").join();
    var deadLetter = poll("todorok.dead-letter", Duration.ofSeconds(15));

    assertThat(attempts).hasValue(4);
    assertThat(deadLetter.key()).isEqualTo("aggregate-1");
    assertThat(deadLetter.value()).isEqualTo("broken-event");
    assertThat(lastHeader(deadLetter, KafkaHeaders.DLT_ORIGINAL_TOPIC))
            .isEqualTo("source-topic".getBytes(UTF_8));
    assertThat(lastHeader(deadLetter, KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN))
            .isEqualTo(IllegalStateException.class.getName().getBytes(UTF_8));
}
```

- [ ] **Step 2: 실패 확인**

Run: `gradlew.bat :libs:messaging-support:test --tests '*ConsumerFailureHandlerIntegrationTest' --no-daemon --no-configuration-cache`

Expected: FAIL, factory와 Kafka test dependency가 없다.

- [ ] **Step 3: dependency와 factory 구현**

```kotlin
implementation("org.springframework.kafka:spring-kafka")
testImplementation("org.testcontainers:testcontainers-kafka")
```

```java
public interface ConsumerFailureHandlerFactory {
    CommonErrorHandler create(
            KafkaOperations<Object, Object> operations,
            String deadLetterTopic);
}
```

implementation은 `DeadLetterPublishingRecoverer` destination을 항상 전달받은 topic의 같은 partition으로 정하고 `new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L))`을 반환한다. recoverer logging에는 record value를 포함하지 않는다.

- [ ] **Step 4: Kafka test 통과**

Run: `gradlew.bat :libs:messaging-support:test --tests '*ConsumerFailureHandlerIntegrationTest' --no-daemon --no-configuration-cache`

Expected: PASS, attempt 4회 뒤 dead-letter 한 건과 원본 header가 확인된다.

- [ ] **Step 5: 실제 consumer 중복·재시작 test 작성**

KafkaContainer와 PostgreSQLContainer를 함께 사용한다. listener는 `@Transactional` method에서 `InboxEventGuard.claim`이 true일 때만 `local_counter`를 1 증가시킨다. 같은 event ID envelope를 다른 Kafka offset으로 두 번 보낸 뒤 counter가 1인지 확인하고, listener container를 stop/start한 뒤 같은 envelope를 다시 보내도 counter가 1인지 검증한다.

```java
assertThat(awaitCounterValue(Duration.ofSeconds(15))).isEqualTo(1);
listenerContainer.stop();
listenerContainer.start();
template.send("source-topic", EVENT_ID.toString(), envelopeJson()).join();
assertThat(awaitProcessedRecords(3, Duration.ofSeconds(15))).isEqualTo(3);
assertThat(jdbc.queryForObject(
        "select value from local_counter", Integer.class)).isEqualTo(1);
```

- [ ] **Step 6: consumer 중복 test 통과**

Run: `gradlew.bat :libs:messaging-support:test --tests '*IdempotentConsumerIntegrationTest' --no-daemon --no-configuration-cache`

Expected: PASS, 서로 다른 offset과 consumer 재시작 뒤에도 local counter는 1이다.

- [ ] **Step 7: 커밋**

```bash
git add libs/messaging-support
git commit -m "feat(messaging): consumer 재시도와 dead-letter 기반 추가"
```

---

### Task 5: PostgreSQL replication role·publication·slot

**Files:**
- Modify: `.env.example`
- Modify: `infra/docker/postgres/init/001-create-service-roles.sh`
- Create: `infra/docker/postgres/replication/initialize-outbox-replication.sh`
- Create: `infra/docker/postgres/maintenance/prune-messaging.sql`
- Create: `libs/messaging-support/src/test/java/io/todorok/messaging/MessagingRetentionIntegrationTest.java`
- Create: `scripts/postgres-messaging-config.test.mjs`
- Modify: `infra/docker/compose.yml`

**Interfaces:**
- Consumes: migrated planner·activity outbox table.
- Produces: `debezium_app`, `todorok_outbox` publication, `todorok_outbox_slot`, safe prune SQL.

- [ ] **Step 1: PostgreSQL config 실패 테스트 작성**

`scripts/postgres-messaging-config.test.mjs`는 `docker compose ... config --format json`을 실행하고 다음 literal을 검증한다.

```js
assert.deepEqual(services.postgres.command, [
  'postgres',
  '-c', 'wal_level=logical',
  '-c', 'max_wal_senders=1',
  '-c', 'max_replication_slots=1',
  '-c', 'max_slot_wal_keep_size=2048MB',
])
assert.equal(services['replication-init'].restart, 'no')
assert.equal(
  services['replication-init'].depends_on['activity-migration'].condition,
  'service_completed_successfully',
)
```

- [ ] **Step 2: 실패 확인**

Run: `node --test scripts/postgres-messaging-config.test.mjs`

Expected: FAIL, postgres command와 replication-init이 없다.

- [ ] **Step 3: role 초기화와 환경 변수 작성**

`.env.example`에 `DEBEZIUM_DB_PASSWORD=replace-with-debezium-password`를 추가한다. role script는 secret을 psql variable로 받고 다음을 실행한다.

```sql
create role debezium_app login replication password :'debezium_password';
grant connect, create on database todorok to debezium_app;
grant usage on schema planner, activity to debezium_app;
alter default privileges for role planner_app in schema planner
    grant select on tables to debezium_app;
alter default privileges for role activity_app in schema activity
    grant select on tables to debezium_app;
```

planner·activity V2 마지막에는 role이 존재할 때 자기 `outbox_event` SELECT를 grant하는 idempotent DO block을 추가한다.

- [ ] **Step 4: publication·slot script 작성**

script는 관리자 계정으로 publication table 목록과 slot plugin을 조회한다. resource가 없으면 생성하고, 이름은 같지만 table 집합이나 plugin이 다르면 stderr와 exit 1로 종료한다.

```sql
create publication todorok_outbox
for table planner.outbox_event, activity.outbox_event;

select * from pg_create_logical_replication_slot(
    'todorok_outbox_slot', 'pgoutput'
);
```

- [ ] **Step 5: 보존 SQL 작성**

DO block에서 slot 존재·active·confirmed_flush_lsn non-null·WAL lag 16MB 이하를 확인한다. 조건 미달이면 exception을 던지고, 통과하면 planner·activity 7일 outbox와 세 schema 30일 inbox를 삭제한다.

`MessagingRetentionIntegrationTest`는 PostgreSQLContainer에 slot과 여섯 table을 만들고 7일 경계 전후 outbox, 30일 경계 전후 inbox를 삽입한다. slot active 조건을 충족한 connection에서 SQL을 실행한 뒤 오래된 row만 삭제됐는지 검증한다. slot이 없거나 inactive인 case는 SQL exception과 삭제 0건을 검증한다.

- [ ] **Step 6: Compose 연결과 test 통과**

postgres service에 네 command flag를 추가하고, `replication-init`은 postgres image와 replication script를 사용하며 planner·activity migration 완료를 기다린다.

Run: `node --test scripts/postgres-messaging-config.test.mjs`

Run: `gradlew.bat :libs:messaging-support:test --tests '*MessagingRetentionIntegrationTest' --no-daemon --no-configuration-cache`

Run: `docker compose --env-file .env.example -f infra/docker/compose.yml config --quiet`

Expected: PASS.

- [ ] **Step 7: 커밋**

```bash
git add .env.example services/*/src/main/resources/db/migration/V2__add_messaging_tables.sql infra/docker/postgres infra/docker/compose.yml scripts/postgres-messaging-config.test.mjs
git commit -m "feat(messaging): PostgreSQL replication 기반 추가"
```

---

### Task 6: Kafka topic과 Debezium Connect 구성

**Files:**
- Create: `infra/docker/kafka/initialize-topics.sh`
- Create: `infra/docker/connect/Dockerfile`
- Create: `infra/docker/connect/connector-template.json`
- Create: `infra/docker/connect/register-connector.sh`
- Create: `scripts/connect-config.test.mjs`
- Modify: `infra/docker/compose.yml`

**Interfaces:**
- Consumes: Kafka broker, publication·slot, `DEBEZIUM_DB_PASSWORD`.
- Produces: domain·dead-letter topic, Connect worker, `todorok-postgres-outbox` connector.

- [ ] **Step 1: Connect 구성 실패 테스트 작성**

```js
test('Connect와 topic 보존 정책이 고정된다', () => {
  const services = composeServices()
  assert.equal(services.connect.image, 'quay.io/debezium/connect:3.6.2.Final')
  assert.equal(services.connect.mem_limit, '805306368')
  assert.match(services.connect.environment.KAFKA_HEAP_OPTS, /-Xmx512m/)
  assert.equal(services['connect-init'].depends_on.connect.condition, 'service_healthy')
  assert.equal(services['kafka-init'].depends_on.kafka.condition, 'service_healthy')
})
```

같은 test가 connector template을 JSON parse한 뒤 `slot.name`, `publication.name`, `table.include.list`, `snapshot.mode`, Outbox Event Router, route replacement, fatal update, source fail-fast 값을 literal로 검증한다.

- [ ] **Step 2: 실패 확인**

Run: `node --test scripts/connect-config.test.mjs`

Expected: FAIL, connect·init service와 template이 없다.

- [ ] **Step 3: Kafka topic init 작성**

script는 task·activity topic을 partitions 3, retention.ms 604800000, retention.bytes 1073741824로 만든다. dead-letter는 partitions 3, retention.ms 2592000000, retention.bytes 1073741824다. Connect internal topic 3개는 partition 1, cleanup.policy compact다. 기존 topic에는 네 명시 설정만 `kafka-configs.sh --alter`한다.

- [ ] **Step 4: connector template 작성**

template은 spec의 connector property를 모두 포함하고 DB host `postgres`, port `5432`, database `${POSTGRES_DB}`, user `debezium_app`, topic prefix `todorok-cdc`를 사용한다. password value는 `${DEBEZIUM_DB_PASSWORD}` token으로 남긴다.

- [ ] **Step 5: register image와 drift 검사 작성**

init image는 Alpine에 `bash`, `curl`, `jq`, `gettext`를 설치하고 template·script를 복사한다. script는 `envsubst '${POSTGRES_DB} ${DEBEZIUM_DB_PASSWORD}'`로 두 secret source만 치환해 `${routedByValue}` route token을 보존한다. `/connectors/todorok-postgres-outbox/config`을 조회해 404면 POST 생성, 200이면 secret을 제외한 중요 config를 정렬 비교해 같을 때 0, 다를 때 1로 종료한다.

- [ ] **Step 6: Compose Connect 연결**

Connect worker environment는 broker `kafka:19092`, internal topic 3개, replication factor 1, JSON converter schema false를 사용한다. health check는 `curl --fail http://localhost:8083/connectors`다. `connect`는 kafka-init과 replication-init 성공을 기다리고, `connect-init`은 connect health를 기다린다. application 서비스는 connect-init을 기다리지 않고 kafka-init만 기다린다.

- [ ] **Step 7: 구성 test 통과**

Run: `node --test scripts/connect-config.test.mjs scripts/postgres-messaging-config.test.mjs scripts/persistence-compose.test.mjs`

Run: `docker compose --env-file .env.example -f infra/docker/compose.yml config --quiet`

Expected: PASS, 기존 persistence migration gate도 유지된다.

- [ ] **Step 8: 커밋**

```bash
git add infra/docker/kafka infra/docker/connect infra/docker/compose.yml scripts/connect-config.test.mjs
git commit -m "feat(messaging): Kafka topic과 Debezium connector 구성"
```

---

### Task 7: 실제 CDC 장애·복구 통합 테스트

**Files:**
- Modify: `settings.gradle.kts`
- Create: `tests/messaging-integration/build.gradle.kts`
- Create: `tests/messaging-integration/src/test/resources/messaging-integration.compose.yml`
- Create: `tests/messaging-integration/src/test/resources/postgres/init.sql`
- Create: `tests/messaging-integration/src/test/java/io/todorok/messaging/integration/MessagingComposeFixture.java`
- Create: `tests/messaging-integration/src/test/java/io/todorok/messaging/integration/DebeziumOutboxRoundTripTest.java`
- Create: `tests/messaging-integration/src/test/java/io/todorok/messaging/integration/DebeziumRecoveryTest.java`

**Interfaces:**
- Consumes: production topic·replication·connector scripts와 Debezium image.
- Produces: `messagingIntegrationTest` Gradle gate.

- [ ] **Step 1: planner·activity 왕복 실패 테스트 작성**

`MessagingComposeFixture`는 `ComposeContainer`로 test compose를 시작하고 postgres·kafka·connect container state를 제공한다. `insertOutbox(schema, eventId, aggregateType, aggregateId, eventType, payload)`는 postgres의 psql을 실행한다. `consume(topic, expectedEventId, timeout)`은 kafka console consumer를 조건 polling해 전체 envelope JSON을 반환한다.

```java
@Test
void routesPlannerAndActivityEnvelopes() {
    fixture.insertOutbox("planner", TASK_EVENT_ID, "task", TASK_ID,
            "TASK_CHANGED", taskEnvelopeJson());
    fixture.insertOutbox("activity", ACTIVITY_EVENT_ID, "activity", ACTIVITY_ID,
            "ACTIVITY_COMPLETED", activityEnvelopeJson());

    assertThat(fixture.consume("todorok.task.v1", TASK_EVENT_ID, TEN_SECONDS))
            .containsEntry("eventId", TASK_EVENT_ID.toString());
    assertThat(fixture.consume("todorok.activity.v1", ACTIVITY_EVENT_ID, TEN_SECONDS))
            .containsEntry("eventId", ACTIVITY_EVENT_ID.toString());
}
```

- [ ] **Step 2: 실패 확인**

Run: `gradlew.bat :tests:messaging-integration:test --tests '*DebeziumOutboxRoundTripTest' --no-daemon --no-configuration-cache`

Expected: FAIL, integration module과 fixture가 없다.

- [ ] **Step 3: integration module과 isolated Compose 작성**

module은 JUnit Jupiter, AssertJ, Testcontainers core·compose, Jackson 3을 사용한다. test compose는 host volume을 만들지 않고 tmpfs PostgreSQL, Kafka KRaft, production init scripts, Debezium Connect, init service를 같은 network에서 실행한다. PostgreSQL outbox fixture는 production V2와 동일한 DDL을 사용한다.

- [ ] **Step 4: fixture 구현**

모든 wait는 250ms 간격 polling과 deadline을 사용한다. 고정 sleep을 두지 않는다. command exit code가 non-zero이면 stdout·stderr를 포함한 assertion error를 던진다. `close()`는 ComposeContainer와 network를 항상 종료한다.

- [ ] **Step 5: 왕복 test 통과**

Run: `gradlew.bat :tests:messaging-integration:test --tests '*DebeziumOutboxRoundTripTest' --no-daemon --no-configuration-cache`

Expected: PASS, 두 topic value가 전체 envelope JSON이고 key가 aggregate ID다.

같은 aggregate ID로 occurredAt이 증가하는 event 두 개를 INSERT하고 consumer 결과의 event ID 순서가 insert 순서와 같은지 검증한다. `service_metadata`를 UPDATE한 뒤 domain topic consumer가 새 record를 받지 않는 것도 검증한다.

- [ ] **Step 6: Connect·Kafka 복구 실패 테스트 작성**

```java
@Test
void publishesRowsCreatedWhileConnectIsStopped() {
    fixture.stop("connect");
    fixture.insertOutbox("planner", EVENT_ID, "task", TASK_ID,
            "TASK_CHANGED", taskEnvelopeJson());
    fixture.start("connect");
    fixture.awaitConnectorRunning();
    assertThat(fixture.consume("todorok.task.v1", EVENT_ID, THIRTY_SECONDS))
            .containsEntry("eventId", EVENT_ID.toString());
}

@Test
void recoversAfterKafkaRestart() {
    fixture.stop("kafka");
    fixture.insertOutbox("activity", EVENT_ID, "activity", ACTIVITY_ID,
            "ACTIVITY_COMPLETED", activityEnvelopeJson());
    fixture.start("kafka");
    fixture.awaitKafkaReady();
    fixture.awaitConnectorRunning();
    assertThat(fixture.consume("todorok.activity.v1", EVENT_ID, THIRTY_SECONDS))
            .containsEntry("eventId", EVENT_ID.toString());
}
```

- [ ] **Step 7: slot 유실 복구 test 작성**

Connect를 중단하고 slot을 삭제한 뒤 replication init script로 같은 이름의 `pgoutput` slot을 재생성한다. Connect를 시작하면 `snapshot.mode=when_needed`로 retained outbox를 다시 읽을 수 있으며, 수신 envelope의 event ID가 원본과 같은지 검증한다. 중복 수신 개수는 1 이상으로만 검사하고 exactly-once를 요구하지 않는다.

- [ ] **Step 8: 장애·복구 test 통과**

Run: `gradlew.bat :tests:messaging-integration:test --no-daemon --no-configuration-cache --max-workers=1`

Expected: PASS, Connect·Kafka·slot 복구 뒤 event 누락이 없다.

- [ ] **Step 9: 커밋**

```bash
git add settings.gradle.kts tests/messaging-integration
git commit -m "test(messaging): Debezium CDC 장애와 복구 검증 추가"
```

---

### Task 8: 상태 점검·보존·전체 CI gate

**Files:**
- Create: `scripts/messaging-health.mjs`
- Create: `scripts/messaging-health.test.mjs`
- Modify: `scripts/verify-all.mjs`
- Modify: `README.md`
- Modify: `docs/plans/2026-09-01-mvp-implementation-plan.md`
- Modify: `docs/ISSUE_ROADMAP.md`

**Interfaces:**
- Consumes: Connect REST, PostgreSQL slot query, Kafka CLI JSON/text output.
- Produces: messaging health JSON, exit code 1 critical gate, #20 완료 근거.

- [ ] **Step 1: health 판정 실패 테스트 작성**

```js
test('WAL 2GB 또는 중단 connector를 critical로 판정한다', () => {
  assert.deepEqual(evaluateMessagingHealth({
    connectorState: 'FAILED',
    taskStates: ['RUNNING'],
    slotActive: false,
    retainedWalBytes: 2147483648,
    topicPoliciesValid: true,
    consumerLag: 0,
  }), {
    status: 'critical',
    reasons: ['connector_not_running', 'slot_inactive', 'wal_limit_reached'],
  })
})

test('WAL 1.5GB부터 warning이다', () => {
  assert.equal(evaluateMessagingHealth(healthyState({
    retainedWalBytes: 1610612736,
  })).status, 'warning')
})
```

- [ ] **Step 2: 실패 확인**

Run: `node --test scripts/messaging-health.test.mjs`

Expected: FAIL, health module이 없다.

- [ ] **Step 3: pure 판정과 CLI adapter 구현**

`evaluateMessagingHealth`는 connector·task RUNNING, slot active, WAL 1.5GB warning·2GB critical, topic policy, consumer lag를 판정한다. CLI main은 Connect REST, `docker compose exec -T postgres psql`, Kafka consumer-groups/configs command를 실행해 상태를 모으고 JSON을 stdout에 출력한다. critical만 exit 1, warning은 exit 0이다.

- [ ] **Step 4: health test 통과**

Run: `node --test scripts/messaging-health.test.mjs`

Expected: PASS.

- [ ] **Step 5: 전체 검증 연결**

`scripts/verify-all.mjs`에 다음 순서를 추가한다.

```text
Node messaging config unit tests
messaging-support PostgreSQL·Kafka tests
messaging-integration CDC tests
기존 Java·TypeScript·web·Compose·기록 정책
```

실행 중 Compose가 없는 상태에서도 pure health test는 실행하지만 live `messaging-health.mjs`는 배포 smoke 명령으로 README에 기록하고 일반 CI에서는 호출하지 않는다.

- [ ] **Step 6: 문서 상태 갱신**

README에 connector 등록, health, prune 명령과 장애 복구 순서를 기록한다. MVP 계획 T4를 완료 체크하고 roadmap #20 행에 설계·구현 계획 링크를 추가한다.

- [ ] **Step 7: 전체 검증**

Run: `node scripts/verify-all.mjs`

Expected: 계약 drift, persistence, messaging-support, CDC 장애·복구, TypeScript, web, Compose, 기록 정책이 PASS.

- [ ] **Step 8: 정책 검사와 커밋**

Run: `git diff --check`

Run: 저장소 변경 파일과 `develop..HEAD` 커밋 기록의 금지 문자열 검사.

Expected: whitespace 오류와 금지 문자열 0개.

```bash
git add scripts README.md docs/plans/2026-09-01-mvp-implementation-plan.md docs/ISSUE_ROADMAP.md
git commit -m "ci(messaging): CDC 검증과 운영 점검 연결"
```

---

## 최종 수용 기준 대조

```text
DB commit 이후 Kafka 발행
  → OutboxEventWriterIntegrationTest + DebeziumOutboxRoundTripTest

Connect·Kafka 재시작 후 누락 없음
  → DebeziumRecoveryTest

중복 전달에도 local 결과 한 번
  → InboxEventGuardIntegrationTest + consumer duplicate case

consumer 실패 dead-letter
  → ConsumerFailureHandlerIntegrationTest

WAL·topic·outbox·inbox 상한
  → postgres config test + connect config test + prune SQL test + health test

slot 유실 복구
  → DebeziumRecoveryTest의 when_needed snapshot case
```
