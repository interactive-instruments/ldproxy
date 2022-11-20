/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Range;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableLevelFilter.Builder.class)
public interface LevelFilter {
  int getMin();

  int getMax();

  String getFilter();

  @Value.Derived
  default Range<Integer> getRange() {
    return Range.closed(getMin(), getMax());
  }

  default boolean matches(int level) {
    return getRange().contains(level);
  }
}
