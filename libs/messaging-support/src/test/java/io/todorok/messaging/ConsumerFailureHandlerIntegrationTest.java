package io.todorok.messaging;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@Testcontainers
class ConsumerFailureHandlerIntegrationTest {

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka-native:4.3.1");

    private KafkaTemplate<Object, Object> template;
    private KafkaMessageListenerContainer<Object, Object> listenerContainer;
    private final AtomicInteger attempts = new AtomicInteger();

    @BeforeEach
    void setUp() throws Exception {
        try (var admin = AdminClient.create(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(
                    new NewTopic("source-topic", 1, (short) 1),
                    new NewTopic("todorok.dead-letter", 1, (short) 1)))
                    .all().get();
        }

        var producerProperties = Map.<String, Object>of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProperties));

        var consumerProperties = Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "failure-handler-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        var properties = new ContainerProperties("source-topic");
        listenerContainer = new KafkaMessageListenerContainer<>(
                new DefaultKafkaConsumerFactory<>(consumerProperties),
                properties);
        listenerContainer.setupMessageListener((MessageListener<Object, Object>) record -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("consumer-probe");
        });
        listenerContainer.setCommonErrorHandler(
                new DefaultConsumerFailureHandlerFactory()
                        .create(template, "todorok.dead-letter"));
        listenerContainer.start();
        ContainerTestUtils.waitForAssignment(listenerContainer, 1);
    }

    @AfterEach
    void tearDown() {
        if (listenerContainer != null) listenerContainer.stop();
        if (template != null) template.destroy();
    }

    @Test
    void retriesThreeTimesThenPublishesDeadLetter() throws Exception {
        template.send("source-topic", "aggregate-1", "broken-event").get();
        var deadLetter = pollDeadLetter(Duration.ofSeconds(20));

        assertThat(attempts).hasValue(4);
        assertThat(deadLetter.key()).isEqualTo("aggregate-1");
        assertThat(deadLetter.value()).isEqualTo("broken-event");
        assertThat(deadLetter.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC).value())
                .isEqualTo("source-topic".getBytes(UTF_8));
        assertThat(new String(
                deadLetter.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN).value(),
                UTF_8)).contains("IllegalStateException");
    }

    private org.apache.kafka.clients.consumer.ConsumerRecord<String, String> pollDeadLetter(
            Duration timeout) {
        var properties = Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dead-letter-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (var consumer = new KafkaConsumer<String, String>(properties)) {
            consumer.subscribe(List.of("todorok.dead-letter"));
            var deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                var records = consumer.poll(Duration.ofMillis(250));
                if (!records.isEmpty()) return records.iterator().next();
            }
        }
        throw new AssertionError("dead-letter record was not published");
    }
}
