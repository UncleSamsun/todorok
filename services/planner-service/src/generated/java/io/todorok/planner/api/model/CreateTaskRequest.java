package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.planner.api.model.TaskType;
import io.todorok.planner.api.model.TemplateSelection;
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
 * CreateTaskRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class CreateTaskRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID commandId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable TemplateSelection templateSelection;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String note;

  private String title;

  private TaskType taskType;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate scheduledDate;

  public CreateTaskRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateTaskRequest(String title, TaskType taskType, LocalDate scheduledDate) {
    this.title = title;
    this.taskType = taskType;
    this.scheduledDate = scheduledDate;
  }

  public CreateTaskRequest commandId(@Nullable UUID commandId) {
    this.commandId = commandId;
    return this;
  }

  /**
   * Get commandId
   * @return commandId
   */
  @Valid

  @JsonProperty("commandId")
  public @Nullable UUID getCommandId() {
    return commandId;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("commandId")
  public void setCommandId(@Nullable UUID commandId) {
    this.commandId = commandId;
  }

  public CreateTaskRequest templateSelection(@Nullable TemplateSelection templateSelection) {
    this.templateSelection = templateSelection;
    return this;
  }

  /**
   * Get templateSelection
   * @return templateSelection
   */
  @Valid

  @JsonProperty("templateSelection")
  public @Nullable TemplateSelection getTemplateSelection() {
    return templateSelection;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("templateSelection")
  public void setTemplateSelection(@Nullable TemplateSelection templateSelection) {
    this.templateSelection = templateSelection;
  }

  public CreateTaskRequest note(@Nullable String note) {
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

  public CreateTaskRequest title(String title) {
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

  public CreateTaskRequest taskType(TaskType taskType) {
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

  public CreateTaskRequest scheduledDate(LocalDate scheduledDate) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateTaskRequest createTaskRequest = (CreateTaskRequest) o;
    return Objects.equals(this.commandId, createTaskRequest.commandId) &&
        Objects.equals(this.templateSelection, createTaskRequest.templateSelection) &&
        Objects.equals(this.note, createTaskRequest.note) &&
        Objects.equals(this.title, createTaskRequest.title) &&
        Objects.equals(this.taskType, createTaskRequest.taskType) &&
        Objects.equals(this.scheduledDate, createTaskRequest.scheduledDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(commandId, templateSelection, note, title, taskType, scheduledDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateTaskRequest {\n");
    sb.append("    commandId: ").append(toIndentedString(commandId)).append("\n");
    sb.append("    templateSelection: ").append(toIndentedString(templateSelection)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    taskType: ").append(toIndentedString(taskType)).append("\n");
    sb.append("    scheduledDate: ").append(toIndentedString(scheduledDate)).append("\n");
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
