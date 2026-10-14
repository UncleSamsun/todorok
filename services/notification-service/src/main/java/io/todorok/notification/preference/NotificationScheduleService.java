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

    @org.springframework.transaction.annotation.Transactional
    public void apply(UUID owner, boolean enabled, String summaryTime) {
        if (!enabled) {
            jdbc.update("update notification_delivery set status='CANCELED',error_code='PREFERENCE_DISABLED',lease_token=null,lease_until=null where user_id=? and status='PENDING'", owner);
            return;
        }
        var now = clock.instant();
        // Use the consumer's task lock, then re-read its projection in a new statement.
        // Otherwise a concurrent completion can leave a stale canceled row resurrected.
        var candidates=jdbc.queryForList("select task_id from task_reminder where user_id=? and enabled and scheduled_for>? order by task_id",
            UUID.class,owner,now.atOffset(java.time.ZoneOffset.UTC));
        for(UUID task:candidates){
            jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))","task-reminder:"+task);
            jdbc.update("""
            update notification_delivery d set status='PENDING',error_code=null,lease_token=null,lease_until=null,
                next_attempt_at=d.scheduled_for
            from task_reminder r
            where d.task_id=r.task_id and d.user_id=? and d.task_id=? and d.kind='TASK_REMINDER'
                and d.status='CANCELED' and d.error_code='PREFERENCE_DISABLED'
                and r.enabled and d.scheduled_for=r.scheduled_for and d.scheduled_for>?
                and not exists(select 1 from notification_delivery newer
                    where newer.task_id=d.task_id and newer.task_revision>d.task_revision)
            """,owner,task,now.atOffset(java.time.ZoneOffset.UTC));
        }
        var scheduled = now.atZone(SEOUL).toLocalDate().atTime(LocalTime.parse(summaryTime)).atZone(SEOUL);
        if (!scheduled.toInstant().isAfter(now)) scheduled = scheduled.plusDays(1);
        var due = scheduled.toOffsetDateTime();
        jdbc.update("""
            update notification_delivery
            set status='CANCELED'
            where user_id=? and kind='DAILY_SUMMARY' and status='PENDING' and scheduled_for <> ?
            """, owner, due);
        jdbc.update("""
            insert into notification_delivery(id,user_id,kind,scheduled_for,deadline_at,status)
            values (?,?,'DAILY_SUMMARY',?,?, 'PENDING')
            on conflict(user_id,scheduled_for) where kind='DAILY_SUMMARY' do update
                set status='PENDING',error_code=null,lease_token=null,lease_until=null,next_attempt_at=excluded.scheduled_for
                where notification_delivery.status='CANCELED'
        """, UUID.randomUUID(), owner, due, scheduled.toLocalDate().atTime(10,0).atZone(SEOUL).toOffsetDateTime());
    }
}
