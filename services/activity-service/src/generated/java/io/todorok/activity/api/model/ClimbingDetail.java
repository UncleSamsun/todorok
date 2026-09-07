package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.activity.api.model.ClimbingRound;
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
 * ClimbingDetail
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class ClimbingDetail {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer durationSeconds;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid ClimbingRound> rounds = new ArrayList<>();

  public ClimbingDetail durationSeconds(@Nullable Integer durationSeconds) {
    this.durationSeconds = durationSeconds;
    return this;
  }

  /**
   * Get durationSeconds
   * minimum: 0
   * maximum: 604800
   * @return durationSeconds
   */
  @Min(value = 0) @Max(value = 604800)
  @JsonProperty("durationSeconds")
  public @Nullable Integer getDurationSeconds() {
    return durationSeconds;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("durationSeconds")
  public void setDurationSeconds(@Nullable Integer durationSeconds) {
    this.durationSeconds = durationSeconds;
  }

  public ClimbingDetail rounds(List<@Valid ClimbingRound> rounds) {
    this.rounds = rounds;
    return this;
  }

  public ClimbingDetail addRoundsItem(ClimbingRound roundsItem) {
    if (this.rounds == null) {
      this.rounds = new ArrayList<>();
    }
    this.rounds.add(roundsItem);
    return this;
  }

  /**
   * Get rounds
   * @return rounds
   */
  @Valid @Size(max = 500)
  @JsonProperty("rounds")
  public List<@Valid ClimbingRound> getRounds() {
    return rounds;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("rounds")
  public void setRounds(List<@Valid ClimbingRound> rounds) {
    this.rounds = rounds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClimbingDetail climbingDetail = (ClimbingDetail) o;
    return Objects.equals(this.durationSeconds, climbingDetail.durationSeconds) &&
        Objects.equals(this.rounds, climbingDetail.rounds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(durationSeconds, rounds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClimbingDetail {\n");
    sb.append("    durationSeconds: ").append(toIndentedString(durationSeconds)).append("\n");
    sb.append("    rounds: ").append(toIndentedString(rounds)).append("\n");
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
