package io.todorok.activity.template;

import io.todorok.activity.api.model.TemplateFieldType;
import java.util.UUID;

record FieldDefinition(
    UUID fieldId,
    String name,
    TemplateFieldType type,
    String unit,
    int position
) {}
