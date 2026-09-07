package io.todorok.contracts.events.v2;

// Generated from contracts/events/series-changed/v2.schema.json.
public record SeriesChangedV2(
    java.util.UUID seriesId,
    String command,
    TemplateBindingLink templateLink
) {}
