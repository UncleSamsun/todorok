package io.todorok.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record ActivityCompleted(
        UUID activityId,
        UUID taskId,
        String activityType,
        Instant completedAt,
        String outcome) {}
