package io.todorok.messaging;

import io.todorok.contracts.EventEnvelope;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public final class JdbcOutboxEventWriter implements OutboxEventWriter {

    private static final String INSERT_SQL = """
            insert into outbox_event(
                id, aggregatetype, aggregateid, type, payload, occurred_at
            ) values (?, ?, ?, ?, cast(? as jsonb), ?)
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public JdbcOutboxEventWriter(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public void append(String aggregateType, String aggregateId, EventEnvelope<?> event) {
        requireNotBlank(aggregateType, "aggregateType");
        requireNotBlank(aggregateId, "aggregateId");
        Objects.requireNonNull(event, "event must not be null");

        jdbc.update(
                INSERT_SQL,
                event.eventId(),
                aggregateType,
                aggregateId,
                event.type().name(),
                serialize(event),
                event.occurredAt().atOffset(ZoneOffset.UTC));
    }

    private String serialize(EventEnvelope<?> event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException("event serialization failed", exception);
        }
    }

    private static void requireNotBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
