package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * StudyDetail
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class StudyDetail {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String subject;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer durationMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Map<String, Object> values = new HashMap<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Map<String, Object> snapshot = new HashMap<>();

  public StudyDetail subject(@Nullable String subject) {
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

  public StudyDetail durationMinutes(@Nullable Integer durationMinutes) {
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

  public StudyDetail values(Map<String, Object> values) {
    this.values = values;
    return this;
  }

  public StudyDetail putValuesItem(String key, Object valuesItem) {
    if (this.values == null) {
      this.values = new HashMap<>();
    }
    this.values.put(key, valuesItem);
    return this;
  }

  /**
   * Get values
   * @return values
   */

  @JsonProperty("values")
  public Map<String, Object> getValues() {
    return values;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("values")
  public void setValues(Map<String, Object> values) {
    this.values = values;
  }

  public StudyDetail snapshot(Map<String, Object> snapshot) {
    this.snapshot = snapshot;
    return this;
  }

  public StudyDetail putSnapshotItem(String key, Object snapshotItem) {
    if (this.snapshot == null) {
      this.snapshot = new HashMap<>();
    }
    this.snapshot.put(key, snapshotItem);
    return this;
  }

  /**
   * Get snapshot
   * @return snapshot
   */

  @JsonProperty("snapshot")
  public Map<String, Object> getSnapshot() {
    return snapshot;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("snapshot")
  public void setSnapshot(Map<String, Object> snapshot) {
    this.snapshot = snapshot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudyDetail studyDetail = (StudyDetail) o;
    return Objects.equals(this.subject, studyDetail.subject) &&
        Objects.equals(this.durationMinutes, studyDetail.durationMinutes) &&
        Objects.equals(this.values, studyDetail.values) &&
        Objects.equals(this.snapshot, studyDetail.snapshot);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, durationMinutes, values, snapshot);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudyDetail {\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    durationMinutes: ").append(toIndentedString(durationMinutes)).append("\n");
    sb.append("    values: ").append(toIndentedString(values)).append("\n");
    sb.append("    snapshot: ").append(toIndentedString(snapshot)).append("\n");
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
