package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.todorok.activity.api.model.ProgramSessionTarget;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ProgramEnrollmentResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ProgramEnrollmentResponse {

  private UUID enrollmentId;

  private String catalogKey;

  private Long catalogVersion;

  private Integer recommendedWeek;

  private Integer startWeek;

  private Integer currentWeek;

  private Integer currentSession;

  /**
   * Gets or Sets status
   */
  public enum StatusEnum {
    ACTIVE("ACTIVE"),

    COMPLETED("COMPLETED");

    private final String value;

    StatusEnum(String value) {
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
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private StatusEnum status;

  private ProgramSessionTarget target;

  public ProgramEnrollmentResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProgramEnrollmentResponse(UUID enrollmentId, String catalogKey, Long catalogVersion, Integer recommendedWeek, Integer startWeek, Integer currentWeek, Integer currentSession, StatusEnum status, ProgramSessionTarget target) {
    this.enrollmentId = enrollmentId;
    this.catalogKey = catalogKey;
    this.catalogVersion = catalogVersion;
    this.recommendedWeek = recommendedWeek;
    this.startWeek = startWeek;
    this.currentWeek = currentWeek;
    this.currentSession = currentSession;
    this.status = status;
    this.target = target;
  }

  public ProgramEnrollmentResponse enrollmentId(UUID enrollmentId) {
    this.enrollmentId = enrollmentId;
    return this;
  }

  /**
   * Get enrollmentId
   * @return enrollmentId
   */
  @NotNull @Valid
  @JsonProperty("enrollmentId")
  public UUID getEnrollmentId() {
    return enrollmentId;
  }

  @JsonProperty("enrollmentId")
  public void setEnrollmentId(UUID enrollmentId) {
    this.enrollmentId = enrollmentId;
  }

  public ProgramEnrollmentResponse catalogKey(String catalogKey) {
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

  public ProgramEnrollmentResponse catalogVersion(Long catalogVersion) {
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

  public ProgramEnrollmentResponse recommendedWeek(Integer recommendedWeek) {
    this.recommendedWeek = recommendedWeek;
    return this;
  }

  /**
   * Get recommendedWeek
   * minimum: 1
   * @return recommendedWeek
   */
  @NotNull @Min(value = 1)
  @JsonProperty("recommendedWeek")
  public Integer getRecommendedWeek() {
    return recommendedWeek;
  }

  @JsonProperty("recommendedWeek")
  public void setRecommendedWeek(Integer recommendedWeek) {
    this.recommendedWeek = recommendedWeek;
  }

  public ProgramEnrollmentResponse startWeek(Integer startWeek) {
    this.startWeek = startWeek;
    return this;
  }

  /**
   * Get startWeek
   * minimum: 1
   * @return startWeek
   */
  @NotNull @Min(value = 1)
  @JsonProperty("startWeek")
  public Integer getStartWeek() {
    return startWeek;
  }

  @JsonProperty("startWeek")
  public void setStartWeek(Integer startWeek) {
    this.startWeek = startWeek;
  }

  public ProgramEnrollmentResponse currentWeek(Integer currentWeek) {
    this.currentWeek = currentWeek;
    return this;
  }

  /**
   * Get currentWeek
   * minimum: 1
   * @return currentWeek
   */
  @NotNull @Min(value = 1)
  @JsonProperty("currentWeek")
  public Integer getCurrentWeek() {
    return currentWeek;
  }

  @JsonProperty("currentWeek")
  public void setCurrentWeek(Integer currentWeek) {
    this.currentWeek = currentWeek;
  }

  public ProgramEnrollmentResponse currentSession(Integer currentSession) {
    this.currentSession = currentSession;
    return this;
  }

  /**
   * Get currentSession
   * minimum: 1
   * @return currentSession
   */
  @NotNull @Min(value = 1)
  @JsonProperty("currentSession")
  public Integer getCurrentSession() {
    return currentSession;
  }

  @JsonProperty("currentSession")
  public void setCurrentSession(Integer currentSession) {
    this.currentSession = currentSession;
  }

  public ProgramEnrollmentResponse status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public ProgramEnrollmentResponse target(ProgramSessionTarget target) {
    this.target = target;
    return this;
  }

  /**
   * Get target
   * @return target
   */
  @NotNull @Valid
  @JsonProperty("target")
  public ProgramSessionTarget getTarget() {
    return target;
  }

  @JsonProperty("target")
  public void setTarget(ProgramSessionTarget target) {
    this.target = target;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProgramEnrollmentResponse programEnrollmentResponse = (ProgramEnrollmentResponse) o;
    return Objects.equals(this.enrollmentId, programEnrollmentResponse.enrollmentId) &&
        Objects.equals(this.catalogKey, programEnrollmentResponse.catalogKey) &&
        Objects.equals(this.catalogVersion, programEnrollmentResponse.catalogVersion) &&
        Objects.equals(this.recommendedWeek, programEnrollmentResponse.recommendedWeek) &&
        Objects.equals(this.startWeek, programEnrollmentResponse.startWeek) &&
        Objects.equals(this.currentWeek, programEnrollmentResponse.currentWeek) &&
        Objects.equals(this.currentSession, programEnrollmentResponse.currentSession) &&
        Objects.equals(this.status, programEnrollmentResponse.status) &&
        Objects.equals(this.target, programEnrollmentResponse.target);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enrollmentId, catalogKey, catalogVersion, recommendedWeek, startWeek, currentWeek, currentSession, status, target);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProgramEnrollmentResponse {\n");
    sb.append("    enrollmentId: ").append(toIndentedString(enrollmentId)).append("\n");
    sb.append("    catalogKey: ").append(toIndentedString(catalogKey)).append("\n");
    sb.append("    catalogVersion: ").append(toIndentedString(catalogVersion)).append("\n");
    sb.append("    recommendedWeek: ").append(toIndentedString(recommendedWeek)).append("\n");
    sb.append("    startWeek: ").append(toIndentedString(startWeek)).append("\n");
    sb.append("    currentWeek: ").append(toIndentedString(currentWeek)).append("\n");
    sb.append("    currentSession: ").append(toIndentedString(currentSession)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    target: ").append(toIndentedString(target)).append("\n");
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
