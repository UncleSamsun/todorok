package io.todorok.activity.template;

import java.util.List;
import java.util.UUID;

record TemplateVersion(
    UUID templateId,
    long version,
    String name,
    List<FieldDefinition> fields
) {
    TemplateVersion {
        fields = List.copyOf(fields);
    }
}
