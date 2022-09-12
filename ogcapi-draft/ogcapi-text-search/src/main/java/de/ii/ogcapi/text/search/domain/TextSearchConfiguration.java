/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.text.search.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableTextSearchConfiguration.Builder.class)
public interface TextSearchConfiguration extends ExtensionConfiguration {

  List<String> getProperties();

  Optional<FeaturesCollectionQueryables> getQueryables();

  default List<String> getQ() {
    if (getQueryables().isPresent()) {
      return Stream.concat(
              getQueryables().get().getQ().stream(), getQueryables().get().getOther().stream())
          .collect(Collectors.toList());
    }

    return ImmutableList.of();
  }

  // Todo add derived method that first checks for getQueryables().getQ() in
  // FeaturesCoreConfiguration to support existing configurations
}
