package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ArchiveTemplateRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ArchiveTemplateRequest {

  private UUID commandId;

  private Long expectedRevision;

  public ArchiveTemplateRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ArchiveTemplateRequest(UUID commandId, Long expectedRevision) {
    this.commandId = commandId;
    this.expectedRevision = expectedRevision;
  }

  public ArchiveTemplateRequest commandId(UUID commandId) {
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

  public ArchiveTemplateRequest expectedRevision(Long expectedRevision) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ArchiveTemplateRequest archiveTemplateRequest = (ArchiveTemplateRequest) o;
    return Objects.equals(this.commandId, archiveTemplateRequest.commandId) &&
        Objects.equals(this.expectedRevision, archiveTemplateRequest.expectedRevision);
  }

  @Override
  public int hashCode() {
    return Objects.hash(commandId, expectedRevision);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ArchiveTemplateRequest {\n");
    sb.append("    commandId: ").append(toIndentedString(commandId)).append("\n");
    sb.append("    expectedRevision: ").append(toIndentedString(expectedRevision)).append("\n");
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
