package io.todorok.activity.record;

import io.todorok.contracts.EventType;
import io.todorok.messaging.*;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ActivityEventConsumer {

    private final JdbcTemplate jdbc;
    private final InboxEventGuard inbox;
    private final ObjectMapper mapper;

    public ActivityEventConsumer(
        JdbcTemplate jdbc,
        InboxEventGuard inbox,
        ObjectMapper mapper
    ) {
        this.jdbc = jdbc;
        this.inbox = inbox;
        this.mapper = mapper;
    }

    @KafkaListener(
        id = "activity-reference",
        topics = "todorok.task.v1",
        groupId = "activity-reference-v1",
        autoStartup = "${todorok.messaging.enabled:false}"
    )
    @Transactional
    public void receive(String json) {
        var e = EventJson.read(mapper, json);
        if (
            e.type() != EventType.TASK_SCHEDULED &&
            e.type() != EventType.TASK_CHANGED &&
            e.type() != EventType.TASK_ROLLED_OVER &&
            e.type() != EventType.ACTIVITY_SYNC_RESULT
        ) return;
        var p = e.payload();
        var task = EventJson.uuid(p, "taskId");
        if (e.type() == EventType.ACTIVITY_SYNC_RESULT) {
            var activity = EventJson.uuid(p, "activityId");
            String state = EventJson.text(p, "state"),
                reason = EventJson.text(p, "reason");
            if (
                !Set.of("APPLIED", "CONFLICT").contains(state) ||
                reason.length() > 100
            ) throw new IllegalArgumentException("Invalid result");
            if (!inbox.claim(e.eventId(), e.type().name())) return;
            jdbc.update(
                "update activity_record set sync_state=?,sync_reason=? where id=? and task_id=? and user_id=? and revision=? and sync_state='PENDING'",
                state,
                reason,
                activity,
                task,
                e.userId(),
                e.aggregateVersion()
            );
            return;
        }
        String type = EventJson.text(p, "taskType"),
            status = EventJson.text(p, "status");
        LocalDate date = LocalDate.parse(EventJson.text(p, "scheduledDate"));
        if (
            !Set.of("GENERAL", "WORKOUT", "STUDY", "CLIMBING").contains(type) ||
            !Set.of("PLANNED", "COMPLETED", "SKIPPED", "DELETED").contains(
                status
            )
        ) throw new IllegalArgumentException("Invalid task reference");
        if (!inbox.claim(e.eventId(), e.type().name())) return;
        jdbc.update(
            """
            insert into task_reference(task_id,user_id,task_type,scheduled_date,status,version) values (?,?,?,?,?,?)
            on conflict(task_id) do update set scheduled_date=excluded.scheduled_date,status=excluded.status,version=excluded.version
            where task_reference.version<excluded.version and task_reference.user_id=excluded.user_id
                and task_reference.task_type=excluded.task_type and task_reference.status<>'DELETED'
            """,
            task,
            e.userId(),
            type,
            date,
            status,
            e.aggregateVersion()
        );
        var reference = jdbc.queryForMap(
            "select user_id,task_type from task_reference where task_id=?",
            task
        );
        if (
            !e.userId().equals(reference.get("user_id")) ||
            !type.equals(reference.get("task_type"))
        ) throw new IllegalArgumentException("Task reference identity changed");
    }
}
