package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.todorok.planner.api.model.CalendarDaySummary;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CalendarSummaryResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class CalendarSummaryResponse {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate from;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate to;

  private List<@Valid CalendarDaySummary> days = new ArrayList<>();

  public CalendarSummaryResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CalendarSummaryResponse(LocalDate from, LocalDate to, List<@Valid CalendarDaySummary> days) {
    this.from = from;
    this.to = to;
    this.days = days;
  }

  public CalendarSummaryResponse from(LocalDate from) {
    this.from = from;
    return this;
  }

  /**
   * Get from
   * @return from
   */
  @NotNull @Valid

  @JsonProperty("from")
  public LocalDate getFrom() {
    return from;
  }

  @JsonProperty("from")
  public void setFrom(LocalDate from) {
    this.from = from;
  }

  public CalendarSummaryResponse to(LocalDate to) {
    this.to = to;
    return this;
  }

  /**
   * Get to
   * @return to
   */
  @NotNull @Valid

  @JsonProperty("to")
  public LocalDate getTo() {
    return to;
  }

  @JsonProperty("to")
  public void setTo(LocalDate to) {
    this.to = to;
  }

  public CalendarSummaryResponse days(List<@Valid CalendarDaySummary> days) {
    this.days = days;
    return this;
  }

  public CalendarSummaryResponse addDaysItem(CalendarDaySummary daysItem) {
    if (this.days == null) {
      this.days = new ArrayList<>();
    }
    this.days.add(daysItem);
    return this;
  }

  /**
   * Get days
   * @return days
   */
  @NotNull @Valid @Size(max = 42)

  @JsonProperty("days")
  public List<@Valid CalendarDaySummary> getDays() {
    return days;
  }

  @JsonProperty("days")
  public void setDays(List<@Valid CalendarDaySummary> days) {
    this.days = days;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CalendarSummaryResponse calendarSummaryResponse = (CalendarSummaryResponse) o;
    return Objects.equals(this.from, calendarSummaryResponse.from) &&
        Objects.equals(this.to, calendarSummaryResponse.to) &&
        Objects.equals(this.days, calendarSummaryResponse.days);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, to, days);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CalendarSummaryResponse {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
    sb.append("    days: ").append(toIndentedString(days)).append("\n");
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
