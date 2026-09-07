package io.todorok.messaging.integration;

import static org.assertj.core.api.Assertions.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import io.todorok.activity.ActivityApplication;
import io.todorok.planner.PlannerApplication;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;

/** Actual HTTP -> service DB -> Debezium -> Kafka -> service listener -> ack -> HTTP. */
class ActivityServiceRoundTripTest {

    static MessagingInfrastructureFixture infra;
    static ConfigurableApplicationContext planner, activity;
    static final JsonMapper JSON = JsonMapper.builder()
        .findAndAddModules()
        .build();
    static final java.security.KeyPair KEYS = keys();
    static final HttpClient HTTP = HttpClient.newHttpClient();

    static java.security.KeyPair keys() {
        try {
            var g = java.security.KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            return g.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeAll
    static void start() {
        infra = MessagingInfrastructureFixture.start();
        for (String schema : List.of("planner", "activity"))
            Flyway.configure()
                .dataSource(infra.jdbcUrl(schema), "postgres", "postgres")
                .schemas(schema)
                .defaultSchema(schema)
                .baselineOnMigrate(true)
                .baselineVersion("2")
                .locations(
                    "filesystem:" +
                        Path.of(
                            System.getProperty("todorok.repository.root"),
                            "services",
                            schema + "-service",
                            "src/main/resources/db/migration"
                        )
                            .toString()
                            .replace('\\', '/')
                )
                .load()
                .migrate();
        planner = app(PlannerApplication.class, "planner");
        activity = app(ActivityApplication.class, "activity");
    }

    static ConfigurableApplicationContext app(Class<?> main, String schema) {
        return new SpringApplicationBuilder(main).run(
            "--server.port=0",
            "--server.servlet.context-path=/api/" + schema + "/v1",
            "--todorok.auth.allowed-origins=https://todorok.test",
            "--spring.datasource.url=" + infra.jdbcUrl(schema),
            "--spring.datasource.username=postgres",
            "--spring.datasource.password=postgres",
            "--spring.flyway.enabled=false",
            "--spring.jpa.properties.hibernate.default_schema=" + schema,
            "--spring.jpa.hibernate.ddl-auto=validate",
            "--spring.kafka.bootstrap-servers=" + infra.bootstrapServers(),
            "--todorok.messaging.enabled=true",
            "--spring.kafka.consumer.auto-offset-reset=earliest",
            "--spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
            "--spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
            "--spring.jackson.deserialization.fail-on-unknown-properties=true",
            "--spring.jackson.deserialization.accept-float-as-int=false",
            "--spring.jackson.mapper.allow-coercion-of-scalars=false",
            "--todorok.auth.public-key=" +
                Base64.getEncoder().encodeToString(
                    KEYS.getPublic().getEncoded()
                ),
            "--todorok.auth.private-key=" +
                Base64.getEncoder().encodeToString(
                    KEYS.getPrivate().getEncoded()
                )
        );
    }

    @AfterAll
    static void stop() {
        if (activity != null) activity.close();
        if (planner != null) planner.close();
        if (infra != null) infra.close();
    }

    static HttpResponse<String> call(
        String service,
        UUID owner,
        String method,
        String path,
        String body
    ) throws Exception {
        var ctx = service.equals("activity") ? activity : planner;
        var jwt = new SignedJWT(
            new JWSHeader(JWSAlgorithm.RS256),
            new JWTClaimsSet.Builder()
                .issuer("todorok")
                .audience("todorok-api")
                .subject(owner.toString())
                .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                .build()
        );
        jwt.sign(new RSASSASigner(KEYS.getPrivate()));
        var request = HttpRequest.newBuilder(
            URI.create(
                "http://localhost:" +
                    ctx.getEnvironment().getProperty("local.server.port") +
                    "/api/" +
                    service +
                    "/v1" +
                    path
            )
        )
            .header("Authorization", "Bearer " + jwt.serialize())
            .header("Content-Type", "application/json")
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body)
            )
            .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    static JsonNode ok(HttpResponse<String> response, int status) {
        assertThat(response.statusCode())
            .withFailMessage(response.body())
            .isEqualTo(status);
        return JSON.readTree(response.body());
    }

    static String task(UUID owner, String type) throws Exception {
        String id = ok(
            call(
                "planner",
                owner,
                "POST",
                "/tasks",
                JSON.writeValueAsString(
                    Map.of(
                        "title",
                        "기록",
                        "taskType",
                        type,
                        "scheduledDate",
                        "2026-09-07"
                    )
                )
            ),
            201
        )
            .get("taskId")
            .asText();
        await(
            () ->
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from activity.task_reference where task_id=?",
                        Integer.class,
                        UUID.fromString(id)
                    ) == 1
        );
        return id;
    }

