package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.Nulls;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ProgramSource
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ProgramSource {

  /**
   * Gets or Sets kind
   */
  public enum KindEnum {
    SYNTHETIC("SYNTHETIC"),

    PRIVATE_VERIFIED("PRIVATE_VERIFIED");

    private final String value;

    KindEnum(String value) {
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
    public static KindEnum fromValue(String value) {
      for (KindEnum b : KindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private KindEnum kind;

  private String label;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable URI url;

  private List<@Size(min = 1, max = 500)String> conditions = new ArrayList<>();

  private List<@Size(min = 1, max = 500)String> cautions = new ArrayList<>();

  public ProgramSource() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProgramSource(KindEnum kind, String label, List<@Size(min = 1, max = 500)String> conditions, List<@Size(min = 1, max = 500)String> cautions) {
    this.kind = kind;
    this.label = label;
    this.conditions = conditions;
    this.cautions = cautions;
  }

  public ProgramSource kind(KindEnum kind) {
    this.kind = kind;
    return this;
  }

  /**
   * Get kind
   * @return kind
   */
  @NotNull
  @JsonProperty("kind")
  public KindEnum getKind() {
    return kind;
  }

  @JsonProperty("kind")
  public void setKind(KindEnum kind) {
    this.kind = kind;
  }

  public ProgramSource label(String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
   */
  @NotNull @Size(min = 1, max = 200)
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(String label) {
    this.label = label;
  }

  public ProgramSource url(@Nullable URI url) {
    this.url = url;
    return this;
  }

  /**
   * Get url
   * @return url
   */
  @Valid @Size(max = 2048)
  @JsonProperty("url")
  public @Nullable URI getUrl() {
    return url;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("url")
  public void setUrl(@Nullable URI url) {
    this.url = url;
  }

  public ProgramSource conditions(List<@Size(min = 1, max = 500)String> conditions) {
    this.conditions = conditions;
    return this;
  }

  public ProgramSource addConditionsItem(String conditionsItem) {
    if (this.conditions == null) {
      this.conditions = new ArrayList<>();
    }
    this.conditions.add(conditionsItem);
    return this;
  }

  /**
   * Get conditions
   * @return conditions
   */
  @NotNull
  @JsonProperty("conditions")
  public List<@Size(min = 1, max = 500)String> getConditions() {
    return conditions;
  }

  @JsonProperty("conditions")
  public void setConditions(List<@Size(min = 1, max = 500)String> conditions) {
    this.conditions = conditions;
  }

  public ProgramSource cautions(List<@Size(min = 1, max = 500)String> cautions) {
    this.cautions = cautions;
    return this;
  }

  public ProgramSource addCautionsItem(String cautionsItem) {
    if (this.cautions == null) {
      this.cautions = new ArrayList<>();
    }
    this.cautions.add(cautionsItem);
    return this;
  }

  /**
   * Get cautions
   * @return cautions
   */
  @NotNull
  @JsonProperty("cautions")
  public List<@Size(min = 1, max = 500)String> getCautions() {
    return cautions;
  }

  @JsonProperty("cautions")
  public void setCautions(List<@Size(min = 1, max = 500)String> cautions) {
    this.cautions = cautions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProgramSource programSource = (ProgramSource) o;
    return Objects.equals(this.kind, programSource.kind) &&
        Objects.equals(this.label, programSource.label) &&
        Objects.equals(this.url, programSource.url) &&
        Objects.equals(this.conditions, programSource.conditions) &&
        Objects.equals(this.cautions, programSource.cautions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, label, url, conditions, cautions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProgramSource {\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    conditions: ").append(toIndentedString(conditions)).append("\n");
    sb.append("    cautions: ").append(toIndentedString(cautions)).append("\n");
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
