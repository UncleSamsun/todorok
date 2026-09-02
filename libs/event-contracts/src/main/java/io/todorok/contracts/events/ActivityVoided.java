package io.todorok.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record ActivityVoided(
        UUID activityId,
        UUID taskId,
        Instant voidedAt,
        String reason) {}
