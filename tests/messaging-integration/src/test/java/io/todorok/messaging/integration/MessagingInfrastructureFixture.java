package io.todorok.messaging.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class MessagingInfrastructureFixture implements AutoCloseable {

    private static final int CONNECT_PORT = 8083;

    private final Network network;
    private final PostgreSQLContainer postgres;
    private final KafkaContainer kafka;
    private final GenericContainer<?> connect;
    private final JdbcTemplate jdbc;
    private final JsonMapper mapper = JsonMapper.builder().build();

    private MessagingInfrastructureFixture(
            Network network,
            PostgreSQLContainer postgres,
            KafkaContainer kafka,
            GenericContainer<?> connect,
            JdbcTemplate jdbc) {
        this.network = network;
        this.postgres = postgres;
        this.kafka = kafka;
        this.connect = connect;
        this.jdbc = jdbc;
    }

    static MessagingInfrastructureFixture start() {
        var network = Network.newNetwork();
        var postgres = new PostgreSQLContainer("postgres:17.11-alpine")
                .withDatabaseName("todorok")
                .withUsername("postgres")
                .withPassword("postgres")
                .withNetwork(network)
                .withNetworkAliases("postgres")
                .withCommand(
                        "postgres",
                        "-c", "wal_level=logical",
                        "-c", "max_wal_senders=1",
                        "-c", "max_replication_slots=1",
                        "-c", "max_slot_wal_keep_size=2048MB");
        var kafka = new KafkaContainer("apache/kafka-native:4.3.1")
                .withNetwork(network)
                .withListener("kafka:19092");
        postgres.start();
        kafka.start();

        var dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var jdbc = new JdbcTemplate(dataSource);
        initializeDatabase(jdbc);
        initializeTopics(kafka.getBootstrapServers());

        var connect = new GenericContainer<>("quay.io/debezium/connect:3.6.2.Final")
                .withNetwork(network)
                .withNetworkAliases("connect")
                .withExposedPorts(CONNECT_PORT)
                .withEnv("BOOTSTRAP_SERVERS", "kafka:19092")
                .withEnv("GROUP_ID", "todorok-connect-test")
                .withEnv("CONFIG_STORAGE_TOPIC", "todorok.connect.configs")
                .withEnv("OFFSET_STORAGE_TOPIC", "todorok.connect.offsets")
                .withEnv("STATUS_STORAGE_TOPIC", "todorok.connect.status")
                .withEnv("CONFIG_STORAGE_REPLICATION_FACTOR", "1")
                .withEnv("OFFSET_STORAGE_REPLICATION_FACTOR", "1")
                .withEnv("STATUS_STORAGE_REPLICATION_FACTOR", "1")
                .withEnv("KEY_CONVERTER", "org.apache.kafka.connect.storage.StringConverter")
                .withEnv("VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
                .withEnv("VALUE_CONVERTER_SCHEMAS_ENABLE", "false")
                .waitingFor(Wait.forHttp("/connectors")
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(2)));
        connect.start();

        var fixture = new MessagingInfrastructureFixture(
                network, postgres, kafka, connect, jdbc);
        fixture.registerConnector();
        fixture.awaitConnectorRunning();
        return fixture;
    }

    void insertOutbox(
            String schema,
            UUID eventId,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload) {
        if (!schema.equals("planner") && !schema.equals("activity")) {
            throw new IllegalArgumentException("unsupported schema: " + schema);
        }
        jdbc.update(
                "insert into " + schema + ".outbox_event("
                        + "id, aggregatetype, aggregateid, type, payload, occurred_at) "
                        + "values (?, ?, ?, ?, cast(? as jsonb), ?)",
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                payload,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    void updateServiceMetadata(String schema, int version) {
        if (!schema.equals("planner") && !schema.equals("activity")) {
            throw new IllegalArgumentException("unsupported schema: " + schema);
        }
        jdbc.update(
                "update " + schema + ".service_metadata set schema_version = ?",
                version);
    }

    ReceivedRecord consume(String topic, UUID eventId, Duration timeout) {
        var properties = Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "cdc-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (var consumer = new KafkaConsumer<String, String>(properties)) {
            consumer.subscribe(List.of(topic));
            var deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                for (var record : consumer.poll(Duration.ofMillis(250))) {
                    try {
                        var value = mapper.readTree(record.value());
                        if (!value.has("eventId")) {
                            throw new AssertionError("Unexpected CDC JSON root: " + record.value());
                        }
                        if (eventId.toString().equals(value.path("eventId").asText())) {
                            return new ReceivedRecord(record.key(), value, record.offset());
                        }
                    } catch (Exception exception) {
                        throw new IllegalStateException("invalid Kafka JSON", exception);
                    }
                }
            }
        }
        throw new AssertionError("event was not received from " + topic + ": " + eventId
                + "\nconnector status: " + connectorStatus()
                + "\ntopics: " + topicNames()
                + "\nconnect logs:\n" + connect.getLogs());
    }

    boolean hasCdcRecordForServiceMetadata(Duration timeout) {
        var deadline = System.nanoTime() + timeout.toNanos();
        try (var admin = AdminClient.create(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
            while (System.nanoTime() < deadline) {
                var names = admin.listTopics().names().get();
                if (names.stream().anyMatch(name -> name.contains("service_metadata"))) {
                    return true;
                }
                Thread.sleep(100L);
            }
            return false;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    void awaitConnectorRunning() {
        var deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                var response = httpClient().send(
                        HttpRequest.newBuilder(connectorUri("/status")).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    var status = mapper.readTree(response.body());
                    if (status.path("connector").path("state").asText().equals("RUNNING")
                            && status.path("tasks").path(0).path("state").asText()
                            .equals("RUNNING")
                            && slotIsActive()) {
                        return;
                    }
                }
                Thread.sleep(250L);
            } catch (Exception ignored) {
                try {
                    Thread.sleep(250L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }
        }
        throw new AssertionError("Debezium connector did not reach RUNNING state");
    }

    private boolean slotIsActive() {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select active from pg_replication_slots "
                        + "where slot_name = 'todorok_outbox_slot'",
                Boolean.class));
    }

    private void registerConnector() {
        try {
            var template = java.nio.file.Path.of(System.getProperty("todorok.repository.root"))
                    .resolve("infra/docker/connect/connector-template.json");
            var request = java.nio.file.Files.readString(template)
                    .replace("${POSTGRES_DB}", "todorok")
                    .replace("${DEBEZIUM_DB_PASSWORD}", "debezium");
            var response = httpClient().send(
                    HttpRequest.newBuilder(connectUri("/connectors"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(request))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new IllegalStateException(
                        "connector registration failed: " + response.statusCode()
                                + " " + response.body());
            }
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private HttpClient httpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    private String connectorStatus() {
        try {
            return httpClient().send(
                    HttpRequest.newBuilder(connectorUri("/status")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception exception) {
            return exception.toString();
        }
    }

    private java.util.Set<String> topicNames() {
        try (var admin = AdminClient.create(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
            return admin.listTopics().names().get();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private URI connectUri(String path) {
        return URI.create("http://" + connect.getHost() + ":"
                + connect.getMappedPort(CONNECT_PORT) + path);
    }

    private URI connectorUri(String suffix) {
        return connectUri("/connectors/todorok-postgres-outbox" + suffix);
    }

    private static void initializeDatabase(JdbcTemplate jdbc) {
        jdbc.execute("create role debezium_app login replication password 'debezium'");
        jdbc.execute("create schema planner");
        jdbc.execute("create schema activity");
        for (var schema : new String[] {"planner", "activity"}) {
            jdbc.execute("create table " + schema + ".service_metadata ("
                    + "service_name varchar(40) primary key, schema_version integer not null)");
            jdbc.update("insert into " + schema
                    + ".service_metadata(service_name, schema_version) values (?, 1)", schema);
            jdbc.execute("create table " + schema + ".outbox_event ("
                    + "id uuid primary key, aggregatetype varchar(80) not null, "
                    + "aggregateid varchar(255) not null, type varchar(100) not null, "
                    + "payload jsonb not null, occurred_at timestamptz not null, "
                    + "created_at timestamptz not null default now())");
            jdbc.execute("grant usage on schema " + schema + " to debezium_app");
            jdbc.execute("grant select on " + schema + ".outbox_event to debezium_app");
        }
        jdbc.execute("create publication todorok_outbox for table "
                + "planner.outbox_event, activity.outbox_event");
        jdbc.queryForList("select * from pg_create_logical_replication_slot("
                + "'todorok_outbox_slot', 'pgoutput')");
    }

    private static void initializeTopics(String bootstrapServers) {
        try (var admin = AdminClient.create(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            admin.createTopics(List.of(
                    new NewTopic("todorok.task.v1", 3, (short) 1),
                    new NewTopic("todorok.activity.v1", 3, (short) 1),
                    new NewTopic("todorok.connect.configs", 1, (short) 1)
                            .configs(Map.of("cleanup.policy", "compact")),
                    new NewTopic("todorok.connect.offsets", 1, (short) 1)
                            .configs(Map.of("cleanup.policy", "compact")),
                    new NewTopic("todorok.connect.status", 1, (short) 1)
                            .configs(Map.of("cleanup.policy", "compact"))))
                    .all().get();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    public void close() {
        connect.stop();
        kafka.stop();
        postgres.stop();
        network.close();
    }

    record ReceivedRecord(String key, JsonNode value, long offset) {}
}
