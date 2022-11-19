/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Range;
import de.ii.ogcapi.tilematrixsets.domain.MinMax;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.stream.Collectors;
import org.immutables.value.Value;

public interface WithLevels {
  Map<String, MinMax> getLevels();

  @JsonIgnore
  @Value.Derived
  default Map<String, Range<Integer>> getTmsRanges() {
    return getLevels().entrySet().stream()
        .map(
            entry ->
                new SimpleImmutableEntry<>(
                    entry.getKey(),
                    Range.closed(entry.getValue().getMin(), entry.getValue().getMax())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
