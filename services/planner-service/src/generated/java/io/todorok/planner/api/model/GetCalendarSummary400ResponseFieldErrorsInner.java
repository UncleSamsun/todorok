package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * GetCalendarSummary400ResponseFieldErrorsInner
 */

@JsonTypeName("getCalendarSummary_400_response_fieldErrors_inner")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class GetCalendarSummary400ResponseFieldErrorsInner {

  private String field;

  private String code;

  private String message;

  public GetCalendarSummary400ResponseFieldErrorsInner() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GetCalendarSummary400ResponseFieldErrorsInner(String field, String code, String message) {
    this.field = field;
    this.code = code;
    this.message = message;
  }

  public GetCalendarSummary400ResponseFieldErrorsInner field(String field) {
    this.field = field;
    return this;
  }

  /**
   * Get field
   * @return field
   */
  @NotNull @Size(min = 1) 
  @JsonProperty("field")
  public String getField() {
    return field;
  }

  @JsonProperty("field")
  public void setField(String field) {
    this.field = field;
  }

  public GetCalendarSummary400ResponseFieldErrorsInner code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Get code
   * @return code
   */
  @NotNull @Pattern(regexp = "^[A-Z][A-Z0-9_]+$") 
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(String code) {
    this.code = code;
  }

  public GetCalendarSummary400ResponseFieldErrorsInner message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
   */
  @NotNull @Size(min = 1) 
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  @JsonProperty("message")
  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetCalendarSummary400ResponseFieldErrorsInner getCalendarSummary400ResponseFieldErrorsInner = (GetCalendarSummary400ResponseFieldErrorsInner) o;
    return Objects.equals(this.field, getCalendarSummary400ResponseFieldErrorsInner.field) &&
        Objects.equals(this.code, getCalendarSummary400ResponseFieldErrorsInner.code) &&
        Objects.equals(this.message, getCalendarSummary400ResponseFieldErrorsInner.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field, code, message);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetCalendarSummary400ResponseFieldErrorsInner {\n");
    sb.append("    field: ").append(toIndentedString(field)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

