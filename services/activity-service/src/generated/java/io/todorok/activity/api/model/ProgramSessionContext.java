package io.todorok.activity.api.model;

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
 * ProgramSessionContext
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ProgramSessionContext {

  private Integer week;

  private Integer session;

  private Integer sessionsPerWeek;

  private Boolean canRepeatWeek;

  public ProgramSessionContext() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProgramSessionContext(Integer week, Integer session, Integer sessionsPerWeek, Boolean canRepeatWeek) {
    this.week = week;
    this.session = session;
    this.sessionsPerWeek = sessionsPerWeek;
    this.canRepeatWeek = canRepeatWeek;
  }

  public ProgramSessionContext week(Integer week) {
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

  public ProgramSessionContext session(Integer session) {
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

  public ProgramSessionContext sessionsPerWeek(Integer sessionsPerWeek) {
    this.sessionsPerWeek = sessionsPerWeek;
    return this;
  }

  /**
   * Get sessionsPerWeek
   * minimum: 1
   * maximum: 7
   * @return sessionsPerWeek
   */
  @NotNull @Min(value = 1) @Max(value = 7)
  @JsonProperty("sessionsPerWeek")
  public Integer getSessionsPerWeek() {
    return sessionsPerWeek;
  }

  @JsonProperty("sessionsPerWeek")
  public void setSessionsPerWeek(Integer sessionsPerWeek) {
    this.sessionsPerWeek = sessionsPerWeek;
  }

  public ProgramSessionContext canRepeatWeek(Boolean canRepeatWeek) {
    this.canRepeatWeek = canRepeatWeek;
    return this;
  }

  /**
   * True only for the current active final session of its week.
   * @return canRepeatWeek
   */
  @NotNull
  @JsonProperty("canRepeatWeek")
  public Boolean getCanRepeatWeek() {
    return canRepeatWeek;
  }

  @JsonProperty("canRepeatWeek")
  public void setCanRepeatWeek(Boolean canRepeatWeek) {
    this.canRepeatWeek = canRepeatWeek;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProgramSessionContext programSessionContext = (ProgramSessionContext) o;
    return Objects.equals(this.week, programSessionContext.week) &&
        Objects.equals(this.session, programSessionContext.session) &&
        Objects.equals(this.sessionsPerWeek, programSessionContext.sessionsPerWeek) &&
        Objects.equals(this.canRepeatWeek, programSessionContext.canRepeatWeek);
  }

  @Override
  public int hashCode() {
    return Objects.hash(week, session, sessionsPerWeek, canRepeatWeek);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProgramSessionContext {\n");
    sb.append("    week: ").append(toIndentedString(week)).append("\n");
    sb.append("    session: ").append(toIndentedString(session)).append("\n");
    sb.append("    sessionsPerWeek: ").append(toIndentedString(sessionsPerWeek)).append("\n");
    sb.append("    canRepeatWeek: ").append(toIndentedString(canRepeatWeek)).append("\n");
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
