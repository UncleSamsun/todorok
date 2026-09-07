package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.activity.api.model.TemplateFieldType;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * FieldDefinition
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class FieldDefinition {

  private UUID fieldId;

  private String name;

  private TemplateFieldType type;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String unit;

  private Integer position;

  public FieldDefinition() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FieldDefinition(UUID fieldId, String name, TemplateFieldType type, Integer position) {
    this.fieldId = fieldId;
    this.name = name;
    this.type = type;
    this.position = position;
  }

  public FieldDefinition fieldId(UUID fieldId) {
    this.fieldId = fieldId;
    return this;
  }

  /**
   * Get fieldId
   * @return fieldId
   */
  @NotNull @Valid
  @JsonProperty("fieldId")
  public UUID getFieldId() {
    return fieldId;
  }

  @JsonProperty("fieldId")
  public void setFieldId(UUID fieldId) {
    this.fieldId = fieldId;
  }

  public FieldDefinition name(String name) {
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

  public FieldDefinition type(TemplateFieldType type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid
  @JsonProperty("type")
  public TemplateFieldType getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(TemplateFieldType type) {
    this.type = type;
  }

  public FieldDefinition unit(@Nullable String unit) {
    this.unit = unit;
    return this;
  }

  /**
   * Get unit
   * @return unit
   */
  @Size(max = 40)
  @JsonProperty("unit")
  public @Nullable String getUnit() {
    return unit;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("unit")
  public void setUnit(@Nullable String unit) {
    this.unit = unit;
  }

  public FieldDefinition position(Integer position) {
    this.position = position;
    return this;
  }

  /**
   * Get position
   * minimum: 0
   * @return position
   */
  @NotNull @Min(value = 0)
  @JsonProperty("position")
  public Integer getPosition() {
    return position;
  }

  @JsonProperty("position")
  public void setPosition(Integer position) {
    this.position = position;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FieldDefinition fieldDefinition = (FieldDefinition) o;
    return Objects.equals(this.fieldId, fieldDefinition.fieldId) &&
        Objects.equals(this.name, fieldDefinition.name) &&
        Objects.equals(this.type, fieldDefinition.type) &&
        Objects.equals(this.unit, fieldDefinition.unit) &&
        Objects.equals(this.position, fieldDefinition.position);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fieldId, name, type, unit, position);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FieldDefinition {\n");
    sb.append("    fieldId: ").append(toIndentedString(fieldId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    unit: ").append(toIndentedString(unit)).append("\n");
    sb.append("    position: ").append(toIndentedString(position)).append("\n");
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
