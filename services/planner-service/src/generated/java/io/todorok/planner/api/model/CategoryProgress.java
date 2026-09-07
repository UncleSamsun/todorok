package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.todorok.planner.api.model.TaskType;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CategoryProgress
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class CategoryProgress {

  private TaskType taskType;

  private Integer totalCount;

  private Integer completedCount;

  public CategoryProgress() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CategoryProgress(TaskType taskType, Integer totalCount, Integer completedCount) {
    this.taskType = taskType;
    this.totalCount = totalCount;
    this.completedCount = completedCount;
  }

  public CategoryProgress taskType(TaskType taskType) {
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

  public CategoryProgress totalCount(Integer totalCount) {
    this.totalCount = totalCount;
    return this;
  }

  /**
   * Get totalCount
   * minimum: 0
   * @return totalCount
   */
  @NotNull @Min(value = 0)
  @JsonProperty("totalCount")
  public Integer getTotalCount() {
    return totalCount;
  }

  @JsonProperty("totalCount")
  public void setTotalCount(Integer totalCount) {
    this.totalCount = totalCount;
  }

  public CategoryProgress completedCount(Integer completedCount) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CategoryProgress categoryProgress = (CategoryProgress) o;
    return Objects.equals(this.taskType, categoryProgress.taskType) &&
        Objects.equals(this.totalCount, categoryProgress.totalCount) &&
        Objects.equals(this.completedCount, categoryProgress.completedCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskType, totalCount, completedCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CategoryProgress {\n");
    sb.append("    taskType: ").append(toIndentedString(taskType)).append("\n");
    sb.append("    totalCount: ").append(toIndentedString(totalCount)).append("\n");
    sb.append("    completedCount: ").append(toIndentedString(completedCount)).append("\n");
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

