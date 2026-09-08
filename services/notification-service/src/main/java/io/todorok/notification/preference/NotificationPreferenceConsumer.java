package io.todorok.notification.preference;

import io.todorok.contracts.EventType;
import io.todorok.messaging.EventJson;
import io.todorok.messaging.InboxEventGuard;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class NotificationPreferenceConsumer {
    private final ObjectMapper mapper;
    private final InboxEventGuard inbox;
    private final JdbcTemplate jdbc;
    private final NotificationScheduleService schedules;
    public NotificationPreferenceConsumer(ObjectMapper mapper, InboxEventGuard inbox, JdbcTemplate jdbc, NotificationScheduleService schedules) { this.mapper = mapper; this.inbox = inbox; this.jdbc = jdbc; this.schedules = schedules; }

    @KafkaListener(id="notification-preference", topics="todorok.notification-preference.v1", groupId="notification-preference-v1", autoStartup="${todorok.messaging.enabled:false}")
    @Transactional
    public void receive(String json) {
        var event = EventJson.read(mapper, json);
        if (event.type() != EventType.NOTIFICATION_PREFERENCE_CHANGED) return;
        if (!event.payload().path("notificationsEnabled").isBoolean()) throw new IllegalArgumentException("Invalid notification preference");
        boolean enabled = event.payload().get("notificationsEnabled").asBoolean();
        String time = EventJson.text(event.payload(), "summaryTime");
        if (!time.matches("^(?:[01][0-9]|2[0-3]):[0-5][0-9]$")) throw new IllegalArgumentException("Invalid summary time");
        if (!inbox.claim(event.eventId(), event.type().name())) return;
        UUID owner = event.userId();
        int written = jdbc.update("""
            insert into notification_preference(user_id,notifications_enabled,summary_time,revision)
            values (?,?,?,?)
            on conflict(user_id) do update set notifications_enabled=excluded.notifications_enabled,summary_time=excluded.summary_time,
              revision=excluded.revision,updated_at=now()
            where notification_preference.revision < excluded.revision
            """, owner, enabled, time, event.aggregateVersion());
        if (written > 0) schedules.apply(owner, enabled, time);
    }
}
