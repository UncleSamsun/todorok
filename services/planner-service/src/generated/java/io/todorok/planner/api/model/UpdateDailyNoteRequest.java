package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UpdateDailyNoteRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class UpdateDailyNoteRequest {

  private String content;

  private Long expectedVersion = null;

  public UpdateDailyNoteRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateDailyNoteRequest(String content, Long expectedVersion) {
    this.content = content;
    this.expectedVersion = expectedVersion;
  }

  public UpdateDailyNoteRequest content(String content) {
    this.content = content;
    return this;
  }

  /**
   * Get content
   * @return content
   */
  @NotNull @Size(max = 20000)

  @JsonProperty("content")
  public String getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(String content) {
    this.content = content;
  }

  public UpdateDailyNoteRequest expectedVersion(Long expectedVersion) {
    this.expectedVersion = expectedVersion;
    return this;
  }

  /**
   * Get expectedVersion
   * minimum: 0
   * @return expectedVersion
   */
  @Min(value = 0L)

  @JsonProperty("expectedVersion")
  public Long getExpectedVersion() {
    return expectedVersion;
  }

  @JsonProperty("expectedVersion")
  public void setExpectedVersion(Long expectedVersion) {
    this.expectedVersion = expectedVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateDailyNoteRequest updateDailyNoteRequest = (UpdateDailyNoteRequest) o;
    return Objects.equals(this.content, updateDailyNoteRequest.content) &&
        Objects.equals(this.expectedVersion, updateDailyNoteRequest.expectedVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, expectedVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateDailyNoteRequest {\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    expectedVersion: ").append(toIndentedString(expectedVersion)).append("\n");
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
