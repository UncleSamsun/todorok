package io.todorok.activity.template;

import io.todorok.activity.api.model.ArchiveTemplateRequest;
import io.todorok.activity.api.model.CreateTemplateRequest;
import io.todorok.activity.api.model.CreateTemplateVersionRequest;
import io.todorok.activity.api.model.FieldDefinitionInput;
import io.todorok.activity.api.model.TemplateDomain;
import io.todorok.activity.api.model.TemplateFieldType;
import io.todorok.activity.api.model.TemplateKind;
import io.todorok.activity.api.model.TemplatePageResponse;
import io.todorok.activity.api.model.TemplateResponse;
import io.todorok.web.ApiFailure;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class TemplateService {
    private static final String DEFAULT_TIME_UNIT = "초";
    private static final HashSet<String> TIME_UNITS = new HashSet<>(List.of("초", "분", "시간"));

    private final TemplateRepository templates;
    private final ObjectMapper mapper;
    private final Clock clock;

    public TemplateService(TemplateRepository templates, ObjectMapper mapper, Clock clock) {
        this.templates = templates;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public TemplateResponse create(UUID owner, CreateTemplateRequest request) {
        validateDomainKind(request.getDomain(), request.getKind());
        String name = validateName(request.getName(), "name");
        List<FieldDefinition> fields = validateFields(request.getFields());
        String fingerprint = fingerprint("CREATE", null, request);
        templates.lockCommand(owner, request.getCommandId());
        var replay = replay(owner, request.getCommandId(), fingerprint);
        if (replay != null) return replay;

        templates.lockFieldIds(fields.stream().map(FieldDefinition::fieldId).toList());
        if (!templates.identities(fields.stream().map(FieldDefinition::fieldId).toList()).isEmpty()) {
            throw validation("FIELD_UNKNOWN", "fields", "Each new fieldId must be globally unused.");
        }

        UUID templateId = UUID.randomUUID();
        var createdAt = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        templates.insertTemplate(new RecordTemplate(
            templateId,
            owner,
            request.getDomain(),
            request.getKind(),
            1,
            0,
            null,
            createdAt
        ));
        templates.insertVersion(templateId, 1, name);
        templates.insertIdentities(templateId, 1, fields);
        templates.insertDefinitions(templateId, 1, fields);
        TemplateResponse response = get(owner, templateId);
        templates.saveCommand(owner, request.getCommandId(), fingerprint, 201, mapper.writeValueAsString(response));
        return response;
    }

    public TemplateResponse get(UUID owner, UUID templateId) {
        return templates.findOwned(owner, templateId)
            .map(this::response)
            .orElseThrow(this::notFound);
    }

    public io.todorok.activity.api.model.TemplateVersion getVersion(
        UUID owner,
        UUID templateId,
        long version
    ) {
        return templates.findVersion(owner, templateId, version)
            .map(this::versionResponse)
            .orElseThrow(this::notFound);
    }

    public TemplatePageResponse list(
        UUID owner,
        TemplateDomain domain,
        TemplateKind kind,
        boolean includeArchived,
        String cursor,
        int limit
    ) {
        if (limit < 1 || limit > 100) throw validation(
            "VALIDATION_FAILED",
            "limit",
            "limit must be between 1 and 100."
        );
        if (domain != null && kind != null) validateDomainKind(domain, kind);
        Cursor boundary = decodeCursor(cursor, domain, kind, includeArchived);
        var snapshots = templates.listOwned(
            owner,
            domain,
            kind,
            includeArchived,
            boundary == null ? null : boundary.createdAt(),
            boundary == null ? null : boundary.id(),
            limit + 1
        );
        var page = new TemplatePageResponse(
            snapshots.stream().limit(limit).map(this::response).toList()
        );
        if (snapshots.size() > limit) {
            RecordTemplate last = snapshots.get(limit - 1).template();
            page.nextCursor(encodeCursor(new Cursor(
                last.createdAt(),
                last.id(),
                domain,
                kind,
                includeArchived
            )));
        }
        return page;
    }

    @Transactional
    public TemplateResponse createVersion(
        UUID owner,
        UUID templateId,
        CreateTemplateVersionRequest request
    ) {
        String name = validateName(request.getName(), "name");
        List<FieldDefinition> fields = validateFields(request.getFields());
        String fingerprint = fingerprint("VERSION", templateId, request);
        templates.lockCommand(owner, request.getCommandId());
        var replay = replay(owner, request.getCommandId(), fingerprint);
        if (replay != null) return replay;

        RecordTemplate template = templates.lockOwned(owner, templateId).orElseThrow(this::notFound);
        if (template.archivedAt() != null) throw conflict(
            "TEMPLATE_ARCHIVED",
            "Archived templates cannot receive new versions."
        );
        if (template.revision() != request.getExpectedRevision()) throw conflict(
            "TEMPLATE_VERSION_CONFLICT",
            "Reload the template before saving this version."
        );

        templates.lockFieldIds(fields.stream().map(FieldDefinition::fieldId).toList());
        var identities = new HashMap<UUID, TemplateFieldIdentity>();
        for (var identity : templates.identities(fields.stream().map(FieldDefinition::fieldId).toList())) {
            identities.put(identity.fieldId(), identity);
        }
        var currentFields = new HashMap<UUID, FieldDefinition>();
        for (var field : templates.currentFields(templateId, template.currentVersion())) {
            currentFields.put(field.fieldId(), field);
        }
        var newFields = new ArrayList<FieldDefinition>();
        for (var field : fields) {
            var identity = identities.get(field.fieldId());
            if (identity == null) {
                newFields.add(field);
                continue;
            }
            var current = currentFields.get(field.fieldId());
            if (!templateId.equals(identity.templateId()) || current == null) throw validation(
                "FIELD_UNKNOWN",
                "fields",
                "A fieldId belongs to another template or was removed from the current version."
            );
            if (current.type() != field.type()) throw validation(
                "FIELD_TYPE_IMMUTABLE",
                "fields",
                "Changing a field type requires a new fieldId."
            );
        }

        long nextVersion = template.currentVersion() + 1;
        long nextRevision = template.revision() + 1;
        templates.insertVersion(templateId, nextVersion, name);
        templates.insertIdentities(templateId, nextVersion, newFields);
        templates.insertDefinitions(templateId, nextVersion, fields);
        templates.advanceVersion(templateId, nextVersion, nextRevision);
        TemplateResponse response = get(owner, templateId);
        templates.saveCommand(owner, request.getCommandId(), fingerprint, 201, mapper.writeValueAsString(response));
        return response;
    }

    @Transactional
    public TemplateResponse archive(UUID owner, UUID templateId, ArchiveTemplateRequest request) {
        String fingerprint = fingerprint("ARCHIVE", templateId, request);
        templates.lockCommand(owner, request.getCommandId());
        var replay = replay(owner, request.getCommandId(), fingerprint);
        if (replay != null) return replay;

        RecordTemplate template = templates.lockOwned(owner, templateId).orElseThrow(this::notFound);
        if (template.archivedAt() != null) throw conflict(
            "TEMPLATE_ARCHIVED",
            "The template is already archived."
        );
        if (template.revision() != request.getExpectedRevision()) throw conflict(
            "TEMPLATE_VERSION_CONFLICT",
            "Reload the template before archiving it."
        );
        templates.archive(
            templateId,
            template.revision() + 1,
            OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
        );
        TemplateResponse response = get(owner, templateId);
        templates.saveCommand(owner, request.getCommandId(), fingerprint, 200, mapper.writeValueAsString(response));
        return response;
    }

    private TemplateResponse replay(UUID owner, UUID commandId, String fingerprint) {
        var previous = templates.findCommand(owner, commandId);
        if (previous.isEmpty()) return null;
        if (!previous.get().fingerprint().equals(fingerprint)) throw conflict(
            "COMMAND_REUSE",
            "A commandId cannot be reused with a different request."
        );
        return mapper.readValue(previous.get().responseJson(), TemplateResponse.class);
    }

    private List<FieldDefinition> validateFields(List<FieldDefinitionInput> inputs) {
        if (inputs == null) throw validation("VALIDATION_FAILED", "fields", "fields is required.");
        var ids = new HashSet<UUID>();
        var fields = new ArrayList<FieldDefinition>();
        for (int position = 0; position < inputs.size(); position++) {
            FieldDefinitionInput input = inputs.get(position);
            if (input == null || input.getFieldId() == null || input.getType() == null) throw validation(
                "VALIDATION_FAILED",
                "fields[" + position + "]",
                "fieldId, name and type are required."
            );
            if (!ids.add(input.getFieldId())) throw validation(
                "FIELD_ID_DUPLICATE",
                "fields[" + position + "].fieldId",
                "fieldId must be unique within a version."
            );
            String name = validateName(input.getName(), "fields[" + position + "].name");
            String unit = normalizeUnit(input.getType(), input.getUnit(), position);
            fields.add(new FieldDefinition(input.getFieldId(), name, input.getType(), unit, position));
        }
        return fields;
    }

    private String validateName(String name, String path) {
        if (name == null || name.isBlank() || name.length() > 120) throw validation(
            "VALIDATION_FAILED",
            path,
            "Name must contain non-whitespace text and be at most 120 characters."
        );
        return name;
    }

    private String normalizeUnit(TemplateFieldType type, String unit, int position) {
        String path = "fields[" + position + "].unit";
        if (unit != null && unit.length() > 40) throw validation(
            "VALIDATION_FAILED",
            path,
            "Unit must be at most 40 characters."
        );
        return switch (type) {
            case NUMBER -> unit;
            case TIME -> {
                if (unit == null) yield DEFAULT_TIME_UNIT;
                if (!TIME_UNITS.contains(unit)) throw validation(
                    "VALIDATION_FAILED",
                    path,
                    "Time unit must be 초, 분 or 시간."
                );
                yield unit;
            }
            case SHORT_TEXT, CHECK, MEMO -> {
                if (unit != null) throw validation(
                    "VALIDATION_FAILED",
                    path,
                    "This field type does not accept a unit."
                );
                yield null;
            }
            default -> throw validation("VALIDATION_FAILED", path, "Unknown field type.");
        };
    }

    private void validateDomainKind(TemplateDomain domain, TemplateKind kind) {
        boolean valid = domain != null && kind != null && switch (domain) {
            case STUDY -> kind == TemplateKind.STUDY_CATEGORY;
            case WORKOUT -> kind == TemplateKind.FREE_WORKOUT;
            case CLIMBING -> kind == TemplateKind.FREE_HANGBOARD || kind == TemplateKind.CLIMBING_SESSION;
            default -> false;
        };
        if (!valid) throw validation(
            "VALIDATION_FAILED",
            "kind",
            "kind is not valid for domain."
        );
    }

    private TemplateResponse response(TemplateRepository.TemplateSnapshot snapshot) {
        var template = snapshot.template();
        return new TemplateResponse(
            template.id(),
            template.domain(),
            template.kind(),
            template.archivedAt() != null,
            template.revision(),
            versionResponse(snapshot.version())
        );
    }

    private io.todorok.activity.api.model.TemplateVersion versionResponse(TemplateVersion version) {
        return new io.todorok.activity.api.model.TemplateVersion(
            version.templateId(),
            version.version(),
            version.name(),
            version.fields().stream().map(field ->
                new io.todorok.activity.api.model.FieldDefinition(
                    field.fieldId(),
                    field.name(),
                    field.type(),
                    field.position()
                ).unit(field.unit())
            ).toList()
        );
    }

    private String fingerprint(String operation, UUID templateId, Object request) {
        try {
            var envelope = mapper.createObjectNode();
            envelope.put("operation", operation);
            if (templateId != null) envelope.put("templateId", templateId.toString());
            envelope.set("request", mapper.valueToTree(request));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                canonical(envelope).getBytes(StandardCharsets.UTF_8)
            ));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String canonical(JsonNode node) {
        if (node.isObject()) {
            var keys = new TreeSet<String>();
            node.propertyNames().forEach(keys::add);
            return "{" + keys.stream().map(key -> mapper.writeValueAsString(key) + ":" + canonical(node.get(key)))
                .collect(java.util.stream.Collectors.joining(",")) + "}";
        }
        if (node.isArray()) {
            var values = new ArrayList<String>();
            node.forEach(value -> values.add(canonical(value)));
            return "[" + String.join(",", values) + "]";
        }
        return node.toString();
    }

    private String encodeCursor(Cursor cursor) {
        String value = String.join(
            "|",
            "1",
            cursor.createdAt().toString(),
            cursor.id().toString(),
            cursor.domain() == null ? "-" : cursor.domain().name(),
            cursor.kind() == null ? "-" : cursor.kind().name(),
            Boolean.toString(cursor.includeArchived())
        );
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decodeCursor(
        String value,
        TemplateDomain domain,
        TemplateKind kind,
        boolean includeArchived
    ) {
        if (value == null) return null;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 6 || !"1".equals(parts[0])) throw new IllegalArgumentException();
            var cursor = new Cursor(
                OffsetDateTime.parse(parts[1]),
                UUID.fromString(parts[2]),
                "-".equals(parts[3]) ? null : TemplateDomain.valueOf(parts[3]),
                "-".equals(parts[4]) ? null : TemplateKind.valueOf(parts[4]),
                switch (parts[5]) {
                    case "true" -> true;
                    case "false" -> false;
                    default -> throw new IllegalArgumentException();
                }
            );
            if (cursor.domain() != domain || cursor.kind() != kind
                || cursor.includeArchived() != includeArchived) throw new IllegalArgumentException();
            return cursor;
        } catch (RuntimeException invalid) {
            throw validation("INVALID_CURSOR", "cursor", "Use the cursor returned for the same filters.");
        }
    }

    private ApiFailure notFound() {
        return new ApiFailure(404, "NOT_FOUND", "Not found", "Template was not found.", false);
    }

    private ApiFailure conflict(String code, String detail) {
        return new ApiFailure(409, code, "Conflict", detail, false);
    }

    private ApiFailure validation(String code, String field, String message) {
        return new ApiFailure(
            400,
            code,
            "Validation failed",
            "One or more template fields are invalid.",
            false,
            List.of(new ApiFailure.FieldError(field, code, message))
        );
    }

    private record Cursor(
        OffsetDateTime createdAt,
        UUID id,
        TemplateDomain domain,
        TemplateKind kind,
        boolean includeArchived
    ) {}
}
