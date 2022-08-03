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
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableImplicitTiling3dTiles.Builder.class)
public interface ImplicitTiling3dTiles {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<ImplicitTiling3dTiles> FUNNEL =
      (from, into) -> {
        // TODO
      };

  @Value.Default
  default String getSubdivisionScheme() {
    return "QUADTREE";
  }

  int getAvailableLevels();

  int getSubtreeLevels();

  Content3dTiles getSubtrees();
}
