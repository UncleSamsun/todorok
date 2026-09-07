package io.todorok.activity.template;

import io.todorok.activity.api.model.TemplateFieldType;
import java.util.UUID;

record TemplateFieldIdentity(
    UUID templateId,
    UUID fieldId,
    long createdVersion,
    TemplateFieldType type
) {}
