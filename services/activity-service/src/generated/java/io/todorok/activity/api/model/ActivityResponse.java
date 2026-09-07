package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.todorok.activity.api.model.ActivityStatus;
import io.todorok.activity.api.model.ActivityType;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ActivityResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ActivityResponse {

  private UUID activityId;

  private UUID commandId;

  private UUID taskId;

  private UUID userId;

  private ActivityType activityType;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime performedAt;

  private Map<String, Object> detail = new HashMap<>();

  private ActivityStatus status;

  private Long version;

  public ActivityResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ActivityResponse(UUID activityId, UUID commandId, UUID taskId, UUID userId, ActivityType activityType, OffsetDateTime performedAt, Map<String, Object> detail, ActivityStatus status, Long version) {
    this.activityId = activityId;
    this.commandId = commandId;
    this.taskId = taskId;
    this.userId = userId;
    this.activityType = activityType;
    this.performedAt = performedAt;
    this.detail = detail;
    this.status = status;
    this.version = version;
  }

  public ActivityResponse activityId(UUID activityId) {
    this.activityId = activityId;
    return this;
  }

  /**
   * Get activityId
   * @return activityId
   */
  @NotNull @Valid
  @JsonProperty("activityId")
  public UUID getActivityId() {
    return activityId;
  }

  @JsonProperty("activityId")
  public void setActivityId(UUID activityId) {
    this.activityId = activityId;
  }

  public ActivityResponse commandId(UUID commandId) {
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

  public ActivityResponse taskId(UUID taskId) {
    this.taskId = taskId;
    return this;
  }

  /**
   * Get taskId
   * @return taskId
   */
  @NotNull @Valid
  @JsonProperty("taskId")
  public UUID getTaskId() {
    return taskId;
  }

  @JsonProperty("taskId")
  public void setTaskId(UUID taskId) {
    this.taskId = taskId;
  }

  public ActivityResponse userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Get userId
   * @return userId
   */
  @NotNull @Valid
  @JsonProperty("userId")
  public UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public ActivityResponse activityType(ActivityType activityType) {
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

  public ActivityResponse performedAt(OffsetDateTime performedAt) {
    this.performedAt = performedAt;
    return this;
  }

  /**
   * Get performedAt
   * @return performedAt
   */
  @NotNull @Valid
  @JsonProperty("performedAt")
  public OffsetDateTime getPerformedAt() {
    return performedAt;
  }

  @JsonProperty("performedAt")
  public void setPerformedAt(OffsetDateTime performedAt) {
    this.performedAt = performedAt;
  }

  public ActivityResponse detail(Map<String, Object> detail) {
    this.detail = detail;
    return this;
  }

  public ActivityResponse putDetailItem(String key, Object detailItem) {
    if (this.detail == null) {
      this.detail = new HashMap<>();
    }
    this.detail.put(key, detailItem);
    return this;
  }

  /**
   * Get detail
   * @return detail
   */
  @NotNull
  @JsonProperty("detail")
  public Map<String, Object> getDetail() {
    return detail;
  }

  @JsonProperty("detail")
  public void setDetail(Map<String, Object> detail) {
    this.detail = detail;
  }

  public ActivityResponse status(ActivityStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid
  @JsonProperty("status")
  public ActivityStatus getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(ActivityStatus status) {
    this.status = status;
  }

  public ActivityResponse version(Long version) {
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
    ActivityResponse activityResponse = (ActivityResponse) o;
    return Objects.equals(this.activityId, activityResponse.activityId) &&
        Objects.equals(this.commandId, activityResponse.commandId) &&
        Objects.equals(this.taskId, activityResponse.taskId) &&
        Objects.equals(this.userId, activityResponse.userId) &&
        Objects.equals(this.activityType, activityResponse.activityType) &&
        Objects.equals(this.performedAt, activityResponse.performedAt) &&
        Objects.equals(this.detail, activityResponse.detail) &&
        Objects.equals(this.status, activityResponse.status) &&
        Objects.equals(this.version, activityResponse.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(activityId, commandId, taskId, userId, activityType, performedAt, detail, status, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ActivityResponse {\n");
    sb.append("    activityId: ").append(toIndentedString(activityId)).append("\n");
    sb.append("    commandId: ").append(toIndentedString(commandId)).append("\n");
    sb.append("    taskId: ").append(toIndentedString(taskId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    activityType: ").append(toIndentedString(activityType)).append("\n");
    sb.append("    performedAt: ").append(toIndentedString(performedAt)).append("\n");
    sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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
