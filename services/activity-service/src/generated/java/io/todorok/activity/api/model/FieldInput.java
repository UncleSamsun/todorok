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
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Exactly one non-null value member matching type is required. Blank text or memo is normalized to omitted.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class FieldInput {

  private UUID fieldId;

  private TemplateFieldType type;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable BigDecimal numberValue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Long timeSeconds;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String textValue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean checked;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String memoValue;

  public FieldInput() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public FieldInput(UUID fieldId, TemplateFieldType type) {
    this.fieldId = fieldId;
    this.type = type;
  }

  public FieldInput fieldId(UUID fieldId) {
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

  public FieldInput type(TemplateFieldType type) {
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

  public FieldInput numberValue(@Nullable BigDecimal numberValue) {
    this.numberValue = numberValue;
    return this;
  }

  /**
   * Get numberValue
   * @return numberValue
   */
  @Valid
  @JsonProperty("numberValue")
  public @Nullable BigDecimal getNumberValue() {
    return numberValue;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("numberValue")
  public void setNumberValue(@Nullable BigDecimal numberValue) {
    this.numberValue = numberValue;
  }

  public FieldInput timeSeconds(@Nullable Long timeSeconds) {
    this.timeSeconds = timeSeconds;
    return this;
  }

  /**
   * Get timeSeconds
   * minimum: 0
   * maximum: 9007199254740991
   * @return timeSeconds
   */
  @Min(value = 0L) @Max(value = 9007199254740991L)
  @JsonProperty("timeSeconds")
  public @Nullable Long getTimeSeconds() {
    return timeSeconds;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("timeSeconds")
  public void setTimeSeconds(@Nullable Long timeSeconds) {
    this.timeSeconds = timeSeconds;
  }

  public FieldInput textValue(@Nullable String textValue) {
    this.textValue = textValue;
    return this;
  }

  /**
   * Get textValue
   * @return textValue
   */
  @Size(max = 120)
  @JsonProperty("textValue")
  public @Nullable String getTextValue() {
    return textValue;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("textValue")
  public void setTextValue(@Nullable String textValue) {
    this.textValue = textValue;
  }

  public FieldInput checked(@Nullable Boolean checked) {
    this.checked = checked;
    return this;
  }

  /**
   * Get checked
   * @return checked
   */

  @JsonProperty("checked")
  public @Nullable Boolean getChecked() {
    return checked;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("checked")
  public void setChecked(@Nullable Boolean checked) {
    this.checked = checked;
  }

  public FieldInput memoValue(@Nullable String memoValue) {
    this.memoValue = memoValue;
    return this;
  }

  /**
   * Get memoValue
   * @return memoValue
   */
  @Size(max = 20000)
  @JsonProperty("memoValue")
  public @Nullable String getMemoValue() {
    return memoValue;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("memoValue")
  public void setMemoValue(@Nullable String memoValue) {
    this.memoValue = memoValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FieldInput fieldInput = (FieldInput) o;
    return Objects.equals(this.fieldId, fieldInput.fieldId) &&
        Objects.equals(this.type, fieldInput.type) &&
        Objects.equals(this.numberValue, fieldInput.numberValue) &&
        Objects.equals(this.timeSeconds, fieldInput.timeSeconds) &&
        Objects.equals(this.textValue, fieldInput.textValue) &&
        Objects.equals(this.checked, fieldInput.checked) &&
        Objects.equals(this.memoValue, fieldInput.memoValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fieldId, type, numberValue, timeSeconds, textValue, checked, memoValue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FieldInput {\n");
    sb.append("    fieldId: ").append(toIndentedString(fieldId)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    numberValue: ").append(toIndentedString(numberValue)).append("\n");
    sb.append("    timeSeconds: ").append(toIndentedString(timeSeconds)).append("\n");
    sb.append("    textValue: ").append(toIndentedString(textValue)).append("\n");
    sb.append("    checked: ").append(toIndentedString(checked)).append("\n");
    sb.append("    memoValue: ").append(toIndentedString(memoValue)).append("\n");
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
