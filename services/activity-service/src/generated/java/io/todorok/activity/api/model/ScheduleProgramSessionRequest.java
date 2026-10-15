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
 * ScheduleProgramSessionRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ScheduleProgramSessionRequest {

  private UUID commandId;

  private Integer week;

  private Integer session;

  private Long expectedRevision;

  public ScheduleProgramSessionRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ScheduleProgramSessionRequest(UUID commandId, Integer week, Integer session, Long expectedRevision) {
    this.commandId = commandId;
    this.week = week;
    this.session = session;
    this.expectedRevision = expectedRevision;
  }

  public ScheduleProgramSessionRequest commandId(UUID commandId) {
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

  public ScheduleProgramSessionRequest week(Integer week) {
    this.week = week;
    return this;
  }

  /**
   * Get week
   * minimum: 1
   * @return week
   */
  @NotNull @Min(value = 1)
  @JsonProperty("week")
  public Integer getWeek() {
    return week;
  }

  @JsonProperty("week")
  public void setWeek(Integer week) {
    this.week = week;
  }

  public ScheduleProgramSessionRequest session(Integer session) {
    this.session = session;
    return this;
  }

  /**
   * Get session
   * minimum: 1
   * maximum: 7
   * @return session
   */
  @NotNull @Min(value = 1) @Max(value = 7)
  @JsonProperty("session")
  public Integer getSession() {
    return session;
  }

  @JsonProperty("session")
  public void setSession(Integer session) {
    this.session = session;
  }

  public ScheduleProgramSessionRequest expectedRevision(Long expectedRevision) {
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
    ScheduleProgramSessionRequest scheduleProgramSessionRequest = (ScheduleProgramSessionRequest) o;
    return Objects.equals(this.commandId, scheduleProgramSessionRequest.commandId) &&
        Objects.equals(this.week, scheduleProgramSessionRequest.week) &&
        Objects.equals(this.session, scheduleProgramSessionRequest.session) &&
        Objects.equals(this.expectedRevision, scheduleProgramSessionRequest.expectedRevision);
  }

  @Override
  public int hashCode() {
    return Objects.hash(commandId, week, session, expectedRevision);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScheduleProgramSessionRequest {\n");
    sb.append("    commandId: ").append(toIndentedString(commandId)).append("\n");
    sb.append("    week: ").append(toIndentedString(week)).append("\n");
    sb.append("    session: ").append(toIndentedString(session)).append("\n");
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
