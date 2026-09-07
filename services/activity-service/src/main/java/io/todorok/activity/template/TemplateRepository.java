package io.todorok.activity.template;

import io.todorok.activity.api.model.TemplateDomain;
import io.todorok.activity.api.model.TemplateFieldType;
import io.todorok.activity.api.model.TemplateKind;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

@Repository
public class TemplateRepository {
    private static final String SNAPSHOT_COLUMNS = """
        select p.id,p.user_id,p.domain,p.kind,p.current_version,p.revision,
               p.archived_at,p.created_at,v.name,d.field_id,d.name as field_name,
               d.type as field_type,d.unit,d.position
        from %s p
        join template_version v on v.template_id=p.id and v.version=p.current_version
        left join template_field_definition d
          on d.template_id=v.template_id and d.version=v.version
        """;

    private final JdbcTemplate jdbc;

    public TemplateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    void lockCommand(UUID owner, UUID commandId) {
        jdbc.queryForList(
            "select pg_advisory_xact_lock(hashtextextended(?,0))",
            owner + ":template-command:" + commandId
        );
    }

    void lockFieldIds(List<UUID> fieldIds) {
        fieldIds.stream().distinct().sorted().forEach(fieldId -> jdbc.queryForList(
            "select pg_advisory_xact_lock(hashtextextended(?,0))",
            "template-field:" + fieldId
        ));
    }

    Optional<CommandResult> findCommand(UUID owner, UUID commandId) {
        return jdbc.query(
            "select fingerprint,status_code,response_json::text from template_management_command where user_id=? and command_id=?",
            result -> result.next()
                ? Optional.of(new CommandResult(
                    result.getString("fingerprint"),
                    result.getInt("status_code"),
                    result.getString("response_json")
                ))
                : Optional.empty(),
            owner,
            commandId
        );
    }

    void saveCommand(
        UUID owner,
        UUID commandId,
        String fingerprint,
        int statusCode,
        String responseJson
    ) {
        jdbc.update(
            """
            insert into template_management_command(user_id,command_id,fingerprint,status_code,response_json)
            values (?,?,?,?,cast(? as jsonb))
            """,
            owner,
            commandId,
            fingerprint,
            statusCode,
            responseJson
        );
    }

    void insertTemplate(RecordTemplate template) {
        jdbc.update(
            """
            insert into record_template(id,user_id,domain,kind,current_version,revision,archived_at,created_at)
            values (?,?,?,?,?,?,?,?)
            """,
            template.id(),
            template.ownerId(),
            template.domain().name(),
            template.kind().name(),
            template.currentVersion(),
            template.revision(),
            template.archivedAt(),
            template.createdAt()
        );
    }

    void insertVersion(UUID templateId, long version, String name) {
        jdbc.update(
            "insert into template_version(template_id,version,name) values (?,?,?)",
            templateId,
            version,
            name
        );
    }

    void insertIdentities(UUID templateId, long version, List<FieldDefinition> fields) {
        for (var field : fields) jdbc.update(
            """
            insert into template_field_identity(template_id,field_id,created_version,type)
            values (?,?,?,?)
            """,
            templateId,
            field.fieldId(),
            version,
            field.type().name()
        );
    }

    void insertDefinitions(UUID templateId, long version, List<FieldDefinition> fields) {
        for (var field : fields) jdbc.update(
            """
            insert into template_field_definition(template_id,version,field_id,name,type,unit,position)
            values (?,?,?,?,?,?,?)
            """,
            templateId,
            version,
            field.fieldId(),
            field.name(),
            field.type().name(),
            field.unit(),
            field.position()
        );
    }

    Optional<RecordTemplate> lockOwned(UUID owner, UUID templateId) {
        return jdbc.query(
            "select * from record_template where user_id=? and id=? for update",
            result -> result.next() ? Optional.of(recordTemplate(result)) : Optional.empty(),
            owner,
            templateId
        );
    }

    List<TemplateFieldIdentity> identities(List<UUID> fieldIds) {
        if (fieldIds.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(fieldIds.size(), "?"));
        return jdbc.query(
            "select template_id,field_id,created_version,type from template_field_identity where field_id in (" + placeholders + ")",
            (result, row) -> new TemplateFieldIdentity(
                result.getObject("template_id", UUID.class),
                result.getObject("field_id", UUID.class),
                result.getLong("created_version"),
                TemplateFieldType.valueOf(result.getString("type"))
            ),
            fieldIds.toArray()
        );
    }

    List<FieldDefinition> currentFields(UUID templateId, long version) {
        return jdbc.query(
            """
            select field_id,name as field_name,type as field_type,unit,position
            from template_field_definition
            where template_id=? and version=?
            order by position
            """,
            (result, row) -> field(result),
            templateId,
            version
        );
    }

    void advanceVersion(UUID templateId, long version, long revision) {
        jdbc.update(
            "update record_template set current_version=?,revision=? where id=?",
            version,
            revision,
            templateId
        );
    }

