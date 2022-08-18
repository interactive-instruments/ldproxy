/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableQueryExpression.Builder.class)
public interface QueryExpression {

  enum FilterOperator {
    AND,
    OR
  }

  @SuppressWarnings("UnstableApiUsage")
  Funnel<QueryExpression> FUNNEL =
      (from, into) -> {
        // TODO
      };

  ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new Jdk8Module())
          .registerModule(new GuavaModule())
          .configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  static QueryExpression of(byte[] requestBody) throws IOException {
    return MAPPER.readValue(requestBody, QueryExpression.class);
  }

  static QueryExpression of(InputStream requestBody) throws IOException {
    return MAPPER.readValue(requestBody, QueryExpression.class);
  }

  static void toFile(QueryExpression query, Path path) throws IOException {
    MAPPER.writeValue(path.toFile(), query);
  }

  @JsonIgnore String SCHEMA_REF = "#/components/schemas/QueryExpression";

  Optional<String> getId();

  Optional<String> getTitle();

  Optional<String> getDescription();

  List<SingleQuery> getQueries();

  List<String> getCollections();

  Map<String, Object> getFilter();

  @Value.Default
  default FilterOperator getFilterOperator() {
    return FilterOperator.AND;
  }

  List<String> getSortby();

  List<String> getProperties();

  Optional<String> getCrs();

  Optional<Double> getMaxAllowableOffset();

  Optional<Integer> getLimit();

  @Value.Default
  default int getOffset() {
    return 0;
  }

  @Value.Check
  default void check() {
    Preconditions.checkState(
        (getQueries().isEmpty() && getCollections().size() == 1)
            || (!getQueries().isEmpty() && getCollections().isEmpty()),
        "Either one or more queries must be provided or a single collection. Query: %s. Collections: %s.",
        getQueries(),
        getCollections());
  }
}
