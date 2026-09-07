package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.todorok.activity.api.model.TemplateDomain;
import io.todorok.activity.api.model.TemplateKind;
import io.todorok.activity.api.model.TemplateVersion;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * TemplateResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class TemplateResponse {

  private UUID templateId;

  private TemplateDomain domain;

  private TemplateKind kind;

  private Boolean archived;

  private Long revision;

  private TemplateVersion currentVersion;

  public TemplateResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TemplateResponse(UUID templateId, TemplateDomain domain, TemplateKind kind, Boolean archived, Long revision, TemplateVersion currentVersion) {
    this.templateId = templateId;
    this.domain = domain;
    this.kind = kind;
    this.archived = archived;
    this.revision = revision;
    this.currentVersion = currentVersion;
  }

  public TemplateResponse templateId(UUID templateId) {
    this.templateId = templateId;
    return this;
  }

  /**
   * Get templateId
   * @return templateId
   */
  @NotNull @Valid
  @JsonProperty("templateId")
  public UUID getTemplateId() {
    return templateId;
  }

  @JsonProperty("templateId")
  public void setTemplateId(UUID templateId) {
    this.templateId = templateId;
  }

  public TemplateResponse domain(TemplateDomain domain) {
    this.domain = domain;
    return this;
  }

  /**
   * Get domain
   * @return domain
   */
  @NotNull @Valid
  @JsonProperty("domain")
  public TemplateDomain getDomain() {
    return domain;
  }

  @JsonProperty("domain")
  public void setDomain(TemplateDomain domain) {
    this.domain = domain;
  }

  public TemplateResponse kind(TemplateKind kind) {
    this.kind = kind;
    return this;
  }

  /**
   * Get kind
   * @return kind
   */
  @NotNull @Valid
  @JsonProperty("kind")
  public TemplateKind getKind() {
    return kind;
  }

  @JsonProperty("kind")
  public void setKind(TemplateKind kind) {
    this.kind = kind;
  }

  public TemplateResponse archived(Boolean archived) {
    this.archived = archived;
    return this;
  }

  /**
   * Get archived
   * @return archived
   */
  @NotNull
  @JsonProperty("archived")
  public Boolean getArchived() {
    return archived;
  }

  @JsonProperty("archived")
  public void setArchived(Boolean archived) {
    this.archived = archived;
  }

  public TemplateResponse revision(Long revision) {
    this.revision = revision;
    return this;
  }

  /**
   * Get revision
   * minimum: 0
   * @return revision
   */
  @NotNull @Min(value = 0L)
  @JsonProperty("revision")
  public Long getRevision() {
    return revision;
  }

  @JsonProperty("revision")
  public void setRevision(Long revision) {
    this.revision = revision;
  }

  public TemplateResponse currentVersion(TemplateVersion currentVersion) {
    this.currentVersion = currentVersion;
    return this;
  }

  /**
   * Get currentVersion
   * @return currentVersion
   */
  @NotNull @Valid
  @JsonProperty("currentVersion")
  public TemplateVersion getCurrentVersion() {
    return currentVersion;
  }

  @JsonProperty("currentVersion")
  public void setCurrentVersion(TemplateVersion currentVersion) {
    this.currentVersion = currentVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TemplateResponse templateResponse = (TemplateResponse) o;
    return Objects.equals(this.templateId, templateResponse.templateId) &&
        Objects.equals(this.domain, templateResponse.domain) &&
        Objects.equals(this.kind, templateResponse.kind) &&
        Objects.equals(this.archived, templateResponse.archived) &&
        Objects.equals(this.revision, templateResponse.revision) &&
        Objects.equals(this.currentVersion, templateResponse.currentVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(templateId, domain, kind, archived, revision, currentVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TemplateResponse {\n");
    sb.append("    templateId: ").append(toIndentedString(templateId)).append("\n");
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    archived: ").append(toIndentedString(archived)).append("\n");
    sb.append("    revision: ").append(toIndentedString(revision)).append("\n");
    sb.append("    currentVersion: ").append(toIndentedString(currentVersion)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}
