package io.todorok.contracts.events;

import java.time.Instant;
import java.util.UUID;

/** Full planner snapshot, so a correction can safely precede its completion event. */
public record ActivityCorrected(
    UUID activityId, UUID taskId, String activityType, Instant completedAt,
    String outcome, Instant startedAt, Instant endedAt, Instant previousPerformedAt, String completionStatus
) {}
