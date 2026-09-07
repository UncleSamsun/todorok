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
 * RolloverResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class RolloverResponse {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate today;

  private Integer movedCount;

  public RolloverResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RolloverResponse(LocalDate today, Integer movedCount) {
    this.today = today;
    this.movedCount = movedCount;
  }

  public RolloverResponse today(LocalDate today) {
    this.today = today;
    return this;
  }

  /**
   * Get today
   * @return today
   */
  @NotNull @Valid
  @JsonProperty("today")
  public LocalDate getToday() {
    return today;
  }

  @JsonProperty("today")
  public void setToday(LocalDate today) {
    this.today = today;
  }

  public RolloverResponse movedCount(Integer movedCount) {
    this.movedCount = movedCount;
    return this;
  }

  /**
   * Get movedCount
   * minimum: 0
   * @return movedCount
   */
  @NotNull @Min(value = 0)
  @JsonProperty("movedCount")
  public Integer getMovedCount() {
    return movedCount;
  }

  @JsonProperty("movedCount")
  public void setMovedCount(Integer movedCount) {
    this.movedCount = movedCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RolloverResponse rolloverResponse = (RolloverResponse) o;
    return Objects.equals(this.today, rolloverResponse.today) &&
        Objects.equals(this.movedCount, rolloverResponse.movedCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(today, movedCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RolloverResponse {\n");
    sb.append("    today: ").append(toIndentedString(today)).append("\n");
    sb.append("    movedCount: ").append(toIndentedString(movedCount)).append("\n");
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
