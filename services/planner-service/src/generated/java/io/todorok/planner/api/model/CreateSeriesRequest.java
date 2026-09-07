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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateSeriesRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class CreateSeriesRequest {

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

  public CreateSeriesRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateSeriesRequest(String title, TaskType taskType, LocalDate startDate, RecurrenceRule rule) {
    this.title = title;
    this.taskType = taskType;
    this.startDate = startDate;
    this.rule = rule;
  }

  public CreateSeriesRequest title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
   */
  @NotNull @Size(min = 1, max = 120)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public CreateSeriesRequest taskType(TaskType taskType) {
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

  public CreateSeriesRequest startDate(LocalDate startDate) {
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

  public CreateSeriesRequest endDate(@Nullable LocalDate endDate) {
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

  public CreateSeriesRequest note(@Nullable String note) {
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

  public CreateSeriesRequest rule(RecurrenceRule rule) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateSeriesRequest createSeriesRequest = (CreateSeriesRequest) o;
    return Objects.equals(this.title, createSeriesRequest.title) &&
        Objects.equals(this.taskType, createSeriesRequest.taskType) &&
        Objects.equals(this.startDate, createSeriesRequest.startDate) &&
        Objects.equals(this.endDate, createSeriesRequest.endDate) &&
        Objects.equals(this.note, createSeriesRequest.note) &&
        Objects.equals(this.rule, createSeriesRequest.rule);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, taskType, startDate, endDate, note, rule);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateSeriesRequest {\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    taskType: ").append(toIndentedString(taskType)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    rule: ").append(toIndentedString(rule)).append("\n");
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

