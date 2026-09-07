package io.todorok.planner.task;

import io.todorok.planner.api.CalendarApi;
import io.todorok.planner.api.TaskApi;
import io.todorok.planner.api.model.CalendarSummaryResponse;
import io.todorok.planner.api.model.CreateTaskRequest;
import io.todorok.planner.api.model.DayDetailResponse;
import io.todorok.planner.api.model.TaskResponse;
import io.todorok.planner.api.model.UpdateTaskRequest;
import io.todorok.planner.api.model.VersionCommand;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnWebApplication
public class TaskController implements TaskApi, CalendarApi {

    private final TaskService tasks;
    private final RolloverService rollover;

    public TaskController(TaskService tasks, RolloverService rollover) {
        this.tasks = tasks;
        this.rollover = rollover;
    }

    @Override
    public ResponseEntity<TaskResponse> skipTask(UUID id, VersionCommand body) {
        return ResponseEntity.ok(tasks.state(owner(), id, body.getVersion(), "SKIPPED"));
    }

    @Override
    public ResponseEntity<io.todorok.planner.api.model.RolloverResponse> rolloverTasks() {
        return ResponseEntity.ok(rollover.rollover(owner()));
    }

    private UUID owner() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Override
    public ResponseEntity<TaskResponse> createTask(CreateTaskRequest body) {
        return ResponseEntity.status(201).body(tasks.create(owner(), body));
    }

    @Override
    public ResponseEntity<TaskResponse> getTask(UUID id) {
        return ResponseEntity.ok(tasks.detail(owner(), id));
    }

    @Override
    public ResponseEntity<TaskResponse> updateTask(UUID id, UpdateTaskRequest body) {
        return ResponseEntity.ok(tasks.update(owner(), id, body));
    }

    @Override
    public ResponseEntity<TaskResponse> completeTask(UUID id, VersionCommand body) {
        return ResponseEntity.ok(tasks.state(owner(), id, body.getVersion(), "COMPLETED"));
    }

    @Override
    public ResponseEntity<TaskResponse> reopenTask(UUID id, VersionCommand body) {
        return ResponseEntity.ok(tasks.state(owner(), id, body.getVersion(), "REOPENED"));
    }

    @Override
    public ResponseEntity<Void> deleteTask(UUID id, Long version) {
        tasks.state(owner(), id, version, "DELETED");
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CalendarSummaryResponse> getCalendarSummary(
        LocalDate from,
        LocalDate to
    ) {
        return ResponseEntity.ok(tasks.range(owner(), from, to));
    }

    @Override
    public ResponseEntity<DayDetailResponse> getDayDetail(LocalDate date) {
        return ResponseEntity.ok(tasks.day(owner(), date));
    }
}
