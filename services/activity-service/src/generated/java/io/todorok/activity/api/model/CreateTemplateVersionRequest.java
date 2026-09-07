package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.todorok.activity.api.model.FieldDefinitionInput;
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
 * CreateTemplateVersionRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class CreateTemplateVersionRequest {

  private UUID commandId;

  private Long expectedRevision;

  private String name;

  private List<@Valid FieldDefinitionInput> fields = new ArrayList<>();

  public CreateTemplateVersionRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateTemplateVersionRequest(UUID commandId, Long expectedRevision, String name, List<@Valid FieldDefinitionInput> fields) {
    this.commandId = commandId;
    this.expectedRevision = expectedRevision;
    this.name = name;
    this.fields = fields;
  }

  public CreateTemplateVersionRequest commandId(UUID commandId) {
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

  public CreateTemplateVersionRequest expectedRevision(Long expectedRevision) {
    this.expectedRevision = expectedRevision;
    return this;
  }

  /**
   * Get expectedRevision
   * minimum: 0
   * @return expectedRevision
   */
  @NotNull @Min(value = 0L)
  @JsonProperty("expectedRevision")
  public Long getExpectedRevision() {
    return expectedRevision;
  }

  @JsonProperty("expectedRevision")
  public void setExpectedRevision(Long expectedRevision) {
    this.expectedRevision = expectedRevision;
  }

  public CreateTemplateVersionRequest name(String name) {
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

  public CreateTemplateVersionRequest fields(List<@Valid FieldDefinitionInput> fields) {
    this.fields = fields;
    return this;
  }

  public CreateTemplateVersionRequest addFieldsItem(FieldDefinitionInput fieldsItem) {
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
    CreateTemplateVersionRequest createTemplateVersionRequest = (CreateTemplateVersionRequest) o;
    return Objects.equals(this.commandId, createTemplateVersionRequest.commandId) &&
        Objects.equals(this.expectedRevision, createTemplateVersionRequest.expectedRevision) &&
        Objects.equals(this.name, createTemplateVersionRequest.name) &&
        Objects.equals(this.fields, createTemplateVersionRequest.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(commandId, expectedRevision, name, fields);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateTemplateVersionRequest {\n");
    sb.append("    commandId: ").append(toIndentedString(commandId)).append("\n");
    sb.append("    expectedRevision: ").append(toIndentedString(expectedRevision)).append("\n");
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
