package io.todorok.messaging;

import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcInboxEventGuard implements InboxEventGuard {

    private static final String CLAIM_SQL = """
            insert into processed_event(event_id, event_type)
            values (?, ?)
            on conflict (event_id) do nothing
            """;

    private final JdbcTemplate jdbc;

    public JdbcInboxEventGuard(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
    }

    @Override
    public boolean claim(UUID eventId, String eventType) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
        return jdbc.update(CLAIM_SQL, eventId, eventType) == 1;
    }
}
