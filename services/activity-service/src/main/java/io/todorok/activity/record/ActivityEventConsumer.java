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
    private final io.todorok.activity.template.TemplateBindingService bindings;

    public ActivityEventConsumer(
        JdbcTemplate jdbc,
        InboxEventGuard inbox,
        ObjectMapper mapper,
        io.todorok.activity.template.TemplateBindingService bindings
    ) {
        this.jdbc = jdbc;
        this.inbox = inbox;
        this.mapper = mapper;
        this.bindings = bindings;
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
        if (e.version()==2 && (!p.has("seriesId") || !p.has("templateLink")))
            throw new IllegalArgumentException("Incomplete v2 task identity");
        java.util.UUID series = e.version()==2 && p.hasNonNull("seriesId") ? EventJson.uuid(p, "seriesId") : null;
        java.util.UUID binding = null, template = null;
        Long selectedVersion = null;
        if (e.version() == 2 && p.hasNonNull("templateLink")) {
            var link = p.get("templateLink");
            if (!link.isObject() || !link.propertyNames().equals(java.util.Set.of("bindingId","templateId","selectedTemplateVersion")))
                throw new IllegalArgumentException("Malformed template link");
            binding = EventJson.uuid(link, "bindingId"); template = EventJson.uuid(link, "templateId");
            if (!link.path("selectedTemplateVersion").isIntegralNumber() || !link.path("selectedTemplateVersion").canConvertToLong()
                || link.path("selectedTemplateVersion").asLong()<1) throw new IllegalArgumentException("Invalid selected template version");
            selectedVersion = link.get("selectedTemplateVersion").asLong();
            bindings.validate(e.userId(), task, series, type, binding, template, selectedVersion);
        }
        if (!inbox.claim(e.eventId(), e.type().name())) return;
        jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))", "task-reference:" + task);
        var existing = jdbc.queryForList("select * from task_reference where task_id=? for update", task);
        if (!existing.isEmpty()) {
            var old=existing.getFirst();
            if (!e.userId().equals(old.get("user_id")) || !type.equals(old.get("task_type"))
                || (binding!=null && old.get("template_binding_id")!=null && !binding.equals(old.get("template_binding_id")))
                || (e.version()==2 && (old.get("template_binding_id")!=null || old.get("series_id")!=null)
                    && !java.util.Objects.equals(series,old.get("series_id"))))
                throw new IllegalArgumentException("Task reference identity changed");
        }
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
        // A verified immutable identity may enrich an older v1 projection even when its state is newer.
        if (binding != null) jdbc.update("""
            update task_reference set series_id=?,template_binding_id=?,template_id=?,selected_template_version=?
            where task_id=? and template_binding_id is null
            """, series,binding,template,selectedVersion,task);
        else if (series != null) jdbc.update("update task_reference set series_id=? where task_id=? and series_id is null",series,task);
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
