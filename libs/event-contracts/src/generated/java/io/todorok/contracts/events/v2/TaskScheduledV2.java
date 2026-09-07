package io.todorok.contracts.events.v2;

// Generated from contracts/events/task-scheduled/v2.schema.json.
public record TaskScheduledV2(
    java.util.UUID taskId,
    String taskType,
    java.time.LocalDate scheduledDate,
    String status,
    java.util.UUID seriesId,
    TemplateBindingLink templateLink
) {}
