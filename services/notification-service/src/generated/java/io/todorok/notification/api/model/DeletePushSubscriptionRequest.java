package io.todorok.notification.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * DeletePushSubscriptionRequest
 */

@JsonTypeName("deletePushSubscription_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class DeletePushSubscriptionRequest {

  private String endpoint;

  public DeletePushSubscriptionRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DeletePushSubscriptionRequest(String endpoint) {
    this.endpoint = endpoint;
  }

  public DeletePushSubscriptionRequest endpoint(String endpoint) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeletePushSubscriptionRequest deletePushSubscriptionRequest = (DeletePushSubscriptionRequest) o;
    return Objects.equals(this.endpoint, deletePushSubscriptionRequest.endpoint);
  }

  @Override
  public int hashCode() {
    return Objects.hash(endpoint);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeletePushSubscriptionRequest {\n");
    sb.append("    endpoint: ").append(toIndentedString(endpoint)).append("\n");
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
