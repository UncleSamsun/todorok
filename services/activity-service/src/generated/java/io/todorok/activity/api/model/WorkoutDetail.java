package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.activity.api.model.FieldInput;
import io.todorok.activity.api.model.WorkoutSet;
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
 * WorkoutDetail
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class WorkoutDetail {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid WorkoutSet> sets = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid FieldInput> fields = new ArrayList<>();

  public WorkoutDetail sets(List<@Valid WorkoutSet> sets) {
    this.sets = sets;
    return this;
  }

  public WorkoutDetail addSetsItem(WorkoutSet setsItem) {
    if (this.sets == null) {
      this.sets = new ArrayList<>();
    }
    this.sets.add(setsItem);
    return this;
  }

  /**
   * Get sets
   * @return sets
   */
  @Valid @Size(max = 500)
  @JsonProperty("sets")
  public List<@Valid WorkoutSet> getSets() {
    return sets;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("sets")
  public void setSets(List<@Valid WorkoutSet> sets) {
    this.sets = sets;
  }

  public WorkoutDetail fields(List<@Valid FieldInput> fields) {
    this.fields = fields;
    return this;
  }

  public WorkoutDetail addFieldsItem(FieldInput fieldsItem) {
    if (this.fields == null) {
      this.fields = new ArrayList<>();
    }
    this.fields.add(fieldsItem);
    return this;
  }

  /**
   * Optional values for the server-selected free-workout template. Existing sets remain relational and separate.
   * @return fields
   */
  @Valid
  @JsonProperty("fields")
  public List<@Valid FieldInput> getFields() {
    return fields;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("fields")
  public void setFields(List<@Valid FieldInput> fields) {
    this.fields = fields;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkoutDetail workoutDetail = (WorkoutDetail) o;
    return Objects.equals(this.sets, workoutDetail.sets) &&
        Objects.equals(this.fields, workoutDetail.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sets, fields);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkoutDetail {\n");
    sb.append("    sets: ").append(toIndentedString(sets)).append("\n");
    sb.append("    fields: ").append(toIndentedString(fields)).append("\n");
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
