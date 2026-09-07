package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ClimbingRound
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ClimbingRound {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String grade;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer attempts;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean completed;

  public ClimbingRound grade(@Nullable String grade) {
    this.grade = grade;
    return this;
  }

  /**
   * Get grade
   * @return grade
   */
  @Size(max = 40)
  @JsonProperty("grade")
  public @Nullable String getGrade() {
    return grade;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("grade")
  public void setGrade(@Nullable String grade) {
    this.grade = grade;
  }

  public ClimbingRound attempts(@Nullable Integer attempts) {
    this.attempts = attempts;
    return this;
  }

  /**
   * Get attempts
   * minimum: 0
   * maximum: 100000
   * @return attempts
   */
  @Min(value = 0) @Max(value = 100000)
  @JsonProperty("attempts")
  public @Nullable Integer getAttempts() {
    return attempts;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("attempts")
  public void setAttempts(@Nullable Integer attempts) {
    this.attempts = attempts;
  }

  public ClimbingRound completed(@Nullable Boolean completed) {
    this.completed = completed;
    return this;
  }

  /**
   * Get completed
   * @return completed
   */

  @JsonProperty("completed")
  public @Nullable Boolean getCompleted() {
    return completed;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("completed")
  public void setCompleted(@Nullable Boolean completed) {
    this.completed = completed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClimbingRound climbingRound = (ClimbingRound) o;
    return Objects.equals(this.grade, climbingRound.grade) &&
        Objects.equals(this.attempts, climbingRound.attempts) &&
        Objects.equals(this.completed, climbingRound.completed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(grade, attempts, completed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClimbingRound {\n");
    sb.append("    grade: ").append(toIndentedString(grade)).append("\n");
    sb.append("    attempts: ").append(toIndentedString(attempts)).append("\n");
    sb.append("    completed: ").append(toIndentedString(completed)).append("\n");
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
