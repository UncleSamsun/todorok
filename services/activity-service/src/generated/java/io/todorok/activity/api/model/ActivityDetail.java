package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.activity.api.model.ClimbingDetail;
import io.todorok.activity.api.model.StudyDetail;
import io.todorok.activity.api.model.WorkoutDetail;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Optional typed detail. Only the member matching activityType is accepted; an empty object is valid.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ActivityDetail {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable WorkoutDetail workout;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StudyDetail study;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ClimbingDetail climbing;

  public ActivityDetail workout(@Nullable WorkoutDetail workout) {
    this.workout = workout;
    return this;
  }

  /**
   * Get workout
   * @return workout
   */
  @Valid
  @JsonProperty("workout")
  public @Nullable WorkoutDetail getWorkout() {
    return workout;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("workout")
  public void setWorkout(@Nullable WorkoutDetail workout) {
    this.workout = workout;
  }

  public ActivityDetail study(@Nullable StudyDetail study) {
    this.study = study;
    return this;
  }

  /**
   * Get study
   * @return study
   */
  @Valid
  @JsonProperty("study")
  public @Nullable StudyDetail getStudy() {
    return study;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("study")
  public void setStudy(@Nullable StudyDetail study) {
    this.study = study;
  }

  public ActivityDetail climbing(@Nullable ClimbingDetail climbing) {
    this.climbing = climbing;
    return this;
  }

  /**
   * Get climbing
   * @return climbing
   */
  @Valid
  @JsonProperty("climbing")
  public @Nullable ClimbingDetail getClimbing() {
    return climbing;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("climbing")
  public void setClimbing(@Nullable ClimbingDetail climbing) {
    this.climbing = climbing;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActivityDetail activityDetail = (ActivityDetail) o;
    return Objects.equals(this.workout, activityDetail.workout) &&
        Objects.equals(this.study, activityDetail.study) &&
        Objects.equals(this.climbing, activityDetail.climbing);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workout, study, climbing);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ActivityDetail {\n");
    sb.append("    workout: ").append(toIndentedString(workout)).append("\n");
    sb.append("    study: ").append(toIndentedString(study)).append("\n");
    sb.append("    climbing: ").append(toIndentedString(climbing)).append("\n");
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
