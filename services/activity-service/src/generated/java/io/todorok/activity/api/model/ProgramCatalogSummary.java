package io.todorok.activity.api.model;

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
 * ProgramCatalogSummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ProgramCatalogSummary {

  private String catalogKey;

  private Long catalogVersion;

  private String checksum;

  private String name;

  private Integer sessionsPerWeek;

  private Integer totalWeeks;

  public ProgramCatalogSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProgramCatalogSummary(String catalogKey, Long catalogVersion, String checksum, String name, Integer sessionsPerWeek, Integer totalWeeks) {
    this.catalogKey = catalogKey;
    this.catalogVersion = catalogVersion;
    this.checksum = checksum;
    this.name = name;
    this.sessionsPerWeek = sessionsPerWeek;
    this.totalWeeks = totalWeeks;
  }

  public ProgramCatalogSummary catalogKey(String catalogKey) {
    this.catalogKey = catalogKey;
    return this;
  }

  /**
   * Get catalogKey
   * @return catalogKey
   */
  @NotNull
  @JsonProperty("catalogKey")
  public String getCatalogKey() {
    return catalogKey;
  }

  @JsonProperty("catalogKey")
  public void setCatalogKey(String catalogKey) {
    this.catalogKey = catalogKey;
  }

  public ProgramCatalogSummary catalogVersion(Long catalogVersion) {
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

  public ProgramCatalogSummary checksum(String checksum) {
    this.checksum = checksum;
    return this;
  }

  /**
   * Get checksum
   * @return checksum
   */
  @NotNull
  @JsonProperty("checksum")
  public String getChecksum() {
    return checksum;
  }

  @JsonProperty("checksum")
  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public ProgramCatalogSummary name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public ProgramCatalogSummary sessionsPerWeek(Integer sessionsPerWeek) {
    this.sessionsPerWeek = sessionsPerWeek;
    return this;
  }

  /**
   * Get sessionsPerWeek
   * minimum: 1
   * @return sessionsPerWeek
   */
  @NotNull @Min(value = 1)
  @JsonProperty("sessionsPerWeek")
  public Integer getSessionsPerWeek() {
    return sessionsPerWeek;
  }

  @JsonProperty("sessionsPerWeek")
  public void setSessionsPerWeek(Integer sessionsPerWeek) {
    this.sessionsPerWeek = sessionsPerWeek;
  }

  public ProgramCatalogSummary totalWeeks(Integer totalWeeks) {
    this.totalWeeks = totalWeeks;
    return this;
  }

  /**
   * Get totalWeeks
   * minimum: 1
   * @return totalWeeks
   */
  @NotNull @Min(value = 1)
  @JsonProperty("totalWeeks")
  public Integer getTotalWeeks() {
    return totalWeeks;
  }

  @JsonProperty("totalWeeks")
  public void setTotalWeeks(Integer totalWeeks) {
    this.totalWeeks = totalWeeks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProgramCatalogSummary programCatalogSummary = (ProgramCatalogSummary) o;
    return Objects.equals(this.catalogKey, programCatalogSummary.catalogKey) &&
        Objects.equals(this.catalogVersion, programCatalogSummary.catalogVersion) &&
        Objects.equals(this.checksum, programCatalogSummary.checksum) &&
        Objects.equals(this.name, programCatalogSummary.name) &&
        Objects.equals(this.sessionsPerWeek, programCatalogSummary.sessionsPerWeek) &&
        Objects.equals(this.totalWeeks, programCatalogSummary.totalWeeks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(catalogKey, catalogVersion, checksum, name, sessionsPerWeek, totalWeeks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProgramCatalogSummary {\n");
    sb.append("    catalogKey: ").append(toIndentedString(catalogKey)).append("\n");
    sb.append("    catalogVersion: ").append(toIndentedString(catalogVersion)).append("\n");
    sb.append("    checksum: ").append(toIndentedString(checksum)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    sessionsPerWeek: ").append(toIndentedString(sessionsPerWeek)).append("\n");
    sb.append("    totalWeeks: ").append(toIndentedString(totalWeeks)).append("\n");
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
