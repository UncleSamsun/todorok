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
 * CalendarDaySummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class CalendarDaySummary {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate date;

  private Integer totalCount;

  private Integer completedCount;

  public CalendarDaySummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CalendarDaySummary(LocalDate date, Integer totalCount, Integer completedCount) {
    this.date = date;
    this.totalCount = totalCount;
    this.completedCount = completedCount;
  }

  public CalendarDaySummary date(LocalDate date) {
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

  public CalendarDaySummary totalCount(Integer totalCount) {
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

  public CalendarDaySummary completedCount(Integer completedCount) {
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
    CalendarDaySummary calendarDaySummary = (CalendarDaySummary) o;
    return Objects.equals(this.date, calendarDaySummary.date) &&
        Objects.equals(this.totalCount, calendarDaySummary.totalCount) &&
        Objects.equals(this.completedCount, calendarDaySummary.completedCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(date, totalCount, completedCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CalendarDaySummary {\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
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

