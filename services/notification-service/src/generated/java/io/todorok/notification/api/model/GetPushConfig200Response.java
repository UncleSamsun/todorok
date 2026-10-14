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
 * GetPushConfig200Response
 */

@JsonTypeName("getPushConfig_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class GetPushConfig200Response {

  private String publicKey;

  public GetPushConfig200Response() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GetPushConfig200Response(String publicKey) {
    this.publicKey = publicKey;
  }

  public GetPushConfig200Response publicKey(String publicKey) {
    this.publicKey = publicKey;
    return this;
  }

  /**
   * Get publicKey
   * @return publicKey
   */
  @NotNull
  @JsonProperty("publicKey")
  public String getPublicKey() {
    return publicKey;
  }

  @JsonProperty("publicKey")
  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetPushConfig200Response getPushConfig200Response = (GetPushConfig200Response) o;
    return Objects.equals(this.publicKey, getPushConfig200Response.publicKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(publicKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetPushConfig200Response {\n");
    sb.append("    publicKey: ").append(toIndentedString(publicKey)).append("\n");
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
