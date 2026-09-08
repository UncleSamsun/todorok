package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * EnrollProgramRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class EnrollProgramRequest {

  private UUID commandId;

  private String catalogKey;

  private Long catalogVersion;

  private Integer initialTestValue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer startWeek;

  public EnrollProgramRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EnrollProgramRequest(UUID commandId, String catalogKey, Long catalogVersion, Integer initialTestValue) {
    this.commandId = commandId;
    this.catalogKey = catalogKey;
    this.catalogVersion = catalogVersion;
    this.initialTestValue = initialTestValue;
  }

  public EnrollProgramRequest commandId(UUID commandId) {
    this.commandId = commandId;
    return this;
  }

  /**
   * Get commandId
   * @return commandId
   */
  @NotNull @Valid
  @JsonProperty("commandId")
  public UUID getCommandId() {
    return commandId;
  }

  @JsonProperty("commandId")
  public void setCommandId(UUID commandId) {
    this.commandId = commandId;
  }

  public EnrollProgramRequest catalogKey(String catalogKey) {
    this.catalogKey = catalogKey;
    return this;
  }

  /**
   * Get catalogKey
   * @return catalogKey
   */
  @NotNull @Size(min = 1, max = 120)
  @JsonProperty("catalogKey")
  public String getCatalogKey() {
    return catalogKey;
  }

  @JsonProperty("catalogKey")
  public void setCatalogKey(String catalogKey) {
    this.catalogKey = catalogKey;
  }

  public EnrollProgramRequest catalogVersion(Long catalogVersion) {
    this.catalogVersion = catalogVersion;
    return this;
  }

  /**
   * Get catalogVersion
   * minimum: 1
   * @return catalogVersion
   */
  @NotNull @Min(value = 1L)
  @JsonProperty("catalogVersion")
  public Long getCatalogVersion() {
    return catalogVersion;
  }

  @JsonProperty("catalogVersion")
  public void setCatalogVersion(Long catalogVersion) {
    this.catalogVersion = catalogVersion;
  }

  public EnrollProgramRequest initialTestValue(Integer initialTestValue) {
    this.initialTestValue = initialTestValue;
    return this;
  }

  /**
   * Get initialTestValue
   * minimum: 0
   * @return initialTestValue
   */
  @NotNull @Min(value = 0)
  @JsonProperty("initialTestValue")
  public Integer getInitialTestValue() {
    return initialTestValue;
  }

  @JsonProperty("initialTestValue")
  public void setInitialTestValue(Integer initialTestValue) {
    this.initialTestValue = initialTestValue;
  }

  public EnrollProgramRequest startWeek(@Nullable Integer startWeek) {
    this.startWeek = startWeek;
    return this;
  }

  /**
   * Get startWeek
   * minimum: 1
   * @return startWeek
   */
  @Min(value = 1)
  @JsonProperty("startWeek")
  public @Nullable Integer getStartWeek() {
    return startWeek;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("startWeek")
  public void setStartWeek(@Nullable Integer startWeek) {
    this.startWeek = startWeek;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnrollProgramRequest enrollProgramRequest = (EnrollProgramRequest) o;
    return Objects.equals(this.commandId, enrollProgramRequest.commandId) &&
        Objects.equals(this.catalogKey, enrollProgramRequest.catalogKey) &&
        Objects.equals(this.catalogVersion, enrollProgramRequest.catalogVersion) &&
        Objects.equals(this.initialTestValue, enrollProgramRequest.initialTestValue) &&
        Objects.equals(this.startWeek, enrollProgramRequest.startWeek);
  }

  @Override
  public int hashCode() {
    return Objects.hash(commandId, catalogKey, catalogVersion, initialTestValue, startWeek);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EnrollProgramRequest {\n");
    sb.append("    commandId: ").append(toIndentedString(commandId)).append("\n");
    sb.append("    catalogKey: ").append(toIndentedString(catalogKey)).append("\n");
    sb.append("    catalogVersion: ").append(toIndentedString(catalogVersion)).append("\n");
    sb.append("    initialTestValue: ").append(toIndentedString(initialTestValue)).append("\n");
    sb.append("    startWeek: ").append(toIndentedString(startWeek)).append("\n");
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
