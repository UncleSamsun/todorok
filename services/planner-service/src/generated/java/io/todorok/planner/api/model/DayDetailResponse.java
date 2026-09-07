package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.todorok.planner.api.model.TaskResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * DayDetailResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class DayDetailResponse {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate date;

  private List<@Valid TaskResponse> tasks = new ArrayList<>();

  public DayDetailResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DayDetailResponse(LocalDate date, List<@Valid TaskResponse> tasks) {
    this.date = date;
    this.tasks = tasks;
  }

  public DayDetailResponse date(LocalDate date) {
    this.date = date;
    return this;
  }

  /**
   * Get date
   * @return date
   */
  @NotNull @Valid
  @JsonProperty("date")
  public LocalDate getDate() {
    return date;
  }

  @JsonProperty("date")
  public void setDate(LocalDate date) {
    this.date = date;
  }

  public DayDetailResponse tasks(List<@Valid TaskResponse> tasks) {
    this.tasks = tasks;
    return this;
  }

  public DayDetailResponse addTasksItem(TaskResponse tasksItem) {
    if (this.tasks == null) {
      this.tasks = new ArrayList<>();
    }
    this.tasks.add(tasksItem);
    return this;
  }

  /**
   * Get tasks
   * @return tasks
   */
  @NotNull @Valid
  @JsonProperty("tasks")
  public List<@Valid TaskResponse> getTasks() {
    return tasks;
  }

  @JsonProperty("tasks")
  public void setTasks(List<@Valid TaskResponse> tasks) {
    this.tasks = tasks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DayDetailResponse dayDetailResponse = (DayDetailResponse) o;
    return Objects.equals(this.date, dayDetailResponse.date) &&
        Objects.equals(this.tasks, dayDetailResponse.tasks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, tasks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DayDetailResponse {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    tasks: ").append(toIndentedString(tasks)).append("\n");
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

