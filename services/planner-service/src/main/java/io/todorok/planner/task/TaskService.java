package io.todorok.planner.task;

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
    private final TaskEvents events;
    private final io.todorok.planner.series.SeriesRepository series;
    private final io.todorok.planner.series.SeriesService recurrence;
    private final java.time.Clock clock;
    private final TaskTransitionPolicy transitions;
    private final io.todorok.planner.template.TemplateCreationCommands creation;

    public TaskService(
        TaskRepository tasks,
        TaskEvents events,
        io.todorok.planner.series.SeriesRepository series,
        io.todorok.planner.series.SeriesService recurrence,
        java.time.Clock clock,
        TaskTransitionPolicy transitions,
        io.todorok.planner.template.TemplateCreationCommands creation
    ) {
        this.tasks = tasks;
        this.events = events;
        this.series = series;
        this.recurrence = recurrence;
        this.clock = clock;
        this.transitions = transitions;
        this.creation = creation;
    }

    public TaskResponse detail(UUID owner, UUID id) {
        return tasks.detail(owner, id).orElseThrow(TaskService::missing).response();
    }

    public DayDetailResponse day(UUID owner, LocalDate date) {
        return new DayDetailResponse(
            date,
            tasks.day(owner, date).stream().map(TaskView::response).toList()
        );
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

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public TaskResponse create(UUID owner, CreateTaskRequest request) {
        String title = title(request.getTitle());
        return creation.create(owner, request.getCommandId(), "TASK", request.getTaskType(), request.getTemplateSelection(), request,
            TaskResponse.class, (target, link) -> {
        var task = new Task(
            owner,
            title,
            request.getTaskType(),
            request.getScheduledDate()
        );
        task.id = target;
        task.templateLink(link);
        task.note = request.getNote();
        tasks.saveAndFlush(task);
        publish(task, "CREATED");
        return task.response();
        });
    }

    @Transactional
    public TaskResponse update(UUID owner, UUID id, UpdateTaskRequest request) {
        Task task = owned(owner, id, request.getVersion());
        String title = title(request.getTitle());
        String note = request.getNote() == null ? task.note : request.getNote();
        if (
            task.title.equals(title) &&
            task.scheduledDate.equals(request.getScheduledDate()) &&
            java.util.Objects.equals(task.note, note)
        ) return task.response();
        task.title = title;
        task.note = note;
        task.scheduledDate = request.getScheduledDate();
        tasks.flush();
        publish(task, "UPDATED");
        return task.response();
    }

    @Transactional
    public TaskResponse state(UUID owner, UUID id, long version, String command) {
        Task task = owned(owner, id, version);
        task.status = transitions.next(
            task.taskType,
            task.status,
            command,
            task.seriesId != null && tasks.hasActive(task.seriesId)
        );
        if (command.equals("REOPENED")) {
            LocalDate today = LocalDate.now(clock.withZone(java.time.ZoneId.of("Asia/Seoul")));
            if (task.scheduledDate.isBefore(today)) task.scheduledDate = today;
        }
        tasks.flush();
        publish(task, command);
        if (task.seriesId != null) recurrence.advance(
            owner,
            task.seriesId,
            task.occurrenceDate,
            command
        );
        return task.response();
    }

    private Task owned(UUID owner, UUID id, long version) {
        tasks
            .seriesId(owner, id)
            .ifPresent(seriesId ->
                series.lock(owner, seriesId).orElseThrow(TaskService::missing)
            );
        var task = tasks.lock(owner, id).orElseThrow(TaskService::missing);
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
        events.publish(task, command);
    }
}
