package io.todorok.notification.preference;

import io.todorok.contracts.EventType;
import io.todorok.messaging.EventJson;
import io.todorok.messaging.InboxEventGuard;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class TaskReminderConsumer {
    private static final ZoneId SEOUL=ZoneId.of("Asia/Seoul");
    private final ObjectMapper mapper;
    private final InboxEventGuard inbox;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public TaskReminderConsumer(ObjectMapper mapper,InboxEventGuard inbox,JdbcTemplate jdbc,Clock clock){
        this.mapper=mapper;this.inbox=inbox;this.jdbc=jdbc;this.clock=clock;
    }
    @KafkaListener(id="notification-task-reminder", topics="todorok.task-reminder.v1", groupId="notification-task-reminder-v1", autoStartup="${todorok.messaging.enabled:false}")
    @Transactional
    public void receive(String json){
        var event=EventJson.read(mapper,json);
        if(event.type()!=EventType.TASK_REMINDER_CHANGED)return;
        var p=event.payload();
        if(event.aggregateVersion()<0 || !p.path("enabled").isBoolean() || !p.has("scheduledFor"))
            throw new IllegalArgumentException("Invalid task reminder");
        UUID task=EventJson.uuid(p,"taskId");boolean enabled=p.get("enabled").asBoolean();
        Instant scheduled=p.get("scheduledFor").isNull()?null:Instant.parse(EventJson.text(p,"scheduledFor"));
        if(enabled&&scheduled==null)throw new IllegalArgumentException("A reminder requires a time");
        // Serialize changes for this task, including the first event and ownership checks.
        jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))","task-reminder:"+task);
        var prior=jdbc.query("select user_id,revision,scheduled_for,enabled from task_reminder where task_id=?",
            (r,i)->new Prior(r.getObject(1,UUID.class),r.getLong(2),r.getObject(3,OffsetDateTime.class),r.getBoolean(4)),task);
        if(!prior.isEmpty()&&!prior.getFirst().owner().equals(event.userId()))throw new IllegalArgumentException("Task owner cannot change");
        if(!inbox.claim(event.eventId(),event.type().name()))return;
        if(!prior.isEmpty()&&prior.getFirst().revision()>=event.aggregateVersion())return;
        jdbc.update("""
            insert into task_reminder(task_id,user_id,revision,scheduled_for,enabled) values (?,?,?,?,?)
            on conflict(task_id) do update set revision=excluded.revision,scheduled_for=excluded.scheduled_for,enabled=excluded.enabled
            """,task,event.userId(),event.aggregateVersion(),offset(scheduled),enabled);
        if(!prior.isEmpty()){
            var old=prior.getFirst();
            if(old.enabled()==enabled&&Objects.equals(old.scheduled()==null?null:old.scheduled().toInstant(),scheduled))return;
        }
        jdbc.update("""
            update notification_delivery set status='CANCELED',error_code='TASK_CHANGED',lease_token=null,lease_until=null
            where task_id=? and status='PENDING'
            """,task);
        if(!enabled)return;
        Instant deadline=scheduled.plusSeconds(1800);
        var local=scheduled.atZone(SEOUL).toLocalTime();
        boolean quiet=local.isBefore(LocalTime.of(7,0))||!local.isBefore(LocalTime.of(22,0));
        boolean expired=quiet||!deadline.isAfter(clock.instant());
        jdbc.update("""
            insert into notification_delivery(id,user_id,kind,task_id,task_revision,scheduled_for,deadline_at,status,error_code,next_attempt_at)
            values (?,?,'TASK_REMINDER',?,?,?,?,?,?,?)
            """,UUID.randomUUID(),event.userId(),task,event.aggregateVersion(),offset(scheduled),offset(deadline),
            expired?"EXPIRED":"PENDING",quiet?"QUIET_HOURS":expired?"DEADLINE_EXCEEDED":null,offset(scheduled));
    }
    private OffsetDateTime offset(Instant time){return time==null?null:time.atOffset(ZoneOffset.UTC);}
    private record Prior(UUID owner,long revision,OffsetDateTime scheduled,boolean enabled){}
}
