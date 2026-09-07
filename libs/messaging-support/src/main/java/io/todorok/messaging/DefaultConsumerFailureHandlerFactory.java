package io.todorok.messaging;

import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

public final class DefaultConsumerFailureHandlerFactory
        implements ConsumerFailureHandlerFactory {

    private static final long RETRY_INTERVAL_MILLIS = 1_000L;
    private static final long RETRY_COUNT = 3L;

    @Override
    public CommonErrorHandler create(
            KafkaOperations<Object, Object> operations,
            String deadLetterTopic) {
        if (operations == null) {
            throw new NullPointerException("operations must not be null");
        }
        if (deadLetterTopic == null || deadLetterTopic.isBlank()) {
            throw new IllegalArgumentException("deadLetterTopic must not be blank");
        }
        var recoverer = new DeadLetterPublishingRecoverer(
                operations,
                (record, exception) -> new TopicPartition(
                        deadLetterTopic,
                        record.partition()));
        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(RETRY_INTERVAL_MILLIS, RETRY_COUNT));
    }
}