    void archive(UUID templateId, long revision, OffsetDateTime archivedAt) {
        jdbc.update(
            "update record_template set revision=?,archived_at=? where id=?",
            revision,
            archivedAt,
            templateId
        );
    }

    Optional<TemplateSnapshot> findOwned(UUID owner, UUID templateId) {
        String sql = SNAPSHOT_COLUMNS.formatted("record_template")
            + " where p.user_id=? and p.id=? order by d.position";
        return jdbc.query(sql, (ResultSetExtractor<Optional<TemplateSnapshot>>)
            result -> snapshots(result).stream().findFirst(), owner, templateId);
    }

    Optional<TemplateVersion> findVersion(UUID owner, UUID templateId, long version) {
        return jdbc.query(
            """
            select v.template_id,v.version,v.name,d.field_id,d.name as field_name,
                   d.type as field_type,d.unit,d.position
            from template_version v
            join record_template r on r.id=v.template_id and r.user_id=?
            left join template_field_definition d
              on d.template_id=v.template_id and d.version=v.version
            where v.template_id=? and v.version=?
            order by d.position
            """,
            result -> {
                UUID id = null;
                String name = null;
                var fields = new ArrayList<FieldDefinition>();
                while (result.next()) {
                    id = result.getObject("template_id", UUID.class);
                    name = result.getString("name");
                    if (result.getObject("field_id") != null) fields.add(field(result));
                }
                return id == null ? Optional.empty() : Optional.of(new TemplateVersion(id, version, name, fields));
            },
            owner,
            templateId,
            version
        );
    }

    List<TemplateSnapshot> listOwned(
        UUID owner,
        TemplateDomain domain,
        TemplateKind kind,
        boolean includeArchived,
        OffsetDateTime beforeCreatedAt,
        UUID beforeId,
        int limit
    ) {
        var conditions = new ArrayList<String>();
        var arguments = new ArrayList<Object>();
        conditions.add("r.user_id=?");
        arguments.add(owner);
        if (domain != null) {
            conditions.add("r.domain=?");
            arguments.add(domain.name());
        }
        if (kind != null) {
            conditions.add("r.kind=?");
            arguments.add(kind.name());
        }
        if (!includeArchived) conditions.add("r.archived_at is null");
        if (beforeCreatedAt != null) {
            conditions.add("(r.created_at,r.id) < (?,?)");
            arguments.add(beforeCreatedAt);
            arguments.add(beforeId);
        }
        arguments.add(limit);
        String page = """
            (select r.* from record_template r
             where %s
             order by r.created_at desc,r.id desc
             limit ?)
            """.formatted(String.join(" and ", conditions));
        String sql = SNAPSHOT_COLUMNS.formatted(page)
            + " order by p.created_at desc,p.id desc,d.position";
        return jdbc.query(sql, this::snapshots, arguments.toArray());
    }

    private List<TemplateSnapshot> snapshots(ResultSet result) throws SQLException {
        var grouped = new LinkedHashMap<UUID, SnapshotBuilder>();
        while (result.next()) {
            UUID id = result.getObject("id", UUID.class);
            var builder = grouped.get(id);
            if (builder == null) {
                builder = new SnapshotBuilder(recordTemplate(result), result.getString("name"));
                grouped.put(id, builder);
            }
            if (result.getObject("field_id") != null) builder.fields.add(field(result));
        }
        return grouped.values().stream().map(SnapshotBuilder::build).toList();
    }

    private RecordTemplate recordTemplate(ResultSet result) throws SQLException {
        return new RecordTemplate(
            result.getObject("id", UUID.class),
            result.getObject("user_id", UUID.class),
            TemplateDomain.valueOf(result.getString("domain")),
            TemplateKind.valueOf(result.getString("kind")),
            result.getLong("current_version"),
            result.getLong("revision"),
            result.getObject("archived_at", OffsetDateTime.class),
            result.getObject("created_at", OffsetDateTime.class)
        );
    }

    private FieldDefinition field(ResultSet result) throws SQLException {
        return new FieldDefinition(
            result.getObject("field_id", UUID.class),
            result.getString("field_name"),
            TemplateFieldType.valueOf(result.getString("field_type")),
            result.getString("unit"),
            result.getInt("position")
        );
    }

    record CommandResult(String fingerprint, int statusCode, String responseJson) {}
    record TemplateSnapshot(RecordTemplate template, TemplateVersion version) {}

    private static final class SnapshotBuilder {
        private final RecordTemplate template;
        private final String name;
        private final List<FieldDefinition> fields = new ArrayList<>();

        private SnapshotBuilder(RecordTemplate template, String name) {
            this.template = template;
            this.name = name;
        }

        private TemplateSnapshot build() {
            return new TemplateSnapshot(
                template,
                new TemplateVersion(template.id(), template.currentVersion(), name, fields)
            );
        }
    }
}
