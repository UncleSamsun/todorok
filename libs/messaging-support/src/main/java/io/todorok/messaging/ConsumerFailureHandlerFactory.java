package io.todorok.messaging;

import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;

public interface ConsumerFailureHandlerFactory {
    CommonErrorHandler create(
            KafkaOperations<Object, Object> operations,
            String deadLetterTopic);
}
