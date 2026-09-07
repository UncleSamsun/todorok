package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Response-only original JSONB. Never interpret as validated template definitions or submit as input.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class LegacyStudyPayload {

  /**
   * Gets or Sets provenance
   */
  public enum ProvenanceEnum {
    UNVERIFIED_LEGACY("UNVERIFIED_LEGACY");

    private final String value;

    ProvenanceEnum(String value) {
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
    public static ProvenanceEnum fromValue(String value) {
      for (ProvenanceEnum b : ProvenanceEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private ProvenanceEnum provenance;

  private @Nullable Object values = null;

  private @Nullable Object snapshot = null;

  public LegacyStudyPayload() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LegacyStudyPayload(ProvenanceEnum provenance) {
    this.provenance = provenance;
  }

  public LegacyStudyPayload provenance(ProvenanceEnum provenance) {
    this.provenance = provenance;
    return this;
  }

  /**
   * Get provenance
   * @return provenance
   */
  @NotNull
  @JsonProperty("provenance")
  public ProvenanceEnum getProvenance() {
    return provenance;
  }

  @JsonProperty("provenance")
  public void setProvenance(ProvenanceEnum provenance) {
    this.provenance = provenance;
  }

  public LegacyStudyPayload values(@Nullable Object values) {
    this.values = values;
    return this;
  }

  /**
   * Get values
   * @return values
   */

  @JsonProperty("values")
  public @Nullable Object getValues() {
    return values;
  }

  @JsonProperty("values")
  public void setValues(@Nullable Object values) {
    this.values = values;
  }

  public LegacyStudyPayload snapshot(@Nullable Object snapshot) {
    this.snapshot = snapshot;
    return this;
  }

  /**
   * Get snapshot
   * @return snapshot
   */

  @JsonProperty("snapshot")
  public @Nullable Object getSnapshot() {
    return snapshot;
  }

  @JsonProperty("snapshot")
  public void setSnapshot(@Nullable Object snapshot) {
    this.snapshot = snapshot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LegacyStudyPayload legacyStudyPayload = (LegacyStudyPayload) o;
    return Objects.equals(this.provenance, legacyStudyPayload.provenance) &&
        Objects.equals(this.values, legacyStudyPayload.values) &&
        Objects.equals(this.snapshot, legacyStudyPayload.snapshot);
  }

  @Override
  public int hashCode() {
    return Objects.hash(provenance, values, snapshot);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LegacyStudyPayload {\n");
    sb.append("    provenance: ").append(toIndentedString(provenance)).append("\n");
    sb.append("    values: ").append(toIndentedString(values)).append("\n");
    sb.append("    snapshot: ").append(toIndentedString(snapshot)).append("\n");
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
