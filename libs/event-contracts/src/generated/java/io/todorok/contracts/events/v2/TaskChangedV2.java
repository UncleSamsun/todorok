package io.todorok.contracts.events.v2;

// Generated from contracts/events/task-changed/v2.schema.json.
public record TaskChangedV2(
    java.util.UUID taskId,
    String taskType,
    java.time.LocalDate scheduledDate,
    String status,
    String command,
    java.util.UUID seriesId,
    TemplateBindingLink templateLink
) {}
