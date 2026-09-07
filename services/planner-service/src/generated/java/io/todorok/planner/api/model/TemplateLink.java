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
 * TemplateLink
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class TemplateLink {

  private UUID bindingId;

  private UUID templateId;

  private Long selectedTemplateVersion;

  private String name;

  private String fieldSummary;

  public TemplateLink() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TemplateLink(UUID bindingId, UUID templateId, Long selectedTemplateVersion, String name, String fieldSummary) {
    this.bindingId = bindingId;
    this.templateId = templateId;
    this.selectedTemplateVersion = selectedTemplateVersion;
    this.name = name;
    this.fieldSummary = fieldSummary;
  }

  public TemplateLink bindingId(UUID bindingId) {
    this.bindingId = bindingId;
    return this;
  }

  /**
   * Get bindingId
   * @return bindingId
   */
  @NotNull @Valid

  @JsonProperty("bindingId")
  public UUID getBindingId() {
    return bindingId;
  }

  @JsonProperty("bindingId")
  public void setBindingId(UUID bindingId) {
    this.bindingId = bindingId;
  }

  public TemplateLink templateId(UUID templateId) {
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

  public TemplateLink selectedTemplateVersion(Long selectedTemplateVersion) {
    this.selectedTemplateVersion = selectedTemplateVersion;
    return this;
  }

  /**
   * Get selectedTemplateVersion
   * minimum: 1
   * @return selectedTemplateVersion
   */
  @NotNull @Min(value = 1L)

  @JsonProperty("selectedTemplateVersion")
  public Long getSelectedTemplateVersion() {
    return selectedTemplateVersion;
  }

  @JsonProperty("selectedTemplateVersion")
  public void setSelectedTemplateVersion(Long selectedTemplateVersion) {
    this.selectedTemplateVersion = selectedTemplateVersion;
  }

  public TemplateLink name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(min = 1, max = 120)

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public TemplateLink fieldSummary(String fieldSummary) {
    this.fieldSummary = fieldSummary;
    return this;
  }

  /**
   * Get fieldSummary
   * @return fieldSummary
   */
  @NotNull @Size(max = 240)

  @JsonProperty("fieldSummary")
  public String getFieldSummary() {
    return fieldSummary;
  }

  @JsonProperty("fieldSummary")
  public void setFieldSummary(String fieldSummary) {
    this.fieldSummary = fieldSummary;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TemplateLink templateLink = (TemplateLink) o;
    return Objects.equals(this.bindingId, templateLink.bindingId) &&
        Objects.equals(this.templateId, templateLink.templateId) &&
        Objects.equals(this.selectedTemplateVersion, templateLink.selectedTemplateVersion) &&
        Objects.equals(this.name, templateLink.name) &&
        Objects.equals(this.fieldSummary, templateLink.fieldSummary);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bindingId, templateId, selectedTemplateVersion, name, fieldSummary);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TemplateLink {\n");
    sb.append("    bindingId: ").append(toIndentedString(bindingId)).append("\n");
    sb.append("    templateId: ").append(toIndentedString(templateId)).append("\n");
    sb.append("    selectedTemplateVersion: ").append(toIndentedString(selectedTemplateVersion)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    fieldSummary: ").append(toIndentedString(fieldSummary)).append("\n");
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
