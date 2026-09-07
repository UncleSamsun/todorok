package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.activity.api.model.ActivityDetail;
import io.todorok.activity.api.model.ActivityStatus;
import io.todorok.activity.api.model.ActivitySyncState;
import io.todorok.activity.api.model.ActivityType;
import java.time.OffsetDateTime;
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

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endedAt;

  private ActivityDetail detail;

  private ActivityStatus status;

  private Long version;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String note;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ActivitySyncState syncState;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String syncReason;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime previousPerformedAt;

  public ActivityResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ActivityResponse(UUID activityId, UUID commandId, UUID taskId, UUID userId, ActivityType activityType, OffsetDateTime performedAt, ActivityDetail detail, ActivityStatus status, Long version) {
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

  public ActivityResponse startedAt(@Nullable OffsetDateTime startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  /**
   * Get startedAt
   * @return startedAt
   */
  @Valid
  @JsonProperty("startedAt")
  public @Nullable OffsetDateTime getStartedAt() {
    return startedAt;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("startedAt")
  public void setStartedAt(@Nullable OffsetDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public ActivityResponse endedAt(@Nullable OffsetDateTime endedAt) {
    this.endedAt = endedAt;
    return this;
  }

  /**
   * Get endedAt
   * @return endedAt
   */
  @Valid
  @JsonProperty("endedAt")
  public @Nullable OffsetDateTime getEndedAt() {
    return endedAt;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("endedAt")
  public void setEndedAt(@Nullable OffsetDateTime endedAt) {
    this.endedAt = endedAt;
  }

  public ActivityResponse detail(ActivityDetail detail) {
    this.detail = detail;
    return this;
  }

  /**
   * Get detail
   * @return detail
   */
  @NotNull @Valid
  @JsonProperty("detail")
  public ActivityDetail getDetail() {
    return detail;
  }

  @JsonProperty("detail")
  public void setDetail(ActivityDetail detail) {
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

  public ActivityResponse note(@Nullable String note) {
    this.note = note;
    return this;
  }

  /**
   * Get note
   * @return note
   */

  @JsonProperty("note")
  public @Nullable String getNote() {
    return note;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("note")
  public void setNote(@Nullable String note) {
    this.note = note;
  }

  public ActivityResponse syncState(@Nullable ActivitySyncState syncState) {
    this.syncState = syncState;
    return this;
  }

  /**
   * Get syncState
   * @return syncState
   */
  @Valid
  @JsonProperty("syncState")
  public @Nullable ActivitySyncState getSyncState() {
    return syncState;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("syncState")
  public void setSyncState(@Nullable ActivitySyncState syncState) {
    this.syncState = syncState;
  }

  public ActivityResponse syncReason(@Nullable String syncReason) {
    this.syncReason = syncReason;
    return this;
  }

  /**
   * Get syncReason
   * @return syncReason
   */

  @JsonProperty("syncReason")
  public @Nullable String getSyncReason() {
    return syncReason;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("syncReason")
  public void setSyncReason(@Nullable String syncReason) {
    this.syncReason = syncReason;
  }

  public ActivityResponse previousPerformedAt(@Nullable OffsetDateTime previousPerformedAt) {
    this.previousPerformedAt = previousPerformedAt;
    return this;
  }

  /**
   * Performed date immediately before the latest correction; invalidate both dates and their months.
   * @return previousPerformedAt
   */
  @Valid
  @JsonProperty("previousPerformedAt")
  public @Nullable OffsetDateTime getPreviousPerformedAt() {
    return previousPerformedAt;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("previousPerformedAt")
  public void setPreviousPerformedAt(@Nullable OffsetDateTime previousPerformedAt) {
    this.previousPerformedAt = previousPerformedAt;
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
        Objects.equals(this.startedAt, activityResponse.startedAt) &&
        Objects.equals(this.endedAt, activityResponse.endedAt) &&
        Objects.equals(this.detail, activityResponse.detail) &&
        Objects.equals(this.status, activityResponse.status) &&
        Objects.equals(this.version, activityResponse.version) &&
        Objects.equals(this.note, activityResponse.note) &&
        Objects.equals(this.syncState, activityResponse.syncState) &&
        Objects.equals(this.syncReason, activityResponse.syncReason) &&
        Objects.equals(this.previousPerformedAt, activityResponse.previousPerformedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(activityId, commandId, taskId, userId, activityType, performedAt, startedAt, endedAt, detail, status, version, note, syncState, syncReason, previousPerformedAt);
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
    sb.append("    startedAt: ").append(toIndentedString(startedAt)).append("\n");
    sb.append("    endedAt: ").append(toIndentedString(endedAt)).append("\n");
    sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    syncState: ").append(toIndentedString(syncState)).append("\n");
    sb.append("    syncReason: ").append(toIndentedString(syncReason)).append("\n");
    sb.append("    previousPerformedAt: ").append(toIndentedString(previousPerformedAt)).append("\n");
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
