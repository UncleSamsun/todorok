package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.activity.api.model.ActivityDetail;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CorrectActivityRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class CorrectActivityRequest {

  private Long expectedVersion;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime performedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String note;

  private ActivityDetail detail;

  public CorrectActivityRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CorrectActivityRequest(Long expectedVersion, OffsetDateTime performedAt, ActivityDetail detail) {
    this.expectedVersion = expectedVersion;
    this.performedAt = performedAt;
    this.detail = detail;
  }

  public CorrectActivityRequest expectedVersion(Long expectedVersion) {
    this.expectedVersion = expectedVersion;
    return this;
  }

  /**
   * Get expectedVersion
   * minimum: 0
   * @return expectedVersion
   */
  @NotNull @Min(value = 0L)
  @JsonProperty("expectedVersion")
  public Long getExpectedVersion() {
    return expectedVersion;
  }

  @JsonProperty("expectedVersion")
  public void setExpectedVersion(Long expectedVersion) {
    this.expectedVersion = expectedVersion;
  }

  public CorrectActivityRequest performedAt(OffsetDateTime performedAt) {
    this.performedAt = performedAt;
    return this;
  }

  /**
   * Get performedAt
   * @return performedAt
   */
  @NotNull @Valid
  @JsonProperty("performedAt")
  public OffsetDateTime getPerformedAt() {
    return performedAt;
  }

  @JsonProperty("performedAt")
  public void setPerformedAt(OffsetDateTime performedAt) {
    this.performedAt = performedAt;
  }

  public CorrectActivityRequest startedAt(@Nullable OffsetDateTime startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  /**
   * Get startedAt
   * @return startedAt
   */
  @Valid
  @JsonProperty("startedAt")
  public @Nullable OffsetDateTime getStartedAt() {
    return startedAt;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("startedAt")
  public void setStartedAt(@Nullable OffsetDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public CorrectActivityRequest endedAt(@Nullable OffsetDateTime endedAt) {
    this.endedAt = endedAt;
    return this;
  }

  /**
   * Get endedAt
   * @return endedAt
   */
  @Valid
  @JsonProperty("endedAt")
  public @Nullable OffsetDateTime getEndedAt() {
    return endedAt;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("endedAt")
  public void setEndedAt(@Nullable OffsetDateTime endedAt) {
    this.endedAt = endedAt;
  }

  public CorrectActivityRequest note(@Nullable String note) {
    this.note = note;
    return this;
  }

  /**
   * Get note
   * @return note
   */
  @Size(max = 20000)
  @JsonProperty("note")
  public @Nullable String getNote() {
    return note;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("note")
  public void setNote(@Nullable String note) {
    this.note = note;
  }

  public CorrectActivityRequest detail(ActivityDetail detail) {
    this.detail = detail;
    return this;
  }

  /**
   * Get detail
   * @return detail
   */
  @NotNull @Valid
  @JsonProperty("detail")
  public ActivityDetail getDetail() {
    return detail;
  }

  @JsonProperty("detail")
  public void setDetail(ActivityDetail detail) {
    this.detail = detail;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CorrectActivityRequest correctActivityRequest = (CorrectActivityRequest) o;
    return Objects.equals(this.expectedVersion, correctActivityRequest.expectedVersion) &&
        Objects.equals(this.performedAt, correctActivityRequest.performedAt) &&
        Objects.equals(this.startedAt, correctActivityRequest.startedAt) &&
        Objects.equals(this.endedAt, correctActivityRequest.endedAt) &&
        Objects.equals(this.note, correctActivityRequest.note) &&
        Objects.equals(this.detail, correctActivityRequest.detail);
  }

  @Override
  public int hashCode() {
    return Objects.hash(expectedVersion, performedAt, startedAt, endedAt, note, detail);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CorrectActivityRequest {\n");
    sb.append("    expectedVersion: ").append(toIndentedString(expectedVersion)).append("\n");
    sb.append("    performedAt: ").append(toIndentedString(performedAt)).append("\n");
    sb.append("    startedAt: ").append(toIndentedString(startedAt)).append("\n");
    sb.append("    endedAt: ").append(toIndentedString(endedAt)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
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
