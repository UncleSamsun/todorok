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
 * PushSubscriptionRequestKeys
 */

@JsonTypeName("PushSubscriptionRequest_keys")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class PushSubscriptionRequestKeys {

  private String p256dh;

  private String auth;

  public PushSubscriptionRequestKeys() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PushSubscriptionRequestKeys(String p256dh, String auth) {
    this.p256dh = p256dh;
    this.auth = auth;
  }

  public PushSubscriptionRequestKeys p256dh(String p256dh) {
    this.p256dh = p256dh;
    return this;
  }

  /**
   * Get p256dh
   * @return p256dh
   */
  @NotNull @Size(min = 87, max = 88)
  @JsonProperty("p256dh")
  public String getP256dh() {
    return p256dh;
  }

  @JsonProperty("p256dh")
  public void setP256dh(String p256dh) {
    this.p256dh = p256dh;
  }

  public PushSubscriptionRequestKeys auth(String auth) {
    this.auth = auth;
    return this;
  }

  /**
   * Get auth
   * @return auth
   */
  @NotNull @Size(min = 22, max = 24)
  @JsonProperty("auth")
  public String getAuth() {
    return auth;
  }

  @JsonProperty("auth")
  public void setAuth(String auth) {
    this.auth = auth;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PushSubscriptionRequestKeys pushSubscriptionRequestKeys = (PushSubscriptionRequestKeys) o;
    return Objects.equals(this.p256dh, pushSubscriptionRequestKeys.p256dh) &&
        Objects.equals(this.auth, pushSubscriptionRequestKeys.auth);
  }

  @Override
  public int hashCode() {
    return Objects.hash(p256dh, auth);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PushSubscriptionRequestKeys {\n");
    sb.append("    p256dh: ").append(toIndentedString(p256dh)).append("\n");
    sb.append("    auth: ").append(toIndentedString(auth)).append("\n");
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
