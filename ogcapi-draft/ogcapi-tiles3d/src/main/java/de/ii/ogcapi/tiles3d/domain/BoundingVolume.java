/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableBoundingVolume.Builder.class)
public interface BoundingVolume {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<BoundingVolume> FUNNEL =
      (from, into) -> {
        from.getRegion().forEach(into::putDouble);
      };

  List<Double> getRegion();
}
