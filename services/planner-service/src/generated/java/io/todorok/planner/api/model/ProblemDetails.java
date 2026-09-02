package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.planner.api.model.ProblemDetailsFieldErrorsInner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ProblemDetails
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ProblemDetails {

  private String type;

  private String title;

  private Integer status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String detail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String instance;

  private String code;

  private String traceId;

  private Boolean retryable;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid ProblemDetailsFieldErrorsInner> fieldErrors = new ArrayList<>();

  public ProblemDetails() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProblemDetails(String type, String title, Integer status, String code, String traceId, Boolean retryable) {
    this.type = type;
    this.title = title;
    this.status = status;
    this.code = code;
    this.traceId = traceId;
    this.retryable = retryable;
  }

  public ProblemDetails type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull 
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  public ProblemDetails title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
   */
  @NotNull 
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public ProblemDetails status(Integer status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * minimum: 400
   * maximum: 599
   * @return status
   */
  @NotNull @Min(value = 400) @Max(value = 599) 
  @JsonProperty("status")
  public Integer getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(Integer status) {
    this.status = status;
  }

  public ProblemDetails detail(@Nullable String detail) {
    this.detail = detail;
    return this;
  }

  /**
   * Get detail
   * @return detail
   */
  
  @JsonProperty("detail")
  public @Nullable String getDetail() {
    return detail;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("detail")
  public void setDetail(@Nullable String detail) {
    this.detail = detail;
  }

  public ProblemDetails instance(@Nullable String instance) {
    this.instance = instance;
    return this;
  }

  /**
   * Get instance
   * @return instance
   */
  
  @JsonProperty("instance")
  public @Nullable String getInstance() {
    return instance;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("instance")
  public void setInstance(@Nullable String instance) {
    this.instance = instance;
  }

  public ProblemDetails code(String code) {
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

  public ProblemDetails traceId(String traceId) {
    this.traceId = traceId;
    return this;
  }

  /**
   * Get traceId
   * @return traceId
   */
  @NotNull @Size(min = 1) 
  @JsonProperty("traceId")
  public String getTraceId() {
    return traceId;
  }

  @JsonProperty("traceId")
  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public ProblemDetails retryable(Boolean retryable) {
    this.retryable = retryable;
    return this;
  }

  /**
   * Get retryable
   * @return retryable
   */
  @NotNull 
  @JsonProperty("retryable")
  public Boolean getRetryable() {
    return retryable;
  }

  @JsonProperty("retryable")
  public void setRetryable(Boolean retryable) {
    this.retryable = retryable;
  }

  public ProblemDetails fieldErrors(List<@Valid ProblemDetailsFieldErrorsInner> fieldErrors) {
    this.fieldErrors = fieldErrors;
    return this;
  }

  public ProblemDetails addFieldErrorsItem(ProblemDetailsFieldErrorsInner fieldErrorsItem) {
    if (this.fieldErrors == null) {
      this.fieldErrors = new ArrayList<>();
    }
    this.fieldErrors.add(fieldErrorsItem);
    return this;
  }

  /**
   * Get fieldErrors
   * @return fieldErrors
   */
  @Valid 
  @JsonProperty("fieldErrors")
  public List<@Valid ProblemDetailsFieldErrorsInner> getFieldErrors() {
    return fieldErrors;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("fieldErrors")
  public void setFieldErrors(List<@Valid ProblemDetailsFieldErrorsInner> fieldErrors) {
    this.fieldErrors = fieldErrors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProblemDetails problemDetails = (ProblemDetails) o;
    return Objects.equals(this.type, problemDetails.type) &&
        Objects.equals(this.title, problemDetails.title) &&
        Objects.equals(this.status, problemDetails.status) &&
        Objects.equals(this.detail, problemDetails.detail) &&
        Objects.equals(this.instance, problemDetails.instance) &&
        Objects.equals(this.code, problemDetails.code) &&
        Objects.equals(this.traceId, problemDetails.traceId) &&
        Objects.equals(this.retryable, problemDetails.retryable) &&
        Objects.equals(this.fieldErrors, problemDetails.fieldErrors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, title, status, detail, instance, code, traceId, retryable, fieldErrors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProblemDetails {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
    sb.append("    instance: ").append(toIndentedString(instance)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    traceId: ").append(toIndentedString(traceId)).append("\n");
    sb.append("    retryable: ").append(toIndentedString(retryable)).append("\n");
    sb.append("    fieldErrors: ").append(toIndentedString(fieldErrors)).append("\n");
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

