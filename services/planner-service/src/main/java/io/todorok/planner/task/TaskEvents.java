package io.todorok.planner.task;

import io.todorok.contracts.*;
import io.todorok.contracts.events.*;
import io.todorok.contracts.events.v2.*;
import io.todorok.messaging.OutboxEventWriter;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TaskEvents {

    private final OutboxEventWriter outbox;
    private final Clock clock;

    public TaskEvents(OutboxEventWriter outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    public void publish(Task task, String command) {
        boolean created = command.equals("CREATED");
        Object payload = created
            ? new TaskScheduledV2(
                  task.id,
                  task.taskType.name(),
                  task.scheduledDate,
                  task.status.name(), task.seriesId, link(task)
              )
            : command.equals("ROLLED_OVER")
              ? new TaskRolledOverV2(task.id, task.taskType.name(), task.scheduledDate, task.status.name(), command, task.seriesId, link(task))
              : new TaskChangedV2(
                  task.id,
                  task.taskType.name(),
                  task.scheduledDate,
                  task.status.name(),
                  command, task.seriesId, link(task)
              );
        EventType type = created
            ? EventType.TASK_SCHEDULED
            : command.equals("ROLLED_OVER")
              ? EventType.TASK_ROLLED_OVER
              : EventType.TASK_CHANGED;
        outbox.append(
            "task",
            task.id.toString(),
            new EventEnvelope<>(
                UUID.randomUUID(),
                type,
                2,
                task.version,
                clock.instant(),
                task.userId,
                payload
            )
        );
        if (task.scheduledTime != null) publishReminder(task);
    }

    public void publishReminder(Task task) {
        var scheduled = task.scheduledTime == null ? null : task.scheduledDate
            .atTime(java.time.LocalTime.parse(task.scheduledTime)).atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant();
        outbox.append("task-reminder", task.id.toString(), new EventEnvelope<>(UUID.randomUUID(),
            EventType.TASK_REMINDER_CHANGED, 1, task.version, clock.instant(), task.userId,
            new TaskReminderChanged(task.id, scheduled, task.reminderEnabled && task.status == io.todorok.planner.api.model.TaskStatus.PLANNED)));
    }

    private TemplateBindingLink link(Task task) {
        if (task.templateLink==null) return null;
        var link=task.templateLink.response();
        return new TemplateBindingLink(link.getBindingId(), link.getTemplateId(), link.getSelectedTemplateVersion());
    }
}
