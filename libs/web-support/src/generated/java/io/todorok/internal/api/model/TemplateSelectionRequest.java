package io.todorok.internal.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * TemplateSelectionRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class TemplateSelectionRequest {

  private UUID requestId;

  private UUID ownerId;

  /**
   * Gets or Sets targetType
   */
  public enum TargetTypeEnum {
    TASK("TASK"),

    SERIES("SERIES");

    private final String value;

    TargetTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TargetTypeEnum fromValue(String value) {
      for (TargetTypeEnum b : TargetTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private TargetTypeEnum targetType;

  private UUID targetId;

  /**
   * Gets or Sets taskType
   */
  public enum TaskTypeEnum {
    STUDY("STUDY"),

    WORKOUT("WORKOUT"),

    CLIMBING("CLIMBING");

    private final String value;

    TaskTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TaskTypeEnum fromValue(String value) {
      for (TaskTypeEnum b : TaskTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private TaskTypeEnum taskType;

  private UUID templateId;

  private Long expectedTemplateVersion;

  public TemplateSelectionRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TemplateSelectionRequest(UUID requestId, UUID ownerId, TargetTypeEnum targetType, UUID targetId, TaskTypeEnum taskType, UUID templateId, Long expectedTemplateVersion) {
    this.requestId = requestId;
    this.ownerId = ownerId;
    this.targetType = targetType;
    this.targetId = targetId;
    this.taskType = taskType;
    this.templateId = templateId;
    this.expectedTemplateVersion = expectedTemplateVersion;
  }

  public TemplateSelectionRequest requestId(UUID requestId) {
    this.requestId = requestId;
    return this;
  }

  /**
   * Get requestId
   * @return requestId
   */
  @NotNull @Valid
  @JsonProperty("requestId")
  public UUID getRequestId() {
    return requestId;
  }

  @JsonProperty("requestId")
  public void setRequestId(UUID requestId) {
    this.requestId = requestId;
  }

  public TemplateSelectionRequest ownerId(UUID ownerId) {
    this.ownerId = ownerId;
    return this;
  }

  /**
   * Get ownerId
   * @return ownerId
   */
  @NotNull @Valid
  @JsonProperty("ownerId")
  public UUID getOwnerId() {
    return ownerId;
  }

  @JsonProperty("ownerId")
  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public TemplateSelectionRequest targetType(TargetTypeEnum targetType) {
    this.targetType = targetType;
    return this;
  }

  /**
   * Get targetType
   * @return targetType
   */
  @NotNull
  @JsonProperty("targetType")
  public TargetTypeEnum getTargetType() {
    return targetType;
  }

  @JsonProperty("targetType")
  public void setTargetType(TargetTypeEnum targetType) {
    this.targetType = targetType;
  }

  public TemplateSelectionRequest targetId(UUID targetId) {
    this.targetId = targetId;
    return this;
  }

  /**
   * Get targetId
   * @return targetId
   */
  @NotNull @Valid
  @JsonProperty("targetId")
  public UUID getTargetId() {
    return targetId;
  }

  @JsonProperty("targetId")
  public void setTargetId(UUID targetId) {
    this.targetId = targetId;
  }

  public TemplateSelectionRequest taskType(TaskTypeEnum taskType) {
    this.taskType = taskType;
    return this;
  }

  /**
   * Get taskType
   * @return taskType
   */
  @NotNull
  @JsonProperty("taskType")
  public TaskTypeEnum getTaskType() {
    return taskType;
  }

  @JsonProperty("taskType")
  public void setTaskType(TaskTypeEnum taskType) {
    this.taskType = taskType;
  }

  public TemplateSelectionRequest templateId(UUID templateId) {
    this.templateId = templateId;
    return this;
  }

  /**
   * Get templateId
   * @return templateId
   */
  @NotNull @Valid
  @JsonProperty("templateId")
  public UUID getTemplateId() {
    return templateId;
  }

  @JsonProperty("templateId")
  public void setTemplateId(UUID templateId) {
    this.templateId = templateId;
  }

  public TemplateSelectionRequest expectedTemplateVersion(Long expectedTemplateVersion) {
    this.expectedTemplateVersion = expectedTemplateVersion;
    return this;
  }

  /**
   * Get expectedTemplateVersion
   * minimum: 1
   * @return expectedTemplateVersion
   */
  @NotNull @Min(value = 1L)
  @JsonProperty("expectedTemplateVersion")
  public Long getExpectedTemplateVersion() {
    return expectedTemplateVersion;
  }

  @JsonProperty("expectedTemplateVersion")
  public void setExpectedTemplateVersion(Long expectedTemplateVersion) {
    this.expectedTemplateVersion = expectedTemplateVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TemplateSelectionRequest templateSelectionRequest = (TemplateSelectionRequest) o;
    return Objects.equals(this.requestId, templateSelectionRequest.requestId) &&
        Objects.equals(this.ownerId, templateSelectionRequest.ownerId) &&
        Objects.equals(this.targetType, templateSelectionRequest.targetType) &&
        Objects.equals(this.targetId, templateSelectionRequest.targetId) &&
        Objects.equals(this.taskType, templateSelectionRequest.taskType) &&
        Objects.equals(this.templateId, templateSelectionRequest.templateId) &&
        Objects.equals(this.expectedTemplateVersion, templateSelectionRequest.expectedTemplateVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestId, ownerId, targetType, targetId, taskType, templateId, expectedTemplateVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TemplateSelectionRequest {\n");
    sb.append("    requestId: ").append(toIndentedString(requestId)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
    sb.append("    targetType: ").append(toIndentedString(targetType)).append("\n");
    sb.append("    targetId: ").append(toIndentedString(targetId)).append("\n");
    sb.append("    taskType: ").append(toIndentedString(taskType)).append("\n");
    sb.append("    templateId: ").append(toIndentedString(templateId)).append("\n");
    sb.append("    expectedTemplateVersion: ").append(toIndentedString(expectedTemplateVersion)).append("\n");
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
