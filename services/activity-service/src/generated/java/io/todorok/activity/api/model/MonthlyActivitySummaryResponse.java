package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.todorok.activity.api.model.ActivityType;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * MonthlyActivitySummaryResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class MonthlyActivitySummaryResponse {

  private String month;

  private ActivityType activityType;

  private Integer completedCount;

  private Long durationSeconds;

  public MonthlyActivitySummaryResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public MonthlyActivitySummaryResponse(String month, ActivityType activityType, Integer completedCount, Long durationSeconds) {
    this.month = month;
    this.activityType = activityType;
    this.completedCount = completedCount;
    this.durationSeconds = durationSeconds;
  }

  public MonthlyActivitySummaryResponse month(String month) {
    this.month = month;
    return this;
  }

  /**
   * Calendar month in YYYY-MM form.
   * @return month
   */
  @NotNull
  @JsonProperty("month")
  public String getMonth() {
    return month;
  }

  @JsonProperty("month")
  public void setMonth(String month) {
    this.month = month;
  }

  public MonthlyActivitySummaryResponse activityType(ActivityType activityType) {
    this.activityType = activityType;
    return this;
  }

  /**
   * Get activityType
   * @return activityType
   */
  @NotNull @Valid
  @JsonProperty("activityType")
  public ActivityType getActivityType() {
    return activityType;
  }

  @JsonProperty("activityType")
  public void setActivityType(ActivityType activityType) {
    this.activityType = activityType;
  }

  public MonthlyActivitySummaryResponse completedCount(Integer completedCount) {
    this.completedCount = completedCount;
    return this;
  }

  /**
   * Get completedCount
   * minimum: 0
   * @return completedCount
   */
  @NotNull @Min(value = 0)
  @JsonProperty("completedCount")
  public Integer getCompletedCount() {
    return completedCount;
  }

  @JsonProperty("completedCount")
  public void setCompletedCount(Integer completedCount) {
    this.completedCount = completedCount;
  }

  public MonthlyActivitySummaryResponse durationSeconds(Long durationSeconds) {
    this.durationSeconds = durationSeconds;
    return this;
  }

  /**
   * Get durationSeconds
   * minimum: 0
   * @return durationSeconds
   */
  @NotNull @Min(value = 0L)
  @JsonProperty("durationSeconds")
  public Long getDurationSeconds() {
    return durationSeconds;
  }

  @JsonProperty("durationSeconds")
  public void setDurationSeconds(Long durationSeconds) {
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
    MonthlyActivitySummaryResponse monthlyActivitySummaryResponse = (MonthlyActivitySummaryResponse) o;
    return Objects.equals(this.month, monthlyActivitySummaryResponse.month) &&
        Objects.equals(this.activityType, monthlyActivitySummaryResponse.activityType) &&
        Objects.equals(this.completedCount, monthlyActivitySummaryResponse.completedCount) &&
        Objects.equals(this.durationSeconds, monthlyActivitySummaryResponse.durationSeconds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(month, activityType, completedCount, durationSeconds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MonthlyActivitySummaryResponse {\n");
    sb.append("    month: ").append(toIndentedString(month)).append("\n");
    sb.append("    activityType: ").append(toIndentedString(activityType)).append("\n");
    sb.append("    completedCount: ").append(toIndentedString(completedCount)).append("\n");
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
