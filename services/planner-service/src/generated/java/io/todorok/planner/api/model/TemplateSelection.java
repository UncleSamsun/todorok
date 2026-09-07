package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * TemplateSelection
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class TemplateSelection {

  private UUID templateId;

  private Long expectedTemplateVersion;

  public TemplateSelection() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TemplateSelection(UUID templateId, Long expectedTemplateVersion) {
    this.templateId = templateId;
    this.expectedTemplateVersion = expectedTemplateVersion;
  }

  public TemplateSelection templateId(UUID templateId) {
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

  public TemplateSelection expectedTemplateVersion(Long expectedTemplateVersion) {
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
    TemplateSelection templateSelection = (TemplateSelection) o;
    return Objects.equals(this.templateId, templateSelection.templateId) &&
        Objects.equals(this.expectedTemplateVersion, templateSelection.expectedTemplateVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(templateId, expectedTemplateVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TemplateSelection {\n");
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
