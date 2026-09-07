package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.activity.api.model.ActivityCompletionStatus;
import io.todorok.activity.api.model.ActivityDetail;
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
 * CreateActivityRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class CreateActivityRequest {

  private UUID commandId;

  private UUID taskId;

  private ActivityType activityType;

  private ActivityCompletionStatus completionStatus;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime performedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String note;

  private ActivityDetail detail;

  public CreateActivityRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateActivityRequest(UUID commandId, UUID taskId, ActivityType activityType, ActivityCompletionStatus completionStatus, OffsetDateTime performedAt, ActivityDetail detail) {
    this.commandId = commandId;
    this.taskId = taskId;
    this.activityType = activityType;
    this.completionStatus = completionStatus;
    this.performedAt = performedAt;
    this.detail = detail;
  }

  public CreateActivityRequest commandId(UUID commandId) {
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

  public CreateActivityRequest taskId(UUID taskId) {
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

  public CreateActivityRequest activityType(ActivityType activityType) {
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

  public CreateActivityRequest completionStatus(ActivityCompletionStatus completionStatus) {
    this.completionStatus = completionStatus;
    return this;
  }

  /**
   * Get completionStatus
   * @return completionStatus
   */
  @NotNull @Valid
  @JsonProperty("completionStatus")
  public ActivityCompletionStatus getCompletionStatus() {
    return completionStatus;
  }

  @JsonProperty("completionStatus")
  public void setCompletionStatus(ActivityCompletionStatus completionStatus) {
    this.completionStatus = completionStatus;
  }

  public CreateActivityRequest performedAt(OffsetDateTime performedAt) {
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

  public CreateActivityRequest startedAt(@Nullable OffsetDateTime startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  /**
   * Optional actual interval start; requires endedAt and same Seoul date as performedAt.
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

  public CreateActivityRequest endedAt(@Nullable OffsetDateTime endedAt) {
    this.endedAt = endedAt;
    return this;
  }

  /**
   * Optional actual interval end; must follow startedAt on the same Seoul date.
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

  public CreateActivityRequest note(@Nullable String note) {
    this.note = note;
    return this;
  }

  /**
   * Get note
   * @return note
   */
  @Size(max = 20000)
  @JsonProperty("note")
  public @Nullable String getNote() {
    return note;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("note")
  public void setNote(@Nullable String note) {
    this.note = note;
  }

  public CreateActivityRequest detail(ActivityDetail detail) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateActivityRequest createActivityRequest = (CreateActivityRequest) o;
    return Objects.equals(this.commandId, createActivityRequest.commandId) &&
        Objects.equals(this.taskId, createActivityRequest.taskId) &&
        Objects.equals(this.activityType, createActivityRequest.activityType) &&
        Objects.equals(this.completionStatus, createActivityRequest.completionStatus) &&
        Objects.equals(this.performedAt, createActivityRequest.performedAt) &&
        Objects.equals(this.startedAt, createActivityRequest.startedAt) &&
        Objects.equals(this.endedAt, createActivityRequest.endedAt) &&
        Objects.equals(this.note, createActivityRequest.note) &&
        Objects.equals(this.detail, createActivityRequest.detail);
  }

  @Override
  public int hashCode() {
    return Objects.hash(commandId, taskId, activityType, completionStatus, performedAt, startedAt, endedAt, note, detail);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateActivityRequest {\n");
    sb.append("    commandId: ").append(toIndentedString(commandId)).append("\n");
    sb.append("    taskId: ").append(toIndentedString(taskId)).append("\n");
    sb.append("    activityType: ").append(toIndentedString(activityType)).append("\n");
    sb.append("    completionStatus: ").append(toIndentedString(completionStatus)).append("\n");
    sb.append("    performedAt: ").append(toIndentedString(performedAt)).append("\n");
    sb.append("    startedAt: ").append(toIndentedString(startedAt)).append("\n");
    sb.append("    endedAt: ").append(toIndentedString(endedAt)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
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