    static String request(
        String task,
        String type,
        String status,
        UUID command,
        String detail
    ) {
        return (
            "{\"taskId\":\"" +
            task +
            "\",\"commandId\":\"" +
            command +
            "\",\"activityType\":\"" +
            type +
            "\",\"completionStatus\":\"" +
            status +
            "\",\"performedAt\":\"2026-09-08T01:00:00+09:00\",\"note\":\"완료 메모\",\"detail\":" +
            detail +
            "}"
        );
    }

    static void await(java.util.function.BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("Service synchronization timed out");
    }

    static void applied(UUID owner, String id) {
        await(() -> {
            try {
                return JSON.readTree(
                    call(
                        "activity",
                        owner,
                        "GET",
                        "/activities/" + id,
                        null
                    ).body()
                )
                    .path("syncState")
                    .asText()
                    .equals("APPLIED");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    static void send(String topic, String key, String json) throws Exception {
        try (
            var producer = new KafkaProducer<String, String>(
                Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    infra.bootstrapServers(),
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    StringSerializer.class,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                    StringSerializer.class
                )
            )
        ) {
            producer
                .send(new ProducerRecord<>(topic, key, json))
                .get(15, TimeUnit.SECONDS);
        }
    }

    @Test
    void typedRoundTripReplaysAndActualInterval() throws Exception {
        for (var entry : Map.of(
            "WORKOUT",
            "{\"workout\":{\"sets\":[{\"exercise\":\"스쿼트\",\"reps\":5,\"weightKg\":60}]}}",
            "STUDY",
            "{\"study\":{\"subject\":\"Java\",\"durationMinutes\":30,\"values\":{\"pages\":10},\"snapshot\":{\"label\":\"독서\"}}}",
            "CLIMBING",
            "{\"climbing\":{\"durationSeconds\":1800,\"rounds\":[{\"grade\":\"V3\",\"attempts\":2,\"completed\":true}]}}"
        ).entrySet()) {
            UUID owner = UUID.randomUUID();
            String task = task(owner, entry.getKey());
            UUID command = UUID.randomUUID();
            String body = request(
                task,
                entry.getKey(),
                "COMPLETED",
                command,
                entry.getValue()
            );
            body =
                body.substring(0, body.length() - 1) +
                ",\"startedAt\":\"2026-09-08T01:00:00+09:00\",\"endedAt\":\"2026-09-08T01:30:00+09:00\"}";
            var saved = ok(
                call("activity", owner, "POST", "/activities", body),
                201
            );
            String id = saved.get("activityId").asText();
            applied(owner, id);
            var done = ok(
                call("planner", owner, "GET", "/tasks/" + task, null),
                200
            );
            assertThat(done.get("status").asText()).isEqualTo("COMPLETED");
            assertThat(done.get("scheduledDate").asText()).isEqualTo(
                "2026-09-08"
            );
            assertThat(done.get("activityId").asText()).isEqualTo(id);
            assertThat(done.get("startedAt").isNull()).isFalse();
            assertThat(
                ok(
                    call("activity", owner, "GET", "/activities/" + id, null),
                    200
                )
                    .get("note")
                    .asText()
            ).isEqualTo("완료 메모");
            var persisted = ok(
                call("activity", owner, "GET", "/activities/" + id, null),
                200
            ).get("detail");
            switch (entry.getKey()) {
                case "WORKOUT" -> {
                    var set = persisted.get("workout").get("sets").get(0);
                    assertThat(set.get("exercise").asText()).isEqualTo(
                        "스쿼트"
                    );
                    assertThat(set.get("reps").asInt()).isEqualTo(5);
                    assertThat(
                        set.get("weightKg").decimalValue()
                    ).isEqualByComparingTo("60");
                }
                case "STUDY" -> {
                    var study = persisted.get("study");
                    assertThat(study.get("durationMinutes").asInt()).isEqualTo(
                        30
                    );
                    assertThat(
                        study.get("values").get("pages").asInt()
                    ).isEqualTo(10);
                    assertThat(
                        study.get("snapshot").get("label").asText()
                    ).isEqualTo("독서");
                }
                case "CLIMBING" -> {
                    var climbing = persisted.get("climbing");
                    assertThat(
                        climbing.get("durationSeconds").asInt()
                    ).isEqualTo(1800);
                    assertThat(
                        climbing.get("rounds").get(0).get("attempts").asInt()
                    ).isEqualTo(2);
                    assertThat(
                        climbing
                            .get("rounds")
                            .get(0)
                            .get("completed")
                            .asBoolean()
                    ).isTrue();
                }
                default -> throw new AssertionError("Unknown test type");
            }
            assertThat(
                ok(call("activity", owner, "POST", "/activities", body), 201)
                    .get("activityId")
                    .asText()
            ).isEqualTo(id);
            ok(
                call(
                    "activity",
                    owner,
                    "POST",
                    "/activities",
                    body.replace("완료 메모", "변경")
                ),
                409
            );
            ok(
                call(
                    "activity",
                    UUID.randomUUID(),
                    "GET",
                    "/activities/" + id,
                    null
                ),
                404
            );
            ok(
                call(
                    "planner",
                    owner,
                    "DELETE",
                    "/tasks/" +
                        task +
                        "?version=" +
                        done.get("version").asLong(),
                    null
                ),
                204
            );
            await(() ->
                infra
                    .database()
                    .queryForObject(
                        "select status from activity.task_reference where task_id=?",
                        String.class,
                        UUID.fromString(task)
                    )
                    .equals("DELETED")
            );
            assertThat(
                ok(call("activity", owner, "POST", "/activities", body), 201)
                    .get("activityId")
                    .asText()
            ).isEqualTo(id);
        }
    }

    @Test
    void concurrentCommandsPartialAndAtomicRollback() throws Exception {
        UUID owner = UUID.randomUUID();
        String task = task(owner, "WORKOUT");
        String body = request(
            task,
            "WORKOUT",
            "COMPLETED",
            UUID.randomUUID(),
            "{}"
        );
        var container = planner
            .getBean(KafkaListenerEndpointRegistry.class)
            .getListenerContainer("planner-activity");
        container.stop();
        try {
            var start = new CountDownLatch(1);
            try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
                var futures = new ArrayList<Future<HttpResponse<String>>>();
                for (int i = 0; i < 8; i++) futures.add(
                    pool.submit(() -> {
                        start.await();
                        return call(
                            "activity",
                            owner,
                            "POST",
                            "/activities",
                            body
                        );
                    })
                );
                start.countDown();
                var ids = new HashSet<String>();
                for (var future : futures)
                    ids.add(ok(future.get(), 201).get("activityId").asText());
                assertThat(ids).hasSize(1);
            }
            ok(
                call(
                    "activity",
                    owner,
                    "POST",
                    "/activities",
                    request(
                        task,
                        "WORKOUT",
                        "COMPLETED",
                        UUID.randomUUID(),
                        "{}"
                    )
                ),
                409
            );
            String partialTask = task(owner, "STUDY");
            var partial = ok(
                call(
                    "activity",
                    owner,
                    "POST",
                    "/activities",
                    request(
                        partialTask,
                        "STUDY",
                        "PARTIAL",
                        UUID.randomUUID(),
                        "{}"
                    )
                ),
                201
            );
            assertThat(partial.get("syncState").asText()).isEqualTo(
                "NOT_REQUIRED"
            );
            assertThat(
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from activity.outbox_event where aggregateid=?",
                        Integer.class,
                        partial.get("activityId").asText()
                    )
            ).isZero();
            assertThat(
                ok(
                    call(
                        "planner",
                        owner,
                        "GET",
                        "/tasks/" + partialTask,
                        null
                    ),
                    200
                )
                    .get("status")
                    .asText()
            ).isEqualTo("PLANNED");
            String rollbackTask = task(owner, "CLIMBING");
            infra
                .database()
                .execute(
                    "alter table activity.outbox_event add constraint fail_test check(payload->>'userId'<>'" +
                        owner +
                        "') not valid"
                );
            try {
                ok(
                    call(
                        "activity",
                        owner,
                        "POST",
                        "/activities",
                        request(
                            rollbackTask,
                            "CLIMBING",
                            "COMPLETED",
                            UUID.randomUUID(),
                            "{\"climbing\":{\"rounds\":[{\"attempts\":1}]}}"
                        )
                    ),
                    500
                );
            } finally {
                infra
                    .database()
                    .execute(
                        "alter table activity.outbox_event drop constraint fail_test"
                    );
            }
            assertThat(
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from activity.activity_record where task_id=?",
                        Integer.class,
                        UUID.fromString(rollbackTask)
                    )
            ).isZero();
            assertThat(
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from activity.climbing_round",
                        Integer.class
                    )
            ).isLessThanOrEqualTo(1);
        } finally {
            container.start();
        }
    }

    @Test
    void stoppedConsumerRestartDeletionAndSkipConflictsRemainReadable()
        throws Exception {
        for (String command : List.of("delete", "skip")) {
            UUID owner = UUID.randomUUID();
            String task = task(owner, "STUDY");
            var listener = planner
                .getBean(KafkaListenerEndpointRegistry.class)
                .getListenerContainer("planner-activity");
            listener.stop();
            String id;
            try {
                var saved = ok(
                    call(
                        "activity",
                        owner,
                        "POST",
                        "/activities",
                        request(
                            task,
                            "STUDY",
                            "COMPLETED",
                            UUID.randomUUID(),
                            "{}"
                        )
                    ),
                    201
                );
                id = saved.get("activityId").asText();
                assertThat(saved.get("syncState").asText()).isEqualTo(
                    "PENDING"
                );
                if (command.equals("delete")) ok(
                    call(
                        "planner",
                        owner,
                        "DELETE",
                        "/tasks/" + task + "?version=0",
                        null
                    ),
                    204
                );
                else ok(
                    call(
                        "planner",
                        owner,
                        "POST",
                        "/tasks/" + task + "/skip",
                        "{\"version\":0}"
                    ),
                    200
                );
                assertThat(
                    ok(
                        call(
                            "activity",
                            owner,
                            "GET",
                            "/activities/" + id,
                            null
                        ),
                        200
                    )
                        .get("syncState")
                        .asText()
                ).isEqualTo("PENDING");
            } finally {
                listener.start();
            }
            await(() ->
                infra
                    .database()
                    .queryForObject(
                        "select sync_state from activity.activity_record where id=?",
                        String.class,
                        UUID.fromString(id)
                    )
                    .equals("CONFLICT")
            );
            var conflict = ok(
                call("activity", owner, "GET", "/activities/" + id, null),
                200
            );
            assertThat(conflict.get("status").asText()).isEqualTo("COMPLETED");
            assertThat(conflict.get("syncReason").asText()).isEqualTo(
                command.equals("delete") ? "TASK_DELETED" : "TASK_SKIPPED"
            );
        }
    }

    @Test
    void duplicateAndLowerVersionKafkaDeliveryCannotResurrectVoidedCompletionOrDeletedReference()
        throws Exception {
        UUID owner = UUID.randomUUID();
        String task = task(owner, "WORKOUT");
        var saved = ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                request(task, "WORKOUT", "COMPLETED", UUID.randomUUID(), "{}")
            ),
            201
        );
        String id = saved.get("activityId").asText();
        applied(owner, id);
        String original = infra
            .database()
            .queryForObject(
                "select payload::text from activity.outbox_event where aggregateid=? and type='ACTIVITY_COMPLETED'",
                String.class,
                id
            );
        send("todorok.activity.v1", id, original);
        var replay = (tools.jackson.databind.node.ObjectNode) JSON.readTree(
            original
        );
        replay.put("eventId", UUID.randomUUID().toString());
        send("todorok.activity.v1", id, replay.toString());
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities/" + id + "/void",
                "{\"reason\":\"취소\",\"version\":0}"
            ),
            200
        );
        applied(owner, id);
        replay.put("eventId", UUID.randomUUID().toString());
        String last = replay.get("eventId").asText();
        send("todorok.activity.v1", id, replay.toString());
        await(
            () ->
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from planner.processed_event where event_id=?",
                        Integer.class,
                        UUID.fromString(last)
                    ) == 1
        );
        assertThat(
            ok(call("planner", owner, "GET", "/tasks/" + task, null), 200)
                .get("status")
                .asText()
        ).isEqualTo("PLANNED");
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select revision from planner.activity_completion_result where activity_id=?",
                    Long.class,
                    UUID.fromString(id)
                )
        ).isEqualTo(1);
    }

    @Test
    void validatesOwnersTypesUnknownFieldsAndActualIntervalsBeforeWriting()
        throws Exception {
        UUID owner = UUID.randomUUID();
        String task = task(owner, "WORKOUT");
        String base = request(
            task,
            "WORKOUT",
            "PARTIAL",
            UUID.randomUUID(),
            "{}"
        );
        ok(
            call("activity", UUID.randomUUID(), "POST", "/activities", base),
            404
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                base.replace(task, UUID.randomUUID().toString())
            ),
            409
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                base.replace("WORKOUT", "STUDY")
            ),
            409
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                base.replace("\"detail\":{}", "\"detail\":{\"study\":{}}")
            ),
            400
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                base.replace("\"detail\":{}", "\"detail\":{\"surprise\":1}")
            ),
            400
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                base.replace(
                    "\"detail\":{}",
                    "\"detail\":{\"workout\":{\"sets\":[{\"reps\":\"5\"}]}}"
                )
            ),
            400
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                base.substring(0, base.length() - 1) +
                    ",\"startedAt\":\"2026-09-08T01:00:00+09:00\"}"
            ),
            400
        );
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select count(*) from activity.activity_record where task_id=?",
                    Integer.class,
                    UUID.fromString(task)
                )
        ).isZero();
    }

    @Test
    void competingCommandsHaveOneWinnerAndSeriesAdvancesOnlyForCompletion()
        throws Exception {
        UUID owner = UUID.randomUUID();
        String series = ok(
            call(
                "planner",
                owner,
                "POST",
                "/series",
                "{\"title\":\"반복\",\"taskType\":\"STUDY\",\"startDate\":\"2026-09-07\",\"rule\":{\"frequency\":\"DAILY\",\"interval\":1,\"weekdays\":[],\"monthDay\":1}}"
            ),
            201
        )
            .get("seriesId")
            .asText();
        String task = infra
            .database()
            .queryForObject(
                "select id::text from planner.task where series_id=?",
                String.class,
                UUID.fromString(series)
            );
        await(
            () ->
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from activity.task_reference where task_id=?",
                        Integer.class,
                        UUID.fromString(task)
                    ) == 1
        );
        ok(
            call(
                "activity",
                owner,
                "POST",
                "/activities",
                request(task, "STUDY", "PARTIAL", UUID.randomUUID(), "{}")
            ),
            201
        );
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select count(*) from planner.task where series_id=?",
                    Integer.class,
                    UUID.fromString(series)
                )
        ).isOne();
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var start = new CountDownLatch(1);
            var futures = new ArrayList<Future<HttpResponse<String>>>();
            for (int i = 0; i < 6; i++) futures.add(
                pool.submit(() -> {
                    start.await();
                    return call(
                        "activity",
                        owner,
                        "POST",
                        "/activities",
                        request(
                            task,
                            "STUDY",
                            "COMPLETED",
                            UUID.randomUUID(),
                            "{}"
                        )
                    );
                })
            );
            start.countDown();
            var statuses = new ArrayList<Integer>();
            String id = null;
            for (var future : futures) {
                var response = future.get();
                statuses.add(response.statusCode());
                if (response.statusCode() == 201) id = JSON.readTree(
                    response.body()
                )
                    .get("activityId")
                    .asText();
            }
            assertThat(statuses).containsOnly(201, 409);
            assertThat(statuses.stream().filter(s -> s == 201)).hasSize(1);
            applied(owner, id);
        }
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select count(*) from planner.task where series_id=?",
                    Integer.class,
                    UUID.fromString(series)
                )
        ).isEqualTo(2);
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select count(*) from planner.task where series_id=? and status='PLANNED'",
                    Integer.class,
                    UUID.fromString(series)
                )
        ).isOne();
    }

    @Test
    void inboxCompletionAndAckRollbackTogetherThenKafkaRestartRecovers()
        throws Exception {
        UUID owner = UUID.randomUUID();
        String task = task(owner, "STUDY");
        var listener = planner
            .getBean(KafkaListenerEndpointRegistry.class)
            .getListenerContainer("planner-activity");
        listener.stop();
        String id;
        try {
            id = ok(
                call(
                    "activity",
                    owner,
                    "POST",
                    "/activities",
                    request(task, "STUDY", "COMPLETED", UUID.randomUUID(), "{}")
                ),
                201
            )
                .get("activityId")
                .asText();
            String event = infra
                .database()
                .queryForObject(
                    "select payload::text from activity.outbox_event where aggregateid=?",
                    String.class,
                    id
                );
            UUID eventId = UUID.fromString(
                JSON.readTree(event).get("eventId").asText()
            );
            infra
                .database()
                .execute(
                    "alter table planner.outbox_event add constraint fail_ack check(type<>'ACTIVITY_SYNC_RESULT') not valid"
                );
            try {
                assertThatThrownBy(() ->
                    planner
                        .getBean(
                            io.todorok.planner.task.ActivityCompletionConsumer.class
                        )
                        .receive(event)
                ).isInstanceOf(RuntimeException.class);
            } finally {
                infra
                    .database()
                    .execute(
                        "alter table planner.outbox_event drop constraint fail_ack"
                    );
            }
            assertThat(
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from planner.processed_event where event_id=?",
                        Integer.class,
                        eventId
                    )
            ).isZero();
            assertThat(
                infra
                    .database()
                    .queryForObject(
                        "select count(*) from planner.activity_completion_result where activity_id=?",
                        Integer.class,
                        UUID.fromString(id)
                    )
            ).isZero();
            assertThat(
                ok(call("planner", owner, "GET", "/tasks/" + task, null), 200)
                    .get("status")
                    .asText()
            ).isEqualTo("PLANNED");
        } finally {
            listener.start();
        }
        applied(owner, id);
    }

    @Test
    void deletedReferenceRejectsLowerAndHigherResurrectionAndOwnerCorruptionRollsBackInbox()
        throws Exception {
        UUID owner = UUID.randomUUID();
        String task = task(owner, "WORKOUT");
        String original = infra
            .database()
            .queryForObject(
                "select payload::text from planner.outbox_event where aggregateid=? and type='TASK_SCHEDULED'",
                String.class,
                task
            );
        ok(
            call(
                "planner",
                owner,
                "DELETE",
                "/tasks/" + task + "?version=0",
                null
            ),
            204
        );
        await(() ->
            infra
                .database()
                .queryForObject(
                    "select status from activity.task_reference where task_id=?",
                    String.class,
                    UUID.fromString(task)
                )
                .equals("DELETED")
        );
        for (long version : List.of(0L, 99L)) {
            var replay = (tools.jackson.databind.node.ObjectNode) JSON.readTree(
                original
            );
            UUID eventId = UUID.randomUUID();
            replay.put("eventId", eventId.toString());
            replay.put("aggregateVersion", version);
            send("todorok.task.v1", task, replay.toString());
            await(
                () ->
                    infra
                        .database()
                        .queryForObject(
                            "select count(*) from activity.processed_event where event_id=?",
                            Integer.class,
                            eventId
                        ) == 1
            );
            assertThat(
                infra
                    .database()
                    .queryForObject(
                        "select status from activity.task_reference where task_id=?",
                        String.class,
                        UUID.fromString(task)
                    )
            ).isEqualTo("DELETED");
        }
        var corrupt = (tools.jackson.databind.node.ObjectNode) JSON.readTree(
            original
        );
        UUID eventId = UUID.randomUUID();
        corrupt.put("eventId", eventId.toString());
        corrupt.put("userId", UUID.randomUUID().toString());
        assertThatThrownBy(() ->
            activity
                .getBean(io.todorok.activity.record.ActivityEventConsumer.class)
                .receive(corrupt.toString())
        ).isInstanceOf(IllegalArgumentException.class);
        assertThat(
            infra
                .database()
                .queryForObject(
                    "select count(*) from activity.processed_event where event_id=?",
                    Integer.class,
                    eventId
                )
        ).isZero();
    }
}
