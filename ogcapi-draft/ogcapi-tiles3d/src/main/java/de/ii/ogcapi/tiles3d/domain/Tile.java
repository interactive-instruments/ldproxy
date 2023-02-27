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
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTile.Builder.class)
public interface Tile {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Tile> FUNNEL =
      (from, into) -> {
        BoundingVolume.FUNNEL.funnel(from.getBoundingVolume(), into);
        from.getGeometricError().ifPresent(into::putFloat);
        into.putString(from.getRefine(), StandardCharsets.UTF_8);
        Content.FUNNEL.funnel(from.getContent(), into);
        ImplicitTiling.FUNNEL.funnel(from.getImplicitTiling(), into);
      };

  BoundingVolume getBoundingVolume();

  Optional<Float> getGeometricError();

  @Value.Default
  default String getRefine() {
    return "REPLACE";
  }

  Content getContent();

  ImplicitTiling getImplicitTiling();
}
