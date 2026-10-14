package io.todorok.notification.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.Nulls;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ListNotificationDeliveries200ResponseInner
 */

@JsonTypeName("listNotificationDeliveries_200_response_inner")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ListNotificationDeliveries200ResponseInner {

  private UUID id;

  /**
   * Gets or Sets status
   */
  public enum StatusEnum {
    PENDING("PENDING"),

    CANCELED("CANCELED"),

    EXPIRED("EXPIRED"),

    DELIVERED("DELIVERED"),

    FAILED("FAILED");

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

  private Integer attempts;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime scheduledFor;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String errorCode;

  public ListNotificationDeliveries200ResponseInner() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ListNotificationDeliveries200ResponseInner(UUID id, StatusEnum status, Integer attempts, OffsetDateTime scheduledFor) {
    this.id = id;
    this.status = status;
    this.attempts = attempts;
    this.scheduledFor = scheduledFor;
  }

  public ListNotificationDeliveries200ResponseInner id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull @Valid
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(UUID id) {
    this.id = id;
  }

  public ListNotificationDeliveries200ResponseInner status(StatusEnum status) {
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

  public ListNotificationDeliveries200ResponseInner attempts(Integer attempts) {
    this.attempts = attempts;
    return this;
  }

  /**
   * Get attempts
   * minimum: 0
   * @return attempts
   */
  @NotNull @Min(value = 0)
  @JsonProperty("attempts")
  public Integer getAttempts() {
    return attempts;
  }

  @JsonProperty("attempts")
  public void setAttempts(Integer attempts) {
    this.attempts = attempts;
  }

  public ListNotificationDeliveries200ResponseInner scheduledFor(OffsetDateTime scheduledFor) {
    this.scheduledFor = scheduledFor;
    return this;
  }

  /**
   * Get scheduledFor
   * @return scheduledFor
   */
  @NotNull @Valid
  @JsonProperty("scheduledFor")
  public OffsetDateTime getScheduledFor() {
    return scheduledFor;
  }

  @JsonProperty("scheduledFor")
  public void setScheduledFor(OffsetDateTime scheduledFor) {
    this.scheduledFor = scheduledFor;
  }

  public ListNotificationDeliveries200ResponseInner errorCode(@Nullable String errorCode) {
    this.errorCode = errorCode;
    return this;
  }

  /**
   * Get errorCode
   * @return errorCode
   */

  @JsonProperty("errorCode")
  public @Nullable String getErrorCode() {
    return errorCode;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("errorCode")
  public void setErrorCode(@Nullable String errorCode) {
    this.errorCode = errorCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListNotificationDeliveries200ResponseInner listNotificationDeliveries200ResponseInner = (ListNotificationDeliveries200ResponseInner) o;
    return Objects.equals(this.id, listNotificationDeliveries200ResponseInner.id) &&
        Objects.equals(this.status, listNotificationDeliveries200ResponseInner.status) &&
        Objects.equals(this.attempts, listNotificationDeliveries200ResponseInner.attempts) &&
        Objects.equals(this.scheduledFor, listNotificationDeliveries200ResponseInner.scheduledFor) &&
        Objects.equals(this.errorCode, listNotificationDeliveries200ResponseInner.errorCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, status, attempts, scheduledFor, errorCode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ListNotificationDeliveries200ResponseInner {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    attempts: ").append(toIndentedString(attempts)).append("\n");
    sb.append("    scheduledFor: ").append(toIndentedString(scheduledFor)).append("\n");
    sb.append("    errorCode: ").append(toIndentedString(errorCode)).append("\n");
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
