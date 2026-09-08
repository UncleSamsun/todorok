package io.todorok.planner.task;

import io.todorok.contracts.EventType;
import io.todorok.messaging.InboxEventGuard;
import io.todorok.messaging.EventJson;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProgramSessionConsumer {
    private final ObjectMapper mapper; private final InboxEventGuard inbox; private final TaskRepository tasks; private final TaskEvents events;
    public ProgramSessionConsumer(ObjectMapper mapper, InboxEventGuard inbox, TaskRepository tasks, TaskEvents events) { this.mapper = mapper; this.inbox = inbox; this.tasks = tasks; this.events = events; }
    @KafkaListener(id="planner-program-session", topics="todorok.activity.v1", groupId="planner-program-session-v1", autoStartup="${todorok.messaging.enabled:false}")
    @Transactional
    public void receive(String json) {
        var event = EventJson.read(mapper, json);
        if (event.type() != EventType.PROGRAM_SESSION_REQUESTED) return;
        UUID taskId = EventJson.uuid(event.payload(), "taskId");
        if (!inbox.claim(event.eventId(), event.type().name())) return;
        var existing = tasks.findById(taskId);
        if (existing.isPresent()) {
            var task = existing.get();
            if (!task.userId.equals(event.userId()) || task.taskType != io.todorok.planner.api.model.TaskType.WORKOUT) throw new IllegalArgumentException("Program task identity changed");
            return;
        }
        var task = new Task(event.userId(), EventJson.text(event.payload(), "title"), io.todorok.planner.api.model.TaskType.WORKOUT, LocalDate.parse(EventJson.text(event.payload(), "scheduledDate")));
        task.id = taskId;
        task.note = "프로그램 세션";
        tasks.saveAndFlush(task);
        events.publish(task, "CREATED");
    }
}
