/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableBoundingVolume3dTiles.Builder.class)
public interface BoundingVolume3dTiles {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<BoundingVolume3dTiles> FUNNEL =
      (from, into) -> {
        // TODO
      };

  List<Double> getRegion();
}
