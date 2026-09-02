package io.todorok.messaging;

import io.todorok.contracts.EventEnvelope;

public interface OutboxEventWriter {
    void append(String aggregateType, String aggregateId, EventEnvelope<?> event);
}
