package io.todorok.planner.series;

import io.todorok.planner.api.model.SeriesResponse;
import io.todorok.planner.api.model.TaskType;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "task_series", schema = "planner")
public class TaskSeries {

    @Id
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(nullable = false)
    String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    TaskType taskType;

    String note;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date")
    LocalDate endDate;

    @Enumerated(EnumType.STRING)
    RecurrenceRule.Frequency frequency;

    @Column(name = "repeat_interval")
    int interval;

    String weekdays;

    @Column(name = "month_day")
    int monthDay;

    boolean archived;

    @Version
    Long version;

    protected TaskSeries() {}

    @Embedded
    io.todorok.planner.template.TemplateLinkColumns templateLink;

    TaskSeries(UUID owner, String title, TaskType type, LocalDate start) {
        id = UUID.randomUUID();
        userId = owner;
        this.title = title;
        taskType = type;
        startDate = start;
    }

    void rule(RecurrenceRule rule) {
        frequency = rule.frequency();
        interval = rule.interval();
        monthDay = rule.monthDay();
        weekdays = rule
            .weekdays()
            .stream()
            .sorted()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }

    RecurrenceRule rule() {
        Set<Integer> days = weekdays.isEmpty()
            ? Set.of()
            : Arrays.stream(weekdays.split(",")).map(Integer::valueOf).collect(Collectors.toSet());
        return new RecurrenceRule(frequency, interval, days, monthDay);
    }

    SeriesResponse response() {
        var r = rule();
        return new SeriesResponse(
            id,
            userId,
            title,
            taskType,
            startDate,
            new io.todorok.planner.api.model.RecurrenceRule(
                io.todorok.planner.api.model.RecurrenceRule.FrequencyEnum.valueOf(frequency.name()),
                interval,
                r.weekdays(),
                monthDay
            ),
            archived,
            version
        )
            .endDate(endDate)
            .note(note)
            .templateLink(templateLink == null ? null : templateLink.response());
    }
}
