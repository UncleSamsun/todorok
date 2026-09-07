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

    public TemplateController(TemplateService templates) {
        this.templates = templates;
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
