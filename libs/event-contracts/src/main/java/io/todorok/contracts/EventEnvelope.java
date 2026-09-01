package io.todorok.contracts;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        EventType type,
        int version,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant occurredAt,
        UUID userId,
        T payload) {

    public EventEnvelope {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be at least 1");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
    }
}
