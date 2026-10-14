package io.todorok.contracts.events;

import java.time.Instant;
import java.util.UUID;

public record TaskReminderChanged(UUID taskId, Instant scheduledFor, boolean enabled) {}
