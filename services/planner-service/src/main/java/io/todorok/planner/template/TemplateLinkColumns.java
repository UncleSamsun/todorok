package io.todorok.planner.template;

import io.todorok.planner.api.model.TemplateLink;
import jakarta.persistence.*;
import java.util.UUID;

@Embeddable
public class TemplateLinkColumns {
    @Column(name="template_binding_id", updatable=false) UUID bindingId;
    @Column(name="template_id", updatable=false) UUID templateId;
    @Column(name="selected_template_version", updatable=false) Long selectedVersion;
    @Column(name="template_name", updatable=false) String name;
    @Column(name="template_field_summary", updatable=false) String fieldSummary;
    protected TemplateLinkColumns() {}
    public TemplateLinkColumns(TemplateLink link) {
        bindingId=link.getBindingId(); templateId=link.getTemplateId(); selectedVersion=link.getSelectedTemplateVersion();
        name=link.getName(); fieldSummary=link.getFieldSummary();
    }
    public TemplateLink response() { return new TemplateLink(bindingId, templateId, selectedVersion, name, fieldSummary); }
}
