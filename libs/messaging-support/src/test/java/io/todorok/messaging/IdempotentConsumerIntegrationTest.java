package io.todorok.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class IdempotentConsumerIntegrationTest {

    private static final UUID EVENT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000301");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka-native:4.3.1");

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17.11-alpine");

    private JdbcTemplate jdbc;
    private KafkaTemplate<Object, Object> template;
    private KafkaMessageListenerContainer<Object, Object> listenerContainer;
    private final AtomicInteger processedRecords = new AtomicInteger();

    @BeforeEach
    void setUp() throws Exception {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("drop table if exists processed_event");
        jdbc.execute("drop table if exists local_counter");
        jdbc.execute("""
                create table processed_event (
                    event_id uuid primary key,
                    event_type varchar(100) not null,
                    processed_at timestamptz not null default now()
                )
                """);
        jdbc.execute("create table local_counter(value integer not null)");
        jdbc.update("insert into local_counter(value) values (0)");
        var guard = new JdbcInboxEventGuard(jdbc);
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        try (var admin = AdminClient.create(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic("idempotent-topic", 1, (short) 1)))
                    .all().get();
        }

        template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)));
        var consumerProperties = Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "idempotent-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        var containerProperties = new ContainerProperties("idempotent-topic");
        containerProperties.setAckMode(ContainerProperties.AckMode.RECORD);
        listenerContainer = new KafkaMessageListenerContainer<>(
                new DefaultKafkaConsumerFactory<>(consumerProperties),
                containerProperties);
        listenerContainer.setupMessageListener((MessageListener<Object, Object>) record -> {
            transaction.executeWithoutResult(status -> {
                var eventId = UUID.fromString(record.value().toString());
                if (guard.claim(eventId, "TASK_CHANGED")) {
                    jdbc.update("update local_counter set value = value + 1");
                }
            });
            processedRecords.incrementAndGet();
        });
        startListener();
    }

    @AfterEach
    void tearDown() {
        if (listenerContainer != null) listenerContainer.stop();
        if (template != null) template.destroy();
    }

    @Test
    void duplicateOffsetsAndConsumerRestartApplyLocalResultOnce() throws Exception {
        template.send("idempotent-topic", "task-1", EVENT_ID.toString()).get();
        template.send("idempotent-topic", "task-1", EVENT_ID.toString()).get();
        awaitProcessedRecords(2, Duration.ofSeconds(15));
        assertThat(counter()).isEqualTo(1);

        listenerContainer.stop();
        startListener();
        template.send("idempotent-topic", "task-1", EVENT_ID.toString()).get();
        awaitProcessedRecords(3, Duration.ofSeconds(15));
        assertThat(counter()).isEqualTo(1);
    }

    private void startListener() {
        listenerContainer.start();
        ContainerTestUtils.waitForAssignment(listenerContainer, 1);
    }

    private void awaitProcessedRecords(int expected, Duration timeout) {
        var deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (processedRecords.get() >= expected) return;
            try {
                Thread.sleep(50L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        }
        throw new AssertionError(
                "expected " + expected + " records but got " + processedRecords.get());
    }

    private int counter() {
        return jdbc.queryForObject("select value from local_counter", Integer.class);
    }
}
