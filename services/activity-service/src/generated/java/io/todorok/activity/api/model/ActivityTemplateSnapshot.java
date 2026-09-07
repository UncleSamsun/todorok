package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.todorok.activity.api.model.FieldDefinition;
import io.todorok.activity.api.model.TemplateDomain;
import io.todorok.activity.api.model.TemplateKind;
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
 * ActivityTemplateSnapshot
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ActivityTemplateSnapshot {

  /**
   * Gets or Sets schemaVersion
   */
  public enum SchemaVersionEnum {
    NUMBER_1(1);

    private final Integer value;

    SchemaVersionEnum(Integer value) {
      this.value = value;
    }

    @JsonValue
    public Integer getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static SchemaVersionEnum fromValue(Integer value) {
      for (SchemaVersionEnum b : SchemaVersionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private SchemaVersionEnum schemaVersion;

  private UUID templateId;

  private Long templateVersion;

  private String name;

  private TemplateDomain domain;

  private TemplateKind kind;

  private List<@Valid FieldDefinition> fields = new ArrayList<>();

  public ActivityTemplateSnapshot() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ActivityTemplateSnapshot(SchemaVersionEnum schemaVersion, UUID templateId, Long templateVersion, String name, TemplateDomain domain, TemplateKind kind, List<@Valid FieldDefinition> fields) {
    this.schemaVersion = schemaVersion;
    this.templateId = templateId;
    this.templateVersion = templateVersion;
    this.name = name;
    this.domain = domain;
    this.kind = kind;
    this.fields = fields;
  }

  public ActivityTemplateSnapshot schemaVersion(SchemaVersionEnum schemaVersion) {
    this.schemaVersion = schemaVersion;
    return this;
  }

  /**
   * Get schemaVersion
   * @return schemaVersion
   */
  @NotNull
  @JsonProperty("schemaVersion")
  public SchemaVersionEnum getSchemaVersion() {
    return schemaVersion;
  }

  @JsonProperty("schemaVersion")
  public void setSchemaVersion(SchemaVersionEnum schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  public ActivityTemplateSnapshot templateId(UUID templateId) {
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

  public ActivityTemplateSnapshot templateVersion(Long templateVersion) {
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

  public ActivityTemplateSnapshot name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public ActivityTemplateSnapshot domain(TemplateDomain domain) {
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

  public ActivityTemplateSnapshot kind(TemplateKind kind) {
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

  public ActivityTemplateSnapshot fields(List<@Valid FieldDefinition> fields) {
    this.fields = fields;
    return this;
  }

  public ActivityTemplateSnapshot addFieldsItem(FieldDefinition fieldsItem) {
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
    ActivityTemplateSnapshot activityTemplateSnapshot = (ActivityTemplateSnapshot) o;
    return Objects.equals(this.schemaVersion, activityTemplateSnapshot.schemaVersion) &&
        Objects.equals(this.templateId, activityTemplateSnapshot.templateId) &&
        Objects.equals(this.templateVersion, activityTemplateSnapshot.templateVersion) &&
        Objects.equals(this.name, activityTemplateSnapshot.name) &&
        Objects.equals(this.domain, activityTemplateSnapshot.domain) &&
        Objects.equals(this.kind, activityTemplateSnapshot.kind) &&
        Objects.equals(this.fields, activityTemplateSnapshot.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(schemaVersion, templateId, templateVersion, name, domain, kind, fields);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ActivityTemplateSnapshot {\n");
    sb.append("    schemaVersion: ").append(toIndentedString(schemaVersion)).append("\n");
    sb.append("    templateId: ").append(toIndentedString(templateId)).append("\n");
    sb.append("    templateVersion: ").append(toIndentedString(templateVersion)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
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
