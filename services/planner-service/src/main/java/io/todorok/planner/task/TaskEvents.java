package io.todorok.planner.task;

import io.todorok.contracts.*;
import io.todorok.contracts.events.*;
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
            ? new TaskScheduled(
                  task.id,
                  task.taskType.name(),
                  task.scheduledDate,
                  task.status.name()
              )
            : new TaskChanged(
                  task.id,
                  task.taskType.name(),
                  task.scheduledDate,
                  task.status.name(),
                  command
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
                1,
                task.version,
                clock.instant(),
                task.userId,
                payload
            )
        );
    }
}
