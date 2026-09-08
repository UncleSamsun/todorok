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
 * UserPreferencesResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class UserPreferencesResponse {

  private ThemeMode theme;

  private Long revision;

  public UserPreferencesResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UserPreferencesResponse(ThemeMode theme, Long revision) {
    this.theme = theme;
    this.revision = revision;
  }

  public UserPreferencesResponse theme(ThemeMode theme) {
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

  public UserPreferencesResponse revision(Long revision) {
    this.revision = revision;
    return this;
  }

  /**
   * Get revision
   * minimum: 0
   * @return revision
   */
  @NotNull @Min(value = 0L)

  @JsonProperty("revision")
  public Long getRevision() {
    return revision;
  }

  @JsonProperty("revision")
  public void setRevision(Long revision) {
    this.revision = revision;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserPreferencesResponse userPreferencesResponse = (UserPreferencesResponse) o;
    return Objects.equals(this.theme, userPreferencesResponse.theme) &&
        Objects.equals(this.revision, userPreferencesResponse.revision);
  }

  @Override
  public int hashCode() {
    return Objects.hash(theme, revision);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserPreferencesResponse {\n");
    sb.append("    theme: ").append(toIndentedString(theme)).append("\n");
    sb.append("    revision: ").append(toIndentedString(revision)).append("\n");
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
