package io.todorok.activity.template;

import io.todorok.internal.api.model.*;
import io.todorok.web.ApiFailure;
import io.todorok.web.security.TemplateServiceTokens;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TemplateBindingService {
    private final JdbcTemplate jdbc;
    private final TemplateRepository templates;
    public TemplateBindingService(JdbcTemplate jdbc, TemplateRepository templates) {
        this.jdbc = jdbc; this.templates = templates;
    }

    @Transactional
    public TemplateLink approve(TemplateSelectionRequest r) {
        String fingerprint = TemplateServiceTokens.hash(String.join("|", r.getOwnerId().toString(), r.getRequestId().toString(),
            r.getTargetType().name(), r.getTargetId().toString(), r.getTaskType().name(), r.getTemplateId().toString(),
            r.getExpectedTemplateVersion().toString()));
        jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))", r.getOwnerId() + ":binding:" + r.getRequestId());
        var previous = jdbc.queryForList("select id,request_fingerprint from template_selection_binding where user_id=? and request_id=?",
            r.getOwnerId(), r.getRequestId());
        if (!previous.isEmpty()) {
            if (!fingerprint.equals(previous.getFirst().get("request_fingerprint"))) throw conflict("COMMAND_REUSE");
            return link((UUID) previous.getFirst().get("id"));
        }
        jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?,0))", "binding-target:" + r.getTargetType() + ":" + r.getTargetId());
        if (!jdbc.queryForList("select id from template_selection_binding where target_type=? and target_id=?", r.getTargetType().name(), r.getTargetId()).isEmpty())
            throw conflict("COMMAND_REUSE");
        var template = templates.lockOwned(r.getOwnerId(), r.getTemplateId()).orElseThrow(() ->
            new ApiFailure(404, "NOT_FOUND", "Not found", "Template was not found.", false));
        if (!template.domain().name().equals(r.getTaskType().name()))
            throw new ApiFailure(400, "VALIDATION_FAILED", "Invalid domain", "Template domain must match the task type.", false);
        if (template.archivedAt() != null) throw conflict("TEMPLATE_ARCHIVED");
        if (template.currentVersion() != r.getExpectedTemplateVersion()) throw conflict("TEMPLATE_VERSION_CONFLICT");
        UUID id = UUID.randomUUID();
        jdbc.update("""
            insert into template_selection_binding(id,user_id,template_id,selected_version,target_type,target_id,request_id,request_fingerprint)
            values (?,?,?,?,?,?,?,?)
            """, id, r.getOwnerId(), r.getTemplateId(), r.getExpectedTemplateVersion(), r.getTargetType().name(), r.getTargetId(), r.getRequestId(), fingerprint);
        return link(id);
    }

    public TemplateLink link(UUID id) {
        var row = jdbc.queryForMap("select * from template_selection_binding where id=?", id);
        var version = templates.findVersion((UUID) row.get("user_id"), (UUID) row.get("template_id"), ((Number) row.get("selected_version")).longValue()).orElseThrow();
        String summary = version.fields().stream().map(f -> f.name() + (f.unit() == null ? "" : " (" + f.unit() + ")"))
            .collect(java.util.stream.Collectors.joining(", "));
        if (summary.length() > 240) summary = summary.substring(0, 239) + "…";
        return new TemplateLink(id, version.templateId(), version.version(), version.name(), summary);
    }

    public void validate(UUID owner, UUID task, UUID series, String type, UUID binding, UUID template, long version) {
        Integer count = jdbc.queryForObject("""
            select count(*) from template_selection_binding b join record_template t on t.id=b.template_id
            where b.id=? and b.user_id=? and t.user_id=? and t.domain=? and b.template_id=? and b.selected_version=?
              and ((b.target_type='TASK' and b.target_id=? and cast(? as uuid) is null)
                or (b.target_type='SERIES' and b.target_id=?))
            """, Integer.class, binding, owner, owner, type, template, version, task, series, series);
        if (count != 1) throw new IllegalArgumentException("Invalid template binding identity");
    }

    private static ApiFailure conflict(String code) {
        return new ApiFailure(409, code, "Selection conflict", "The template selection could not be approved.", false);
    }
}
