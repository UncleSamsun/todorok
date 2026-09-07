package io.todorok.activity.template;

import io.todorok.activity.api.model.TemplateDomain;
import io.todorok.activity.api.model.TemplateKind;
import java.time.OffsetDateTime;
import java.util.UUID;

record RecordTemplate(
    UUID id,
    UUID ownerId,
    TemplateDomain domain,
    TemplateKind kind,
    long currentVersion,
    long revision,
    OffsetDateTime archivedAt,
    OffsetDateTime createdAt
) {}
