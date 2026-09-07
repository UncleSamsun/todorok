package io.todorok.activity.template;

import io.todorok.activity.api.TemplateApi;
import io.todorok.activity.api.model.ArchiveTemplateRequest;
import io.todorok.activity.api.model.CreateTemplateRequest;
import io.todorok.activity.api.model.CreateTemplateVersionRequest;
import io.todorok.activity.api.model.TemplateDomain;
import io.todorok.activity.api.model.TemplateKind;
import io.todorok.activity.api.model.TemplatePageResponse;
import io.todorok.activity.api.model.TemplateResponse;
import io.todorok.activity.api.model.TemplateVersion;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnWebApplication
public class TemplateController implements TemplateApi {
    private final TemplateService templates;
    private final TemplateBindingService bindings;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public TemplateController(TemplateService templates, TemplateBindingService bindings, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.templates = templates;
        this.bindings = bindings;
        this.jdbc = jdbc;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public ResponseEntity<io.todorok.activity.api.model.TaskRecordTemplateResponse> getTaskRecordTemplate(UUID taskId) {
        var rows = jdbc.queryForList("select * from task_reference where task_id=?", taskId);
        if (rows.isEmpty()) throw new io.todorok.web.ApiFailure(409, "TASK_NOT_READY", "Task not ready", "Retry after task projection arrives.", true);
        var row = rows.getFirst();
        if (!owner().equals(row.get("user_id"))) throw new io.todorok.web.ApiFailure(404, "NOT_FOUND", "Not found", "Task was not found.", false);
        var response = new io.todorok.activity.api.model.TaskRecordTemplateResponse(row.get("template_binding_id") != null);
        if (response.getLinked()) {
            var link = bindings.link((UUID) row.get("template_binding_id"));
            bindings.validate(owner(), taskId, (UUID) row.get("series_id"), (String) row.get("task_type"),
                link.getBindingId(), link.getTemplateId(), link.getSelectedTemplateVersion());
            response.templateLink(new io.todorok.activity.api.model.TemplateLink(link.getBindingId(), link.getTemplateId(),
                link.getSelectedTemplateVersion(), link.getName(), link.getFieldSummary()));
            response.template(templates.get(owner(), link.getTemplateId()));
        }
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TemplateResponse> createTemplate(CreateTemplateRequest request) {
        return ResponseEntity.status(201).body(templates.create(owner(), request));
    }

    @Override
    public ResponseEntity<TemplatePageResponse> listTemplates(
        TemplateDomain domain,
        TemplateKind kind,
        Boolean includeArchived,
        String cursor,
        Integer limit
    ) {
        return ResponseEntity.ok(templates.list(
            owner(),
            domain,
            kind,
            Boolean.TRUE.equals(includeArchived),
            cursor,
            limit == null ? 20 : limit
        ));
    }

    @Override
    public ResponseEntity<TemplateResponse> getTemplate(UUID templateId) {
        return ResponseEntity.ok(templates.get(owner(), templateId));
    }

    @Override
    public ResponseEntity<TemplateVersion> getTemplateVersion(UUID templateId, Long templateVersion) {
        return ResponseEntity.ok(templates.getVersion(owner(), templateId, templateVersion));
    }

    @Override
    public ResponseEntity<TemplateResponse> createTemplateVersion(
        UUID templateId,
        CreateTemplateVersionRequest request
    ) {
        return ResponseEntity.status(201).body(templates.createVersion(owner(), templateId, request));
    }

    @Override
    public ResponseEntity<TemplateResponse> archiveTemplate(
        UUID templateId,
        ArchiveTemplateRequest request
    ) {
        return ResponseEntity.ok(templates.archive(owner(), templateId, request));
    }

    private UUID owner() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
