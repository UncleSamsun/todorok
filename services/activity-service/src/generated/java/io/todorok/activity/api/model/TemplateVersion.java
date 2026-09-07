package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.todorok.activity.api.model.FieldDefinition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * TemplateVersion
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class TemplateVersion {

  private UUID templateId;

  private Long templateVersion;

  private String name;

  private List<@Valid FieldDefinition> fields = new ArrayList<>();

  public TemplateVersion() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TemplateVersion(UUID templateId, Long templateVersion, String name, List<@Valid FieldDefinition> fields) {
    this.templateId = templateId;
    this.templateVersion = templateVersion;
    this.name = name;
    this.fields = fields;
  }

  public TemplateVersion templateId(UUID templateId) {
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

  public TemplateVersion templateVersion(Long templateVersion) {
    this.templateVersion = templateVersion;
    return this;
  }

  /**
   * Get templateVersion
   * minimum: 1
   * @return templateVersion
   */
  @NotNull @Min(value = 1L)
  @JsonProperty("templateVersion")
  public Long getTemplateVersion() {
    return templateVersion;
  }

  @JsonProperty("templateVersion")
  public void setTemplateVersion(Long templateVersion) {
    this.templateVersion = templateVersion;
  }

  public TemplateVersion name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(min = 1, max = 120)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public TemplateVersion fields(List<@Valid FieldDefinition> fields) {
    this.fields = fields;
    return this;
  }

  public TemplateVersion addFieldsItem(FieldDefinition fieldsItem) {
    if (this.fields == null) {
      this.fields = new ArrayList<>();
    }
    this.fields.add(fieldsItem);
    return this;
  }

  /**
   * Get fields
   * @return fields
   */
  @NotNull @Valid
  @JsonProperty("fields")
  public List<@Valid FieldDefinition> getFields() {
    return fields;
  }

  @JsonProperty("fields")
  public void setFields(List<@Valid FieldDefinition> fields) {
    this.fields = fields;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TemplateVersion templateVersion = (TemplateVersion) o;
    return Objects.equals(this.templateId, templateVersion.templateId) &&
        Objects.equals(this.templateVersion, templateVersion.templateVersion) &&
        Objects.equals(this.name, templateVersion.name) &&
        Objects.equals(this.fields, templateVersion.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(templateId, templateVersion, name, fields);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TemplateVersion {\n");
    sb.append("    templateId: ").append(toIndentedString(templateId)).append("\n");
    sb.append("    templateVersion: ").append(toIndentedString(templateVersion)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    fields: ").append(toIndentedString(fields)).append("\n");
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
