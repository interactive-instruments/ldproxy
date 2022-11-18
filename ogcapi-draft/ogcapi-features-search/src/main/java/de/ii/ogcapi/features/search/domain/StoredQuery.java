/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Funnel;
import de.ii.ogcapi.features.core.domain.CollectionProperty;
import de.ii.ogcapi.foundation.domain.PageRepresentationWithId;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableStoredQuery.Builder.class)
public abstract class StoredQuery extends PageRepresentationWithId {

  @SuppressWarnings("UnstableApiUsage")
  public static Funnel<StoredQuery> FUNNEL =
      (from, into) -> {
        PageRepresentationWithId.FUNNEL.funnel(from, into);
        from.getParameters()
            .forEach(
                (name, schema) -> {
                  into.putString(name, StandardCharsets.UTF_8);
                  into.putString(schema.toString(), StandardCharsets.UTF_8);
                });
      };

  @JsonIgnore String SCHEMA_REF = "#/components/schemas/StoredQuery";

  public abstract Map<String, JsonNode> getParameters();

  @JsonIgnore
  @Value.Derived
  public List<CollectionProperty> getParameterList() {
    ImmutableList.Builder<CollectionProperty> builder = ImmutableList.builder();
    getParameters().entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEachOrdered(
            entry ->
                builder.add(CollectionProperty.of(entry.getKey(), (ObjectNode) entry.getValue())));
    return builder.build();
  }

  @JsonIgnore
  @Value.Derived
  public boolean hasParameters() {
    return !getParameters().isEmpty();
  }
}
