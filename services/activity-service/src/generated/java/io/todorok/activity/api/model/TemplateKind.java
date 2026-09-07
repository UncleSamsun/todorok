package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;
import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Gets or Sets TemplateKind
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public enum TemplateKind {

  STUDY_CATEGORY("STUDY_CATEGORY"),

  FREE_WORKOUT("FREE_WORKOUT"),

  FREE_HANGBOARD("FREE_HANGBOARD"),

  CLIMBING_SESSION("CLIMBING_SESSION");

  private final String value;

  TemplateKind(String value) {
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
  public static TemplateKind fromValue(String value) {
    for (TemplateKind b : TemplateKind.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
