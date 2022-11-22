/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface LayerOptionsFeatures extends LayerOptionsCommon {
  String COMBINE_ALL = "*";

  Optional<String> getFeatureProvider();

  Optional<String> getFeatureType();

  List<String> getCombine();

  Map<String, List<LevelFilter>> getFilters();

  @JsonIgnore
  @Value.Derived
  default boolean isCombined() {
    return !getCombine().isEmpty();
  }

  // TODO: check
}
