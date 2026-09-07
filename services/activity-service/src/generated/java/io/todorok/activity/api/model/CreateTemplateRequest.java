package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.todorok.activity.api.model.FieldDefinitionInput;
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
 * CreateTemplateRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class CreateTemplateRequest {

  private UUID commandId;

  private String name;

  private TemplateDomain domain;

  private TemplateKind kind;

  private List<@Valid FieldDefinitionInput> fields = new ArrayList<>();

  public CreateTemplateRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateTemplateRequest(UUID commandId, String name, TemplateDomain domain, TemplateKind kind, List<@Valid FieldDefinitionInput> fields) {
    this.commandId = commandId;
    this.name = name;
    this.domain = domain;
    this.kind = kind;
    this.fields = fields;
  }

  public CreateTemplateRequest commandId(UUID commandId) {
    this.commandId = commandId;
    return this;
  }

  /**
   * Get commandId
   * @return commandId
   */
  @NotNull @Valid
  @JsonProperty("commandId")
  public UUID getCommandId() {
    return commandId;
  }

  @JsonProperty("commandId")
  public void setCommandId(UUID commandId) {
    this.commandId = commandId;
  }

  public CreateTemplateRequest name(String name) {
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

  public CreateTemplateRequest domain(TemplateDomain domain) {
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

  public CreateTemplateRequest kind(TemplateKind kind) {
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

  public CreateTemplateRequest fields(List<@Valid FieldDefinitionInput> fields) {
    this.fields = fields;
    return this;
  }

  public CreateTemplateRequest addFieldsItem(FieldDefinitionInput fieldsItem) {
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
  public List<@Valid FieldDefinitionInput> getFields() {
    return fields;
  }

  @JsonProperty("fields")
  public void setFields(List<@Valid FieldDefinitionInput> fields) {
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
    CreateTemplateRequest createTemplateRequest = (CreateTemplateRequest) o;
    return Objects.equals(this.commandId, createTemplateRequest.commandId) &&
        Objects.equals(this.name, createTemplateRequest.name) &&
        Objects.equals(this.domain, createTemplateRequest.domain) &&
        Objects.equals(this.kind, createTemplateRequest.kind) &&
        Objects.equals(this.fields, createTemplateRequest.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(commandId, name, domain, kind, fields);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateTemplateRequest {\n");
    sb.append("    commandId: ").append(toIndentedString(commandId)).append("\n");
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
