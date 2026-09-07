package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * DailyNoteResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class DailyNoteResponse {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate date;

  private String content;

  private Long version = null;

  public DailyNoteResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DailyNoteResponse(LocalDate date, String content, Long version) {
    this.date = date;
    this.content = content;
    this.version = version;
  }

  public DailyNoteResponse date(LocalDate date) {
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

  public DailyNoteResponse content(String content) {
    this.content = content;
    return this;
  }

  /**
   * Get content
   * @return content
   */
  @NotNull @Size(max = 20000)

  @JsonProperty("content")
  public String getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(String content) {
    this.content = content;
  }

  public DailyNoteResponse version(Long version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * minimum: 0
   * @return version
   */
  @Min(value = 0L)

  @JsonProperty("version")
  public Long getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(Long version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DailyNoteResponse dailyNoteResponse = (DailyNoteResponse) o;
    return Objects.equals(this.date, dailyNoteResponse.date) &&
        Objects.equals(this.content, dailyNoteResponse.content) &&
        Objects.equals(this.version, dailyNoteResponse.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, content, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DailyNoteResponse {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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
