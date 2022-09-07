/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableSingleQuery.Builder.class)
public interface SingleQuery {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<SingleQuery> FUNNEL =
      (from, into) -> {
        from.getCollections().forEach(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getFilter()
            .forEach(
                (name, filter) -> {
                  into.putString(name, StandardCharsets.UTF_8);
                });
        from.getSortby().forEach(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getProperties().forEach(s -> into.putString(s, StandardCharsets.UTF_8));
      };

  @JsonIgnore String SCHEMA_REF = "#/components/schemas/Query";

  List<String> getCollections(); // String or Parameter

  Map<String, Object> getFilter();

  List<String> getSortby(); // String or Parameter

  List<String> getProperties(); // String or Parameter
}
