package io.todorok.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record ActivityCompleted(
    UUID activityId,
    UUID taskId,
    String activityType,
    Instant completedAt,
    String outcome,
    Instant startedAt,
    Instant endedAt
) {
    public ActivityCompleted(
        UUID activityId,
        UUID taskId,
        String activityType,
        Instant completedAt,
        String outcome
    ) {
        this(
            activityId,
            taskId,
            activityType,
            completedAt,
            outcome,
            null,
            null
        );
    }
}
