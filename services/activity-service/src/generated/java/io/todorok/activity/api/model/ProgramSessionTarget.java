package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * ProgramSessionTarget
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ProgramSessionTarget {

  private UUID sessionId;

  private Integer week;

  private Integer session;

  private List<@Min(1)Integer> targetSets = new ArrayList<>();

  public ProgramSessionTarget() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProgramSessionTarget(UUID sessionId, Integer week, Integer session, List<@Min(1)Integer> targetSets) {
    this.sessionId = sessionId;
    this.week = week;
    this.session = session;
    this.targetSets = targetSets;
  }

  public ProgramSessionTarget sessionId(UUID sessionId) {
    this.sessionId = sessionId;
    return this;
  }

  /**
   * Get sessionId
   * @return sessionId
   */
  @NotNull @Valid
  @JsonProperty("sessionId")
  public UUID getSessionId() {
    return sessionId;
  }

  @JsonProperty("sessionId")
  public void setSessionId(UUID sessionId) {
    this.sessionId = sessionId;
  }

  public ProgramSessionTarget week(Integer week) {
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

  public ProgramSessionTarget session(Integer session) {
    this.session = session;
    return this;
  }

  /**
   * Get session
   * minimum: 1
   * @return session
   */
  @NotNull @Min(value = 1)
  @JsonProperty("session")
  public Integer getSession() {
    return session;
  }

  @JsonProperty("session")
  public void setSession(Integer session) {
    this.session = session;
  }

  public ProgramSessionTarget targetSets(List<@Min(1)Integer> targetSets) {
    this.targetSets = targetSets;
    return this;
  }

  public ProgramSessionTarget addTargetSetsItem(Integer targetSetsItem) {
    if (this.targetSets == null) {
      this.targetSets = new ArrayList<>();
    }
    this.targetSets.add(targetSetsItem);
    return this;
  }

  /**
   * Get targetSets
   * @return targetSets
   */
  @NotNull @Size(min = 1)
  @JsonProperty("targetSets")
  public List<@Min(1)Integer> getTargetSets() {
    return targetSets;
  }

  @JsonProperty("targetSets")
  public void setTargetSets(List<@Min(1)Integer> targetSets) {
    this.targetSets = targetSets;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProgramSessionTarget programSessionTarget = (ProgramSessionTarget) o;
    return Objects.equals(this.sessionId, programSessionTarget.sessionId) &&
        Objects.equals(this.week, programSessionTarget.week) &&
        Objects.equals(this.session, programSessionTarget.session) &&
        Objects.equals(this.targetSets, programSessionTarget.targetSets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sessionId, week, session, targetSets);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProgramSessionTarget {\n");
    sb.append("    sessionId: ").append(toIndentedString(sessionId)).append("\n");
    sb.append("    week: ").append(toIndentedString(week)).append("\n");
    sb.append("    session: ").append(toIndentedString(session)).append("\n");
    sb.append("    targetSets: ").append(toIndentedString(targetSets)).append("\n");
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
