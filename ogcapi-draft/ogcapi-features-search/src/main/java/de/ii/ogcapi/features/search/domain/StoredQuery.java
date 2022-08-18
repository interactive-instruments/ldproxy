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
import de.ii.ogcapi.foundation.domain.Link;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableStoredQuery.Builder.class)
public interface StoredQuery {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<StoredQuery> FUNNEL =
      (from, into) -> {
        // TODO
      };

  @JsonIgnore String SCHEMA_REF = "#/components/schemas/StoredQuery";

  String getId();

  Optional<String> getTitle();

  Optional<String> getDescription();

  List<Link> getLinks();
}
