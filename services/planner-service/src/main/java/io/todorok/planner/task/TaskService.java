package io.todorok.planner.task;

import io.todorok.contracts.EventEnvelope;
import io.todorok.contracts.EventType;
import io.todorok.contracts.events.TaskChanged;
import io.todorok.contracts.events.TaskScheduled;
import io.todorok.messaging.OutboxEventWriter;
import io.todorok.planner.api.model.CalendarDaySummary;
import io.todorok.planner.api.model.CalendarSummaryResponse;
import io.todorok.planner.api.model.CategoryProgress;
import io.todorok.planner.api.model.CreateTaskRequest;
import io.todorok.planner.api.model.DayDetailResponse;
import io.todorok.planner.api.model.TaskResponse;
import io.todorok.planner.api.model.TaskStatus;
import io.todorok.planner.api.model.TaskType;
import io.todorok.planner.api.model.UpdateTaskRequest;
import io.todorok.web.ApiFailure;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository tasks;
    private final OutboxEventWriter outbox;

    public TaskService(TaskRepository tasks, OutboxEventWriter outbox) {
        this.tasks = tasks;
        this.outbox = outbox;
    }

    public TaskResponse detail(UUID owner, UUID id) {
        return tasks.detail(owner, id).orElseThrow(TaskService::missing);
    }

    public DayDetailResponse day(UUID owner, LocalDate date) {
        return new DayDetailResponse(date, tasks.day(owner, date));
    }

    public CalendarSummaryResponse range(UUID owner, LocalDate from, LocalDate to) {
        long difference = ChronoUnit.DAYS.between(from, to);
        if (difference < 0 || difference > 41) throw new ApiFailure(
            400,
            "INVALID_RANGE",
            "Invalid range",
            "Choose between 1 and 42 days.",
            false
        );
        var counts = tasks.counts(owner, from, to);
        var days = new ArrayList<CalendarDaySummary>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            var progress = new ArrayList<CategoryProgress>();
            int total = 0,
                completed = 0;
            for (TaskType type : TaskType.values()) {
                int n = 0,
                    c = 0;
                for (var row : counts)
                    if (row.getDate().equals(date) && row.getType() == type) {
                        n = Math.toIntExact(row.getTotal());
                        c = Math.toIntExact(row.getCompleted());
                        break;
                    }
                progress.add(new CategoryProgress(type, n, c));
                total += n;
                completed += c;
            }
            days.add(new CalendarDaySummary(date, total, completed, progress));
        }
        return new CalendarSummaryResponse(from, to, days);
    }

    @Transactional
    public TaskResponse create(UUID owner, CreateTaskRequest request) {
        var task = new Task(
            owner,
            title(request.getTitle()),
            request.getTaskType(),
            request.getScheduledDate()
        );
        tasks.saveAndFlush(task);
        publish(task, "CREATED");
        return task.response();
    }

    @Transactional
    public TaskResponse update(UUID owner, UUID id, UpdateTaskRequest request) {
        Task task = owned(owner, id, request.getVersion());
        String title = title(request.getTitle());
        if (
            task.title.equals(title) && task.scheduledDate.equals(request.getScheduledDate())
        ) return task.response();
        task.title = title;
        task.scheduledDate = request.getScheduledDate();
        tasks.flush();
        publish(task, "UPDATED");
        return task.response();
    }

    @Transactional
    public TaskResponse state(UUID owner, UUID id, long version, String command) {
        Task task = owned(owner, id, version);
        if (!command.equals("DELETED") && task.taskType != TaskType.GENERAL) throw conflict(
            "ACTIVITY_REQUIRED",
            "Change this task through its activity record."
        );
        if (command.equals("COMPLETED")) {
            if (task.status != TaskStatus.PLANNED) throw conflict(
                "INVALID_STATE",
                "Only planned tasks can be completed."
            );
            task.status = TaskStatus.COMPLETED;
        } else if (command.equals("REOPENED")) {
            if (
                task.status != TaskStatus.COMPLETED && task.status != TaskStatus.SKIPPED
            ) throw conflict("INVALID_STATE", "Only completed or skipped tasks can be reopened.");
            task.status = TaskStatus.PLANNED;
        } else if (command.equals("DELETED")) task.status = TaskStatus.DELETED;
        else throw new IllegalArgumentException("Unknown task command: " + command);
        tasks.flush();
        publish(task, command);
        return task.response();
    }

    private Task owned(UUID owner, UUID id, long version) {
        var task = tasks.owned(owner, id).orElseThrow(TaskService::missing);
        if (task.version != version) throw conflict(
            "VERSION_CONFLICT",
            "Reload the task before changing it."
        );
        return task;
    }

    private String title(String title) {
        if (title == null || title.isBlank() || title.length() > 120) throw new ApiFailure(
            400,
            "VALIDATION_FAILED",
            "Invalid title",
            "Enter a title of 1 to 120 characters.",
            false,
            List.of(
                new ApiFailure.FieldError(
                    "title",
                    "INVALID_TITLE",
                    "제목을 1~120자로 입력해 주세요."
                )
            )
        );
        return title.strip();
    }

    private static ApiFailure missing() {
        return new ApiFailure(404, "NOT_FOUND", "Not found", "Task was not found.", false);
    }

    private static ApiFailure conflict(String code, String detail) {
        return new ApiFailure(409, code, "Conflict", detail, false);
    }

    private void publish(Task task, String command) {
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
        outbox.append(
            "task",
            task.id.toString(),
            new EventEnvelope<>(
                UUID.randomUUID(),
                created ? EventType.TASK_SCHEDULED : EventType.TASK_CHANGED,
                1,
                task.version,
                Instant.now(),
                task.userId,
                payload
            )
        );
    }
}
