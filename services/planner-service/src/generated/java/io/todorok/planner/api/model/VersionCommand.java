package io.todorok.planner.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * VersionCommand
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class VersionCommand {

  private Long version;

  public VersionCommand() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public VersionCommand(Long version) {
    this.version = version;
  }

  public VersionCommand version(Long version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * minimum: 0
   * @return version
   */
  @NotNull @Min(value = 0L)

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
    VersionCommand versionCommand = (VersionCommand) o;
    return Objects.equals(this.version, versionCommand.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VersionCommand {\n");
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
