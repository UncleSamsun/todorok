package io.todorok.planner.series;

import io.todorok.contracts.*;
import io.todorok.messaging.OutboxEventWriter;
import io.todorok.planner.api.model.*;
import io.todorok.planner.task.*;
import io.todorok.web.ApiFailure;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SeriesService {

    private final SeriesRepository series;
    private final TaskRepository tasks;
    private final TaskEvents events;
    private final NextOccurrencePolicy policy;
    private final Clock clock;
    private final OutboxEventWriter outbox;

    public SeriesService(
        SeriesRepository series,
        TaskRepository tasks,
        TaskEvents events,
        NextOccurrencePolicy policy,
        Clock clock,
        OutboxEventWriter outbox
    ) {
        this.series = series;
        this.tasks = tasks;
        this.events = events;
        this.policy = policy;
        this.clock = clock;
        this.outbox = outbox;
    }

    public SeriesResponse detail(UUID owner, UUID id) {
        return series.findByUserIdAndId(owner, id).orElseThrow(SeriesService::missing).response();
    }

    @Transactional
    public SeriesResponse create(UUID owner, CreateSeriesRequest request) {
        var item = new TaskSeries(
            owner,
            title(request.getTitle()),
            request.getTaskType(),
            request.getStartDate()
        );
        configure(item, request.getRule(), request.getEndDate(), request.getNote());
        series.saveAndFlush(item);
        publish(item, "CREATED");
        generate(item, item.startDate.minusDays(1), "COMPLETED");
        return item.response();
    }

    @Transactional
    public SeriesResponse update(UUID owner, UUID id, UpdateSeriesRequest request) {
        var item = locked(owner, id, request.getVersion());
        if (item.archived) throw conflict("SERIES_ARCHIVED");
        var before = item.response();
        item.title = title(request.getTitle());
        configure(
            item,
            request.getRule(),
            request.getEndDate(),
            request.getNote() == null ? item.note : request.getNote()
        );
        if (!before.equals(item.response())) {
            series.flush();
            publish(item, "UPDATED");
        }
        return item.response();
    }

    @Transactional
    public SeriesResponse archive(UUID owner, UUID id, long version) {
        var item = locked(owner, id, version);
        if (!item.archived) {
            item.archived = true;
            series.flush();
            publish(item, "ARCHIVED");
        }
        return item.response();
    }

    /** Called in the task transaction, after the series lock and task status flush. */
    @Transactional
    public void advance(UUID owner, UUID id, LocalDate occurrence, String completion) {
        var item = series.lock(owner, id).orElseThrow(SeriesService::missing);
        generate(item, occurrence, completion);
    }

    private void generate(TaskSeries item, LocalDate after, String completion) {
        if (!policy.shouldGenerate(tasks.hasActive(item.id), item.archived, completion)) return;
        policy.next(item.rule(), item.startDate, item.endDate, after).ifPresent(date -> {
            var task = Task.occurrence(
                item.userId,
                item.id,
                item.title,
                item.taskType,
                item.note,
                date,
                policy.scheduledDate(date, LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))))
            );
            try {
                tasks.saveAndFlush(task);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                if (
                    e.getMostSpecificCause().getMessage().contains("task_series_one_planned")
                ) throw conflict("ACTIVE_OCCURRENCE_EXISTS");
                throw e;
            }
            events.publish(task, "CREATED");
        });
    }

    private TaskSeries locked(UUID owner, UUID id, long version) {
        var item = series.lock(owner, id).orElseThrow(SeriesService::missing);
        if (item.version != version) throw conflict("VERSION_CONFLICT");
        return item;
    }

    private void configure(
        TaskSeries item,
        io.todorok.planner.api.model.RecurrenceRule rule,
        LocalDate end,
        String note
    ) {
        if (end != null && end.isBefore(item.startDate)) throw invalid(
            "End date precedes start date."
        );
        if (
            rule.getFrequency() ==
                io.todorok.planner.api.model.RecurrenceRule.FrequencyEnum.WEEKLY &&
            rule.getWeekdays().isEmpty()
        ) throw invalid("Select at least one weekday.");
        item.rule(
            new RecurrenceRule(
                RecurrenceRule.Frequency.valueOf(rule.getFrequency().name()),
                rule.getInterval(),
                Set.copyOf(rule.getWeekdays()),
                rule.getMonthDay()
            )
        );
        item.endDate = end;
        item.note = note;
    }

    private String title(String value) {
        if (value.isBlank()) throw invalid("Enter a title.");
        return value.strip();
    }

    private static ApiFailure invalid(String message) {
        return new ApiFailure(400, "VALIDATION_FAILED", "Invalid series", message, false);
    }

    private static ApiFailure missing() {
        return new ApiFailure(404, "NOT_FOUND", "Not found", "Series was not found.", false);
    }

    private static ApiFailure conflict(String code) {
        return new ApiFailure(409, code, "Conflict", "Reload before changing the series.", false);
    }

    private void publish(TaskSeries item, String command) {
        outbox.append(
            "series",
            item.id.toString(),
            new EventEnvelope<>(
                UUID.randomUUID(),
                EventType.SERIES_CHANGED,
                1,
                item.version,
                clock.instant(),
                item.userId,
                Map.of("seriesId", item.id, "command", command)
            )
        );
    }
}
