package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.lang.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * RecurrenceRule
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class RecurrenceRule {

  /**
   * Gets or Sets frequency
   */
  public enum FrequencyEnum {
    DAILY("DAILY"),

    WEEKLY("WEEKLY"),

    MONTHLY("MONTHLY");

    private final String value;

    FrequencyEnum(String value) {
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
    public static FrequencyEnum fromValue(String value) {
      for (FrequencyEnum b : FrequencyEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private FrequencyEnum frequency;

  private Integer interval;

  private Set<@Min(1) @Max(7)Integer> weekdays = new LinkedHashSet<>();

  private Integer monthDay;

  public RecurrenceRule() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RecurrenceRule(FrequencyEnum frequency, Integer interval, Set<@Min(1) @Max(7)Integer> weekdays, Integer monthDay) {
    this.frequency = frequency;
    this.interval = interval;
    this.weekdays = weekdays;
    this.monthDay = monthDay;
  }

  public RecurrenceRule frequency(FrequencyEnum frequency) {
    this.frequency = frequency;
    return this;
  }

  /**
   * Get frequency
   * @return frequency
   */
  @NotNull
  @JsonProperty("frequency")
  public FrequencyEnum getFrequency() {
    return frequency;
  }

  @JsonProperty("frequency")
  public void setFrequency(FrequencyEnum frequency) {
    this.frequency = frequency;
  }

  public RecurrenceRule interval(Integer interval) {
    this.interval = interval;
    return this;
  }

  /**
   * Get interval
   * minimum: 1
   * maximum: 365
   * @return interval
   */
  @NotNull @Min(value = 1) @Max(value = 365)
  @JsonProperty("interval")
  public Integer getInterval() {
    return interval;
  }

  @JsonProperty("interval")
  public void setInterval(Integer interval) {
    this.interval = interval;
  }

  public RecurrenceRule weekdays(Set<@Min(1) @Max(7)Integer> weekdays) {
    this.weekdays = weekdays;
    return this;
  }

  public RecurrenceRule addWeekdaysItem(Integer weekdaysItem) {
    if (this.weekdays == null) {
      this.weekdays = new LinkedHashSet<>();
    }
    this.weekdays.add(weekdaysItem);
    return this;
  }

  /**
   * Get weekdays
   * @return weekdays
   */
  @NotNull @Size(max = 7)
  @JsonProperty("weekdays")
  public Set<@Min(1) @Max(7)Integer> getWeekdays() {
    return weekdays;
  }

  @JsonDeserialize(as = LinkedHashSet.class)
  @JsonProperty("weekdays")
  public void setWeekdays(Set<@Min(1) @Max(7)Integer> weekdays) {
    this.weekdays = weekdays;
  }

  public RecurrenceRule monthDay(Integer monthDay) {
    this.monthDay = monthDay;
    return this;
  }

  /**
   * Get monthDay
   * minimum: 1
   * maximum: 31
   * @return monthDay
   */
  @NotNull @Min(value = 1) @Max(value = 31)
  @JsonProperty("monthDay")
  public Integer getMonthDay() {
    return monthDay;
  }

  @JsonProperty("monthDay")
  public void setMonthDay(Integer monthDay) {
    this.monthDay = monthDay;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecurrenceRule recurrenceRule = (RecurrenceRule) o;
    return Objects.equals(this.frequency, recurrenceRule.frequency) &&
        Objects.equals(this.interval, recurrenceRule.interval) &&
        Objects.equals(this.weekdays, recurrenceRule.weekdays) &&
        Objects.equals(this.monthDay, recurrenceRule.monthDay);
  }

  @Override
  public int hashCode() {
    return Objects.hash(frequency, interval, weekdays, monthDay);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecurrenceRule {\n");
    sb.append("    frequency: ").append(toIndentedString(frequency)).append("\n");
    sb.append("    interval: ").append(toIndentedString(interval)).append("\n");
    sb.append("    weekdays: ").append(toIndentedString(weekdays)).append("\n");
    sb.append("    monthDay: ").append(toIndentedString(monthDay)).append("\n");
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

