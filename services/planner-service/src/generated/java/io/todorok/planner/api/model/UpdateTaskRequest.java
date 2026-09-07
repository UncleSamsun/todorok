package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UpdateTaskRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class UpdateTaskRequest {

  private String title;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate scheduledDate;

  private Long version;

  public UpdateTaskRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateTaskRequest(String title, LocalDate scheduledDate, Long version) {
    this.title = title;
    this.scheduledDate = scheduledDate;
    this.version = version;
  }

  public UpdateTaskRequest title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
   */
  @NotNull @Size(min = 1, max = 120)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public UpdateTaskRequest scheduledDate(LocalDate scheduledDate) {
    this.scheduledDate = scheduledDate;
    return this;
  }

  /**
   * Get scheduledDate
   * @return scheduledDate
   */
  @NotNull @Valid
  @JsonProperty("scheduledDate")
  public LocalDate getScheduledDate() {
    return scheduledDate;
  }

  @JsonProperty("scheduledDate")
  public void setScheduledDate(LocalDate scheduledDate) {
    this.scheduledDate = scheduledDate;
  }

  public UpdateTaskRequest version(Long version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * minimum: 0
   * @return version
   */
  @NotNull @Min(value = 0L)
  @JsonProperty("version")
  public Long getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(Long version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateTaskRequest updateTaskRequest = (UpdateTaskRequest) o;
    return Objects.equals(this.title, updateTaskRequest.title) &&
        Objects.equals(this.scheduledDate, updateTaskRequest.scheduledDate) &&
        Objects.equals(this.version, updateTaskRequest.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, scheduledDate, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateTaskRequest {\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    scheduledDate: ").append(toIndentedString(scheduledDate)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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
