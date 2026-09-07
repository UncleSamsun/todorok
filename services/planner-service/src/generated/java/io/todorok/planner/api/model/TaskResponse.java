package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.planner.api.model.TaskStatus;
import io.todorok.planner.api.model.TaskType;
import io.todorok.planner.api.model.TemplateLink;
import java.time.LocalDate;
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
 * TaskResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class TaskResponse {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable TemplateLink templateLink;

  private UUID taskId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID activityId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime performedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String completionSummary;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID seriesId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate occurrenceDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String note;

  private UUID userId;

  private String title;

  private TaskType taskType;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate scheduledDate;

  private TaskStatus status;

  private Long version;

  public TaskResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TaskResponse(UUID taskId, UUID userId, String title, TaskType taskType, LocalDate scheduledDate, TaskStatus status, Long version) {
    this.taskId = taskId;
    this.userId = userId;
    this.title = title;
    this.taskType = taskType;
    this.scheduledDate = scheduledDate;
    this.status = status;
    this.version = version;
  }

  public TaskResponse templateLink(@Nullable TemplateLink templateLink) {
    this.templateLink = templateLink;
    return this;
  }

  /**
   * Get templateLink
   * @return templateLink
   */
  @Valid

  @JsonProperty("templateLink")
  public @Nullable TemplateLink getTemplateLink() {
    return templateLink;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("templateLink")
  public void setTemplateLink(@Nullable TemplateLink templateLink) {
    this.templateLink = templateLink;
  }

  public TaskResponse taskId(UUID taskId) {
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

  public TaskResponse activityId(@Nullable UUID activityId) {
    this.activityId = activityId;
    return this;
  }

  /**
   * Get activityId
   * @return activityId
   */
  @Valid

  @JsonProperty("activityId")
  public @Nullable UUID getActivityId() {
    return activityId;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("activityId")
  public void setActivityId(@Nullable UUID activityId) {
    this.activityId = activityId;
  }

  public TaskResponse performedAt(@Nullable OffsetDateTime performedAt) {
    this.performedAt = performedAt;
    return this;
  }

  /**
   * Get performedAt
   * @return performedAt
   */
  @Valid

  @JsonProperty("performedAt")
  public @Nullable OffsetDateTime getPerformedAt() {
    return performedAt;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("performedAt")
  public void setPerformedAt(@Nullable OffsetDateTime performedAt) {
    this.performedAt = performedAt;
  }

  public TaskResponse completionSummary(@Nullable String completionSummary) {
    this.completionSummary = completionSummary;
    return this;
  }

  /**
   * Get completionSummary
   * @return completionSummary
   */


  @JsonProperty("completionSummary")
  public @Nullable String getCompletionSummary() {
    return completionSummary;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("completionSummary")
  public void setCompletionSummary(@Nullable String completionSummary) {
    this.completionSummary = completionSummary;
  }

  public TaskResponse startedAt(@Nullable OffsetDateTime startedAt) {
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

  public TaskResponse endedAt(@Nullable OffsetDateTime endedAt) {
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

  public TaskResponse seriesId(@Nullable UUID seriesId) {
    this.seriesId = seriesId;
    return this;
  }

  /**
   * Get seriesId
   * @return seriesId
   */
  @Valid

  @JsonProperty("seriesId")
  public @Nullable UUID getSeriesId() {
    return seriesId;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("seriesId")
  public void setSeriesId(@Nullable UUID seriesId) {
    this.seriesId = seriesId;
  }

  public TaskResponse occurrenceDate(@Nullable LocalDate occurrenceDate) {
    this.occurrenceDate = occurrenceDate;
    return this;
  }

  /**
   * Get occurrenceDate
   * @return occurrenceDate
   */
  @Valid

  @JsonProperty("occurrenceDate")
  public @Nullable LocalDate getOccurrenceDate() {
    return occurrenceDate;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("occurrenceDate")
  public void setOccurrenceDate(@Nullable LocalDate occurrenceDate) {
    this.occurrenceDate = occurrenceDate;
  }

  public TaskResponse note(@Nullable String note) {
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

  public TaskResponse userId(UUID userId) {
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

  public TaskResponse title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
   */
  @NotNull

  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public TaskResponse taskType(TaskType taskType) {
    this.taskType = taskType;
    return this;
  }

  /**
   * Get taskType
   * @return taskType
   */
  @NotNull @Valid

  @JsonProperty("taskType")
  public TaskType getTaskType() {
    return taskType;
  }

  @JsonProperty("taskType")
  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  public TaskResponse scheduledDate(LocalDate scheduledDate) {
    this.scheduledDate = scheduledDate;
    return this;
  }

  /**
   * Get scheduledDate
   * @return scheduledDate
   */
  @NotNull @Valid

  @JsonProperty("scheduledDate")
  public LocalDate getScheduledDate() {
    return scheduledDate;
  }

  @JsonProperty("scheduledDate")
  public void setScheduledDate(LocalDate scheduledDate) {
    this.scheduledDate = scheduledDate;
  }

  public TaskResponse status(TaskStatus status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull @Valid

  @JsonProperty("status")
  public TaskStatus getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public TaskResponse version(Long version) {
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
    TaskResponse taskResponse = (TaskResponse) o;
    return Objects.equals(this.templateLink, taskResponse.templateLink) &&
        Objects.equals(this.taskId, taskResponse.taskId) &&
        Objects.equals(this.activityId, taskResponse.activityId) &&
        Objects.equals(this.performedAt, taskResponse.performedAt) &&
        Objects.equals(this.completionSummary, taskResponse.completionSummary) &&
        Objects.equals(this.startedAt, taskResponse.startedAt) &&
        Objects.equals(this.endedAt, taskResponse.endedAt) &&
        Objects.equals(this.seriesId, taskResponse.seriesId) &&
        Objects.equals(this.occurrenceDate, taskResponse.occurrenceDate) &&
        Objects.equals(this.note, taskResponse.note) &&
        Objects.equals(this.userId, taskResponse.userId) &&
        Objects.equals(this.title, taskResponse.title) &&
        Objects.equals(this.taskType, taskResponse.taskType) &&
        Objects.equals(this.scheduledDate, taskResponse.scheduledDate) &&
        Objects.equals(this.status, taskResponse.status) &&
        Objects.equals(this.version, taskResponse.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(templateLink, taskId, activityId, performedAt, completionSummary, startedAt, endedAt, seriesId, occurrenceDate, note, userId, title, taskType, scheduledDate, status, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskResponse {\n");
    sb.append("    templateLink: ").append(toIndentedString(templateLink)).append("\n");
    sb.append("    taskId: ").append(toIndentedString(taskId)).append("\n");
    sb.append("    activityId: ").append(toIndentedString(activityId)).append("\n");
    sb.append("    performedAt: ").append(toIndentedString(performedAt)).append("\n");
    sb.append("    completionSummary: ").append(toIndentedString(completionSummary)).append("\n");
    sb.append("    startedAt: ").append(toIndentedString(startedAt)).append("\n");
    sb.append("    endedAt: ").append(toIndentedString(endedAt)).append("\n");
    sb.append("    seriesId: ").append(toIndentedString(seriesId)).append("\n");
    sb.append("    occurrenceDate: ").append(toIndentedString(occurrenceDate)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    taskType: ").append(toIndentedString(taskType)).append("\n");
    sb.append("    scheduledDate: ").append(toIndentedString(scheduledDate)).append("\n");
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
