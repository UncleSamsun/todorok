package io.todorok.activity.record;

import io.todorok.activity.api.model.*;
import io.todorok.web.ApiFailure;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

/** Uses only the Activity database. Caller owns the transaction and task/activity lock. */
final class ActivityTemplateRecords {
    private final JdbcTemplate jdbc;
    ActivityTemplateRecords(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    ActivityTemplateSnapshot current(UUID owner, Map<String,Object> reference, Long expectedVersion) {
        if (reference.get("template_binding_id") == null) {
            if (expectedVersion != null) throw invalid("VALIDATION_FAILED", "Unlinked tasks cannot specify expectedTemplateVersion.");
            return null;
        }
        if (expectedVersion == null) throw invalid("VALIDATION_FAILED", "Linked tasks require expectedTemplateVersion.");
        // Lock order: owner/command -> TaskReference -> template identity. Management never locks TaskReference.
        var rows = jdbc.queryForList("select * from record_template where id=? and user_id=? for update", reference.get("template_id"), owner);
        if (rows.isEmpty()) throw new ApiFailure(404, "NOT_FOUND", "Not found", "Template was not found.", false);
        var template = rows.getFirst();
        long version = ((Number) template.get("current_version")).longValue();
        if (version != expectedVersion) throw new ApiFailure(409, "TEMPLATE_VERSION_CONFLICT", "Template changed",
            "Review the current definition and preserve your previous input before retrying with a new command.", false);
        if (!reference.get("task_type").equals(template.get("domain"))) throw invalid("VALIDATION_FAILED", "Template domain must match the task.");
        // Binding is immutable and was verified by TaskReference ingestion. Archive only blocks new bindings.
        var fields = jdbc.query("select * from template_field_definition where template_id=? and version=? order by position",
            (r,n) -> new FieldDefinition(r.getObject("field_id", UUID.class), r.getString("name"),
                TemplateFieldType.valueOf(r.getString("type")), r.getInt("position")).unit(r.getString("unit")), reference.get("template_id"), version);
        return new ActivityTemplateSnapshot(ActivityTemplateSnapshot.SchemaVersionEnum.NUMBER_1, (UUID) template.get("id"), version,
            jdbc.queryForObject("select name from template_version where template_id=? and version=?", String.class, template.get("id"), version),
            TemplateDomain.valueOf((String) template.get("domain")), TemplateKind.valueOf((String) template.get("kind")), fields);
    }

    Map<String,Object> validate(ActivityDetail detail, ActivityTemplateSnapshot snapshot) {
        List<FieldInput> inputs = fields(detail, snapshot);
        if (snapshot == null && inputs != null && !inputs.isEmpty()) throw invalid("FIELD_UNKNOWN", "This activity has no field definition.");
        var values = new LinkedHashMap<String,Object>();
        if (inputs == null) return values;
        var definitions = new HashMap<UUID,FieldDefinition>();
        if (snapshot != null) for (var field : snapshot.getFields()) definitions.put(field.getFieldId(), field);
        var seen = new HashSet<UUID>();
        for (var input : inputs) {
            if (input == null || input.getFieldId() == null || input.getType() == null) throw invalid("FIELD_VALUE_INVALID", "Fields require an ID and type.");
            if (!seen.add(input.getFieldId())) throw invalid("FIELD_ID_DUPLICATE", "Each field may occur only once.");
            var field = definitions.get(input.getFieldId());
            if (field == null) throw invalid("FIELD_UNKNOWN", "The field does not belong to the recorded definition.");
            if (field.getType() != input.getType()) throw invalid("FIELD_VALUE_INVALID", "The field type does not match its definition.");
            long members = java.util.stream.Stream.of(input.getNumberValue(), input.getTimeSeconds(), input.getTextValue(), input.getChecked(), input.getMemoValue()).filter(Objects::nonNull).count();
            if (members != 1) throw invalid("FIELD_VALUE_INVALID", "Supply exactly one value matching the field type.");
            Object value = switch (input.getType()) {
                case NUMBER -> input.getNumberValue();
                case TIME -> input.getTimeSeconds();
                case SHORT_TEXT -> input.getTextValue();
                case CHECK -> input.getChecked();
                case MEMO -> input.getMemoValue();
            };
            if (value == null || value instanceof BigDecimal number && !Double.isFinite(number.doubleValue())
                || value instanceof Long seconds && (seconds < 0 || seconds > 9_007_199_254_740_991L)
                || value instanceof String text && text.length() > (input.getType() == TemplateFieldType.MEMO ? 20_000 : 120))
                throw invalid("FIELD_VALUE_INVALID", "The value is outside the field's type or range.");
            if (value instanceof String text && text.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) continue;
            values.put(input.getFieldId().toString(), value);
        }
        return values;
    }

    private List<FieldInput> fields(ActivityDetail detail, ActivityTemplateSnapshot snapshot) {
        if (detail == null) return null;
        if (snapshot == null) {
            if (detail.getStudy() != null && detail.getStudy().getFields() != null) return detail.getStudy().getFields();
            if (detail.getWorkout() != null && detail.getWorkout().getFields() != null) return detail.getWorkout().getFields();
            if (detail.getClimbing() != null && detail.getClimbing().getFields() != null) return detail.getClimbing().getFields();
            return null;
        }
        return switch (snapshot.getDomain()) {
            case STUDY -> detail.getStudy() == null ? null : detail.getStudy().getFields();
            case WORKOUT -> detail.getWorkout() == null ? null : detail.getWorkout().getFields();
            case CLIMBING -> detail.getClimbing() == null ? null : detail.getClimbing().getFields();
        };
    }

    static ApiFailure invalid(String code, String detail) { return new ApiFailure(400, code, "Invalid field value", detail, false); }
}
