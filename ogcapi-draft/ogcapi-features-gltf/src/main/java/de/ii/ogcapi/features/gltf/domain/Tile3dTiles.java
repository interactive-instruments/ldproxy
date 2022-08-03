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
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTile3dTiles.Builder.class)
public interface Tile3dTiles {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Tile3dTiles> FUNNEL =
      (from, into) -> {
        // TODO
      };

  BoundingVolume3dTiles getBoundingVolume();

  Optional<Float> getGeometricError();

  @Value.Default
  default String getRefine() {
    return "REPLACE";
  }

  Content3dTiles getContent();

  ImplicitTiling3dTiles getImplicitTiling();
}
