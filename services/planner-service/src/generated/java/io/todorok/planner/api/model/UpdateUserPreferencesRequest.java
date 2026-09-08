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

  private Long expectedRevision;

  public UpdateUserPreferencesRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateUserPreferencesRequest(ThemeMode theme, Long expectedRevision) {
    this.theme = theme;
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
        Objects.equals(this.expectedRevision, updateUserPreferencesRequest.expectedRevision);
  }

  @Override
  public int hashCode() {
    return Objects.hash(theme, expectedRevision);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateUserPreferencesRequest {\n");
    sb.append("    theme: ").append(toIndentedString(theme)).append("\n");
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
