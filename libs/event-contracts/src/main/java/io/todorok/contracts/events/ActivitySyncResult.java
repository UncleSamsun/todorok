package io.todorok.contracts.events;

import java.util.UUID;

/** Planner acknowledgement, ordered by the Activity revision being acknowledged. */
public record ActivitySyncResult(
    UUID activityId,
    UUID taskId,
    String state,
    String reason
) {}
