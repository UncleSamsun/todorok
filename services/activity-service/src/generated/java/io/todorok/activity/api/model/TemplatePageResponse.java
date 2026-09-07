package io.todorok.activity.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.todorok.activity.api.model.TemplateResponse;
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
 * TemplatePageResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.24.0")
public class TemplatePageResponse {

  private List<@Valid TemplateResponse> items = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String nextCursor;

  public TemplatePageResponse() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TemplatePageResponse(List<@Valid TemplateResponse> items) {
    this.items = items;
  }

  public TemplatePageResponse items(List<@Valid TemplateResponse> items) {
    this.items = items;
    return this;
  }

  public TemplatePageResponse addItemsItem(TemplateResponse itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * Get items
   * @return items
   */
  @NotNull @Valid
  @JsonProperty("items")
  public List<@Valid TemplateResponse> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid TemplateResponse> items) {
    this.items = items;
  }

  public TemplatePageResponse nextCursor(@Nullable String nextCursor) {
    this.nextCursor = nextCursor;
    return this;
  }

  /**
   * Get nextCursor
   * @return nextCursor
   */

  @JsonProperty("nextCursor")
  public @Nullable String getNextCursor() {
    return nextCursor;
  }

  @JsonSetter(nulls = Nulls.SKIP)
  @JsonProperty("nextCursor")
  public void setNextCursor(@Nullable String nextCursor) {
    this.nextCursor = nextCursor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TemplatePageResponse templatePageResponse = (TemplatePageResponse) o;
    return Objects.equals(this.items, templatePageResponse.items) &&
        Objects.equals(this.nextCursor, templatePageResponse.nextCursor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, nextCursor);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TemplatePageResponse {\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    nextCursor: ").append(toIndentedString(nextCursor)).append("\n");
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
