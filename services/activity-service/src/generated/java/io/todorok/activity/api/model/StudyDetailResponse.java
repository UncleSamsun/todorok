package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.activity.api.model.FieldInput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * StudyDetailResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class StudyDetailResponse {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String subject;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer durationMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid FieldInput> fields = new ArrayList<>();

  public StudyDetailResponse subject(@Nullable String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * Get subject
   * @return subject
   */
  @Size(max = 120)
  @JsonProperty("subject")
  public @Nullable String getSubject() {
    return subject;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("subject")
  public void setSubject(@Nullable String subject) {
    this.subject = subject;
  }

  public StudyDetailResponse durationMinutes(@Nullable Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
    return this;
  }

  /**
   * Get durationMinutes
   * minimum: 0
   * maximum: 10080
   * @return durationMinutes
   */
  @Min(value = 0) @Max(value = 10080)
  @JsonProperty("durationMinutes")
  public @Nullable Integer getDurationMinutes() {
    return durationMinutes;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("durationMinutes")
  public void setDurationMinutes(@Nullable Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public StudyDetailResponse fields(List<@Valid FieldInput> fields) {
    this.fields = fields;
    return this;
  }

  public StudyDetailResponse addFieldsItem(FieldInput fieldsItem) {
    if (this.fields == null) {
      this.fields = new ArrayList<>();
    }
    this.fields.add(fieldsItem);
    return this;
  }

  /**
   * Get fields
   * @return fields
   */
  @Valid
  @JsonProperty("fields")
  public List<@Valid FieldInput> getFields() {
    return fields;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("fields")
  public void setFields(List<@Valid FieldInput> fields) {
    this.fields = fields;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudyDetailResponse studyDetailResponse = (StudyDetailResponse) o;
    return Objects.equals(this.subject, studyDetailResponse.subject) &&
        Objects.equals(this.durationMinutes, studyDetailResponse.durationMinutes) &&
        Objects.equals(this.fields, studyDetailResponse.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, durationMinutes, fields);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudyDetailResponse {\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    durationMinutes: ").append(toIndentedString(durationMinutes)).append("\n");
    sb.append("    fields: ").append(toIndentedString(fields)).append("\n");
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
