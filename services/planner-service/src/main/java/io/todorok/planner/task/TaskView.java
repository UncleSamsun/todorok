package io.todorok.planner.task;

import io.todorok.planner.api.model.*;
import java.time.LocalDate;
import java.util.UUID;

public record TaskView(
    UUID id,
    UUID owner,
    String title,
    TaskType type,
    LocalDate scheduled,
    TaskStatus status,
    Long version,
    UUID series,
    LocalDate occurrence,
    String note,
    UUID activityId,
    java.time.OffsetDateTime performedAt,
    String completionSummary,
    java.time.OffsetDateTime startedAt,
    java.time.OffsetDateTime endedAt,
    io.todorok.planner.template.TemplateLinkColumns templateLink
) {
    TaskResponse response() {
        return new TaskResponse(
            id,
            owner,
            title,
            type,
            scheduled,
            status,
            version
        )
            .seriesId(series)
            .occurrenceDate(occurrence)
            .note(note)
            .activityId(activityId)
            .performedAt(performedAt)
            .completionSummary(completionSummary)
            .startedAt(startedAt)
            .endedAt(endedAt)
            .templateLink(templateLink == null ? null : templateLink.response());
    }
}
