package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.planner.api.model.RecurrenceRule;
import io.todorok.planner.api.model.TaskType;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * SeriesResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class SeriesResponse {

  private UUID seriesId;

  private UUID userId;

  private String title;

  private TaskType taskType;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate endDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String note;

  private RecurrenceRule rule;

  private Boolean archived;

  private Long version;

  public SeriesResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SeriesResponse(UUID seriesId, UUID userId, String title, TaskType taskType, LocalDate startDate, RecurrenceRule rule, Boolean archived, Long version) {
    this.seriesId = seriesId;
    this.userId = userId;
    this.title = title;
    this.taskType = taskType;
    this.startDate = startDate;
    this.rule = rule;
    this.archived = archived;
    this.version = version;
  }

  public SeriesResponse seriesId(UUID seriesId) {
    this.seriesId = seriesId;
    return this;
  }

  /**
   * Get seriesId
   * @return seriesId
   */
  @NotNull @Valid
  @JsonProperty("seriesId")
  public UUID getSeriesId() {
    return seriesId;
  }

  @JsonProperty("seriesId")
  public void setSeriesId(UUID seriesId) {
    this.seriesId = seriesId;
  }

  public SeriesResponse userId(UUID userId) {
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

  public SeriesResponse title(String title) {
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

  public SeriesResponse taskType(TaskType taskType) {
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

  public SeriesResponse startDate(LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * Get startDate
   * @return startDate
   */
  @NotNull @Valid
  @JsonProperty("startDate")
  public LocalDate getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public SeriesResponse endDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * Get endDate
   * @return endDate
   */
  @Valid
  @JsonProperty("endDate")
  public @Nullable LocalDate getEndDate() {
    return endDate;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("endDate")
  public void setEndDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
  }

  public SeriesResponse note(@Nullable String note) {
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

  public SeriesResponse rule(RecurrenceRule rule) {
    this.rule = rule;
    return this;
  }

  /**
   * Get rule
   * @return rule
   */
  @NotNull @Valid
  @JsonProperty("rule")
  public RecurrenceRule getRule() {
    return rule;
  }

  @JsonProperty("rule")
  public void setRule(RecurrenceRule rule) {
    this.rule = rule;
  }

  public SeriesResponse archived(Boolean archived) {
    this.archived = archived;
    return this;
  }

  /**
   * Get archived
   * @return archived
   */
  @NotNull
  @JsonProperty("archived")
  public Boolean getArchived() {
    return archived;
  }

  @JsonProperty("archived")
  public void setArchived(Boolean archived) {
    this.archived = archived;
  }

  public SeriesResponse version(Long version) {
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
    SeriesResponse seriesResponse = (SeriesResponse) o;
    return Objects.equals(this.seriesId, seriesResponse.seriesId) &&
        Objects.equals(this.userId, seriesResponse.userId) &&
        Objects.equals(this.title, seriesResponse.title) &&
        Objects.equals(this.taskType, seriesResponse.taskType) &&
        Objects.equals(this.startDate, seriesResponse.startDate) &&
        Objects.equals(this.endDate, seriesResponse.endDate) &&
        Objects.equals(this.note, seriesResponse.note) &&
        Objects.equals(this.rule, seriesResponse.rule) &&
        Objects.equals(this.archived, seriesResponse.archived) &&
        Objects.equals(this.version, seriesResponse.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(seriesId, userId, title, taskType, startDate, endDate, note, rule, archived, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SeriesResponse {\n");
    sb.append("    seriesId: ").append(toIndentedString(seriesId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    taskType: ").append(toIndentedString(taskType)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    rule: ").append(toIndentedString(rule)).append("\n");
    sb.append("    archived: ").append(toIndentedString(archived)).append("\n");
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
