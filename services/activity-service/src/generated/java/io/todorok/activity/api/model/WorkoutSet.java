package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.math.BigDecimal;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * WorkoutSet
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class WorkoutSet {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String exercise;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer reps;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable BigDecimal weightKg;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer durationSeconds;

  public WorkoutSet exercise(@Nullable String exercise) {
    this.exercise = exercise;
    return this;
  }

  /**
   * Get exercise
   * @return exercise
   */
  @Size(max = 120)
  @JsonProperty("exercise")
  public @Nullable String getExercise() {
    return exercise;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("exercise")
  public void setExercise(@Nullable String exercise) {
    this.exercise = exercise;
  }

  public WorkoutSet reps(@Nullable Integer reps) {
    this.reps = reps;
    return this;
  }

  /**
   * Get reps
   * minimum: 0
   * maximum: 100000
   * @return reps
   */
  @Min(value = 0) @Max(value = 100000)
  @JsonProperty("reps")
  public @Nullable Integer getReps() {
    return reps;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("reps")
  public void setReps(@Nullable Integer reps) {
    this.reps = reps;
  }

  public WorkoutSet weightKg(@Nullable BigDecimal weightKg) {
    this.weightKg = weightKg;
    return this;
  }

  /**
   * Get weightKg
   * minimum: 0
   * maximum: 10000
   * @return weightKg
   */
  @Valid @DecimalMin(value = "0") @DecimalMax(value = "10000")
  @JsonProperty("weightKg")
  public @Nullable BigDecimal getWeightKg() {
    return weightKg;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("weightKg")
  public void setWeightKg(@Nullable BigDecimal weightKg) {
    this.weightKg = weightKg;
  }

  public WorkoutSet durationSeconds(@Nullable Integer durationSeconds) {
    this.durationSeconds = durationSeconds;
    return this;
  }

  /**
   * Get durationSeconds
   * minimum: 0
   * maximum: 604800
   * @return durationSeconds
   */
  @Min(value = 0) @Max(value = 604800)
  @JsonProperty("durationSeconds")
  public @Nullable Integer getDurationSeconds() {
    return durationSeconds;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("durationSeconds")
  public void setDurationSeconds(@Nullable Integer durationSeconds) {
    this.durationSeconds = durationSeconds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkoutSet workoutSet = (WorkoutSet) o;
    return Objects.equals(this.exercise, workoutSet.exercise) &&
        Objects.equals(this.reps, workoutSet.reps) &&
        Objects.equals(this.weightKg, workoutSet.weightKg) &&
        Objects.equals(this.durationSeconds, workoutSet.durationSeconds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(exercise, reps, weightKg, durationSeconds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkoutSet {\n");
    sb.append("    exercise: ").append(toIndentedString(exercise)).append("\n");
    sb.append("    reps: ").append(toIndentedString(reps)).append("\n");
    sb.append("    weightKg: ").append(toIndentedString(weightKg)).append("\n");
    sb.append("    durationSeconds: ").append(toIndentedString(durationSeconds)).append("\n");
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
