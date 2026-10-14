package io.todorok.notification.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.todorok.notification.api.model.PushSubscriptionRequestKeys;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * PushSubscriptionRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class PushSubscriptionRequest {

  private String endpoint;

  private PushSubscriptionRequestKeys keys;

  public PushSubscriptionRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PushSubscriptionRequest(String endpoint, PushSubscriptionRequestKeys keys) {
    this.endpoint = endpoint;
    this.keys = keys;
  }

  public PushSubscriptionRequest endpoint(String endpoint) {
    this.endpoint = endpoint;
    return this;
  }

  /**
   * Get endpoint
   * @return endpoint
   */
  @NotNull @Size(max = 4096)
  @JsonProperty("endpoint")
  public String getEndpoint() {
    return endpoint;
  }

  @JsonProperty("endpoint")
  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public PushSubscriptionRequest keys(PushSubscriptionRequestKeys keys) {
    this.keys = keys;
    return this;
  }

  /**
   * Get keys
   * @return keys
   */
  @NotNull @Valid
  @JsonProperty("keys")
  public PushSubscriptionRequestKeys getKeys() {
    return keys;
  }

  @JsonProperty("keys")
  public void setKeys(PushSubscriptionRequestKeys keys) {
    this.keys = keys;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PushSubscriptionRequest pushSubscriptionRequest = (PushSubscriptionRequest) o;
    return Objects.equals(this.endpoint, pushSubscriptionRequest.endpoint) &&
        Objects.equals(this.keys, pushSubscriptionRequest.keys);
  }

  @Override
  public int hashCode() {
    return Objects.hash(endpoint, keys);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PushSubscriptionRequest {\n");
    sb.append("    endpoint: ").append(toIndentedString(endpoint)).append("\n");
    sb.append("    keys: ").append(toIndentedString(keys)).append("\n");
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
