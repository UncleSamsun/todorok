package io.todorok.planner.task;

import io.todorok.contracts.*;
import io.todorok.contracts.events.ActivitySyncResult;
import io.todorok.messaging.*;
import io.todorok.planner.api.model.*;
import io.todorok.planner.series.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ActivityCompletionConsumer {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final InboxEventGuard inbox;
    private final OutboxEventWriter outbox;
    private final TaskRepository tasks;
    private final SeriesRepository series;
    private final SeriesService recurrence;
    private final TaskEvents events;

    public ActivityCompletionConsumer(
        JdbcTemplate jdbc,
        ObjectMapper mapper,
        InboxEventGuard inbox,
        OutboxEventWriter outbox,
        TaskRepository tasks,
        SeriesRepository series,
        SeriesService recurrence,
        TaskEvents events
    ) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.inbox = inbox;
        this.outbox = outbox;
        this.tasks = tasks;
        this.series = series;
        this.recurrence = recurrence;
        this.events = events;
    }

    @KafkaListener(
        id = "planner-activity",
        topics = "todorok.activity.v1",
        groupId = "planner-activity-v1",
        autoStartup = "${todorok.messaging.enabled:false}"
    )
    @Transactional
    public void receive(String json) {
        var e = EventJson.read(mapper, json);
        if (
            e.type() != EventType.ACTIVITY_COMPLETED &&
            e.type() != EventType.ACTIVITY_VOIDED
        ) return;
        var p = e.payload();
        var id = EventJson.uuid(p, "activityId");
        var taskId = EventJson.uuid(p, "taskId");
        boolean complete = e.type() == EventType.ACTIVITY_COMPLETED;
        TaskType type = null;
        OffsetDateTime performed = null;
        String summary = null;
        if (complete) {
            type = TaskType.valueOf(EventJson.text(p, "activityType"));
            if (type == TaskType.GENERAL) throw new IllegalArgumentException(
                "Activity requires a record type"
            );
            performed = Instant.parse(
                EventJson.text(p, "completedAt")
            ).atOffset(ZoneOffset.UTC);
            summary = EventJson.text(p, "outcome");
            if (
                p.hasNonNull("startedAt") != p.hasNonNull("endedAt")
            ) throw new IllegalArgumentException("Incomplete interval");
            if (p.hasNonNull("startedAt")) {
                var start = Instant.parse(EventJson.text(p, "startedAt"));
                var end = Instant.parse(EventJson.text(p, "endedAt"));
                var zone = ZoneId.of("Asia/Seoul");
                var date = performed.atZoneSameInstant(zone).toLocalDate();
                if (
                    !end.isAfter(start) ||
                    !start.atZone(zone).toLocalDate().equals(date) ||
                    !end.atZone(zone).toLocalDate().equals(date)
                ) throw new IllegalArgumentException("Invalid actual interval");
            }
        } else {
            Instant.parse(EventJson.text(p, "voidedAt"));
            EventJson.text(p, "reason");
        }
        if (!inbox.claim(e.eventId(), e.type().name())) return;
        // Same activity can arrive through different event IDs/partitions. Serialize even before the result row exists.
        jdbc.queryForList(
            "select pg_advisory_xact_lock(hashtextextended(?,0))",
            "activity:" + id
        );
        var previous = jdbc.queryForList(
            "select * from activity_completion_result where activity_id=?",
            id
        );
        if (!previous.isEmpty()) {
            var old = previous.getFirst();
            if (
                !e.userId().equals(old.get("user_id")) ||
                !taskId.equals(old.get("task_id"))
            ) throw new IllegalArgumentException("Activity identity changed");
            if (
                ((Number) old.get("revision")).longValue() >=
                e.aggregateVersion()
            ) return;
        }
        // Same order as all planner commands: series before task; deleted tasks remain visible to this lock.
        tasks
            .seriesId(e.userId(), taskId)
            .ifPresent(s -> series.lock(e.userId(), s).orElseThrow());
        var found = tasks.lockIncludingDeleted(e.userId(), taskId);
        String reason = "APPLIED";
        if (found.isEmpty()) reason = "TASK_NOT_FOUND";
        else {
            var task = found.get();
            if (
                task.status == TaskStatus.DELETED ||
                task.status == TaskStatus.SKIPPED
            ) reason = "TASK_" + task.status;
            else if (complete) {
                if (task.taskType != type) reason = "TASK_TYPE_MISMATCH";
                else if (
                    task.activityId != null && !task.activityId.equals(id)
                ) reason = "OTHER_ACTIVITY_LINKED";
                else if (
                    task.status != TaskStatus.PLANNED &&
                    !id.equals(task.activityId)
                ) reason = "TASK_ALREADY_COMPLETED";
                else {
                    task.activityId = id;
                    task.performedAt = performed;
                    task.completionSummary = summary;
                    task.startedAt = p.hasNonNull("startedAt")
                        ? Instant.parse(
                              EventJson.text(p, "startedAt")
                          ).atOffset(ZoneOffset.UTC)
                        : null;
                    task.endedAt = p.hasNonNull("endedAt")
                        ? Instant.parse(EventJson.text(p, "endedAt")).atOffset(
                              ZoneOffset.UTC
                          )
                        : null;
                    task.status = TaskStatus.COMPLETED;
                    task.scheduledDate = performed
                        .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                        .toLocalDate();
                    tasks.flush();
                    events.publish(task, "COMPLETED");
                    if (task.seriesId != null) recurrence.advance(
                        e.userId(),
                        task.seriesId,
                        task.occurrenceDate,
                        "COMPLETED"
                    );
                }
            } else if (
                task.activityId != null && !id.equals(task.activityId)
            ) reason = "OTHER_ACTIVITY_LINKED";
            else if (id.equals(task.activityId)) {
                if (
                    task.seriesId != null && tasks.hasActive(task.seriesId)
                ) reason = "ACTIVE_OCCURRENCE_EXISTS";
                else {
                    task.activityId = null;
                    task.performedAt = null;
                    task.completionSummary = null;
                    task.status = TaskStatus.PLANNED;
                    task.startedAt = null;
                    task.endedAt = null;
                    tasks.flush();
                    events.publish(task, "REOPENED");
                }
            }
            // A void arriving before completion is a successful tombstone; the lower completion revision is ignored.
        }
        String state = reason.equals("APPLIED") ? "APPLIED" : "CONFLICT";
        jdbc.update(
            """
            insert into activity_completion_result(activity_id,task_id,user_id,revision,state,reason) values (?,?,?,?,?,?)
            on conflict(activity_id) do update set revision=excluded.revision,state=excluded.state,reason=excluded.reason
            """,
            id,
            taskId,
            e.userId(),
            e.aggregateVersion(),
            state,
            reason
        );
        outbox.append(
            "task",
            taskId.toString(),
            new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.ACTIVITY_SYNC_RESULT,
                1,
                e.aggregateVersion(),
                Instant.now(),
                e.userId(),
                new ActivitySyncResult(id, taskId, state, reason)
            )
        );
    }
}
