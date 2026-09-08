package io.todorok.planner.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.todorok.messaging.InboxEventGuard;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class ProgramSessionConsumerTest {
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final InboxEventGuard inbox = mock(InboxEventGuard.class);
    private final TaskEvents events = mock(TaskEvents.class);
    private final ProgramSessionConsumer consumer = new ProgramSessionConsumer(JsonMapper.builder().findAndAddModules().build(), inbox, tasks, events);
    private final UUID eventId = UUID.randomUUID(), owner = UUID.randomUUID(), taskId = UUID.randomUUID();

    @Test void createsOneFixedWorkoutTaskForTheFirstProgramSession() throws Exception {
        when(inbox.claim(eq(eventId), eq("PROGRAM_SESSION_REQUESTED"))).thenReturn(true);
        when(tasks.findById(taskId)).thenReturn(Optional.empty());
        consumer.receive(event());
        var captured = ArgumentCaptor.forClass(Task.class);
        verify(tasks).saveAndFlush(captured.capture());
        assertThat(captured.getValue().id).isEqualTo(taskId);
        assertThat(captured.getValue().userId).isEqualTo(owner);
        assertThat(captured.getValue().taskType).isEqualTo(io.todorok.planner.api.model.TaskType.WORKOUT);
        assertThat(captured.getValue().scheduledDate).isEqualTo(LocalDate.of(2026, 9, 8));
        verify(events).publish(captured.getValue(), "CREATED");
    }

    @Test void doesNotCreateOrPublishWhenTheFixedTaskAlreadyExists() throws Exception {
        var existing = new Task(owner, "세션", io.todorok.planner.api.model.TaskType.WORKOUT, LocalDate.of(2026, 9, 8)); existing.id = taskId;
        when(inbox.claim(eq(eventId), eq("PROGRAM_SESSION_REQUESTED"))).thenReturn(true);
        when(tasks.findById(taskId)).thenReturn(Optional.of(existing));
        consumer.receive(event());
        verify(tasks, never()).saveAndFlush(any());
        verify(events, never()).publish(any(), anyString());
    }

    private String event() throws Exception {
        return JsonMapper.builder().build().writeValueAsString(Map.of("eventId", eventId, "type", "PROGRAM_SESSION_REQUESTED", "version", 1, "aggregateVersion", 0, "occurredAt", "2026-09-08T01:00:00Z", "userId", owner,
            "payload", Map.of("enrollmentId", UUID.randomUUID(), "sessionId", UUID.randomUUID(), "taskId", taskId, "title", "합성 · 1주차 1회", "scheduledDate", "2026-09-08", "targetSets", List.of(3, 3, 2))));
    }
}
