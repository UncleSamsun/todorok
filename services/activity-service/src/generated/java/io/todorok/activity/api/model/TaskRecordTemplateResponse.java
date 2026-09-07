package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.activity.api.model.TemplateLink;
import io.todorok.activity.api.model.TemplateResponse;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * TaskRecordTemplateResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class TaskRecordTemplateResponse {

  private Boolean linked;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable TemplateLink templateLink;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable TemplateResponse template;

  public TaskRecordTemplateResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TaskRecordTemplateResponse(Boolean linked) {
    this.linked = linked;
  }

  public TaskRecordTemplateResponse linked(Boolean linked) {
    this.linked = linked;
    return this;
  }

  /**
   * Get linked
   * @return linked
   */
  @NotNull
  @JsonProperty("linked")
  public Boolean getLinked() {
    return linked;
  }

  @JsonProperty("linked")
  public void setLinked(Boolean linked) {
    this.linked = linked;
  }

  public TaskRecordTemplateResponse templateLink(@Nullable TemplateLink templateLink) {
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

  public TaskRecordTemplateResponse template(@Nullable TemplateResponse template) {
    this.template = template;
    return this;
  }

  /**
   * Get template
   * @return template
   */
  @Valid
  @JsonProperty("template")
  public @Nullable TemplateResponse getTemplate() {
    return template;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("template")
  public void setTemplate(@Nullable TemplateResponse template) {
    this.template = template;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskRecordTemplateResponse taskRecordTemplateResponse = (TaskRecordTemplateResponse) o;
    return Objects.equals(this.linked, taskRecordTemplateResponse.linked) &&
        Objects.equals(this.templateLink, taskRecordTemplateResponse.templateLink) &&
        Objects.equals(this.template, taskRecordTemplateResponse.template);
  }

  @Override
  public int hashCode() {
    return Objects.hash(linked, templateLink, template);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskRecordTemplateResponse {\n");
    sb.append("    linked: ").append(toIndentedString(linked)).append("\n");
    sb.append("    templateLink: ").append(toIndentedString(templateLink)).append("\n");
    sb.append("    template: ").append(toIndentedString(template)).append("\n");
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
