package io.todorok.contracts.events;

import java.time.LocalDate;
import java.util.UUID;

public record TaskScheduled(
        UUID taskId,
        String taskType,
        LocalDate scheduledDate,
        String status) {}
