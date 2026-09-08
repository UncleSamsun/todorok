package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.todorok.planner.api.model.ThemeMode;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UpdateUserPreferencesRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class UpdateUserPreferencesRequest {

  private ThemeMode theme;

  private Boolean notificationsEnabled;

  private String summaryTime;

  private Long expectedRevision;

  public UpdateUserPreferencesRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateUserPreferencesRequest(ThemeMode theme, Boolean notificationsEnabled, String summaryTime, Long expectedRevision) {
    this.theme = theme;
    this.notificationsEnabled = notificationsEnabled;
    this.summaryTime = summaryTime;
    this.expectedRevision = expectedRevision;
  }

  public UpdateUserPreferencesRequest theme(ThemeMode theme) {
    this.theme = theme;
    return this;
  }

  /**
   * Get theme
   * @return theme
   */
  @NotNull @Valid

  @JsonProperty("theme")
  public ThemeMode getTheme() {
    return theme;
  }

  @JsonProperty("theme")
  public void setTheme(ThemeMode theme) {
    this.theme = theme;
  }

  public UpdateUserPreferencesRequest notificationsEnabled(Boolean notificationsEnabled) {
    this.notificationsEnabled = notificationsEnabled;
    return this;
  }

  /**
   * Get notificationsEnabled
   * @return notificationsEnabled
   */
  @NotNull

  @JsonProperty("notificationsEnabled")
  public Boolean getNotificationsEnabled() {
    return notificationsEnabled;
  }

  @JsonProperty("notificationsEnabled")
  public void setNotificationsEnabled(Boolean notificationsEnabled) {
    this.notificationsEnabled = notificationsEnabled;
  }

  public UpdateUserPreferencesRequest summaryTime(String summaryTime) {
    this.summaryTime = summaryTime;
    return this;
  }

  /**
   * Get summaryTime
   * @return summaryTime
   */
  @NotNull @Pattern(regexp = "^(?:[01][0-9]|2[0-3]):[0-5][0-9]$")

  @JsonProperty("summaryTime")
  public String getSummaryTime() {
    return summaryTime;
  }

  @JsonProperty("summaryTime")
  public void setSummaryTime(String summaryTime) {
    this.summaryTime = summaryTime;
  }

  public UpdateUserPreferencesRequest expectedRevision(Long expectedRevision) {
    this.expectedRevision = expectedRevision;
    return this;
  }

  /**
   * Get expectedRevision
   * minimum: 0
   * @return expectedRevision
   */
  @NotNull @Min(value = 0L)

  @JsonProperty("expectedRevision")
  public Long getExpectedRevision() {
    return expectedRevision;
  }

  @JsonProperty("expectedRevision")
  public void setExpectedRevision(Long expectedRevision) {
    this.expectedRevision = expectedRevision;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateUserPreferencesRequest updateUserPreferencesRequest = (UpdateUserPreferencesRequest) o;
    return Objects.equals(this.theme, updateUserPreferencesRequest.theme) &&
        Objects.equals(this.notificationsEnabled, updateUserPreferencesRequest.notificationsEnabled) &&
        Objects.equals(this.summaryTime, updateUserPreferencesRequest.summaryTime) &&
        Objects.equals(this.expectedRevision, updateUserPreferencesRequest.expectedRevision);
  }

  @Override
  public int hashCode() {
    return Objects.hash(theme, notificationsEnabled, summaryTime, expectedRevision);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateUserPreferencesRequest {\n");
    sb.append("    theme: ").append(toIndentedString(theme)).append("\n");
    sb.append("    notificationsEnabled: ").append(toIndentedString(notificationsEnabled)).append("\n");
    sb.append("    summaryTime: ").append(toIndentedString(summaryTime)).append("\n");
    sb.append("    expectedRevision: ").append(toIndentedString(expectedRevision)).append("\n");
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
