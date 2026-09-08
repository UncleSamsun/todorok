package io.todorok.notification.preference;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationScheduleService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public NotificationScheduleService(JdbcTemplate jdbc, Clock clock) { this.jdbc = jdbc; this.clock = clock; }

    public void apply(UUID owner, boolean enabled, String summaryTime) {
        if (!enabled) {
            jdbc.update("update notification_delivery set status='CANCELED' where user_id=? and kind='DAILY_SUMMARY' and status='PENDING'", owner);
            return;
        }
        var now = clock.instant();
        var scheduled = now.atZone(SEOUL).toLocalDate().atTime(LocalTime.parse(summaryTime)).atZone(SEOUL);
        if (!scheduled.toInstant().isAfter(now)) scheduled = scheduled.plusDays(1);
        var due = scheduled.toOffsetDateTime();
        jdbc.update("""
            insert into notification_delivery(id,user_id,kind,scheduled_for,deadline_at,status)
            values (?,?,'DAILY_SUMMARY',?,?, 'PENDING')
            on conflict(user_id,kind,scheduled_for) do nothing
            """, UUID.randomUUID(), owner, due, due.plusHours(2));
    }
}
