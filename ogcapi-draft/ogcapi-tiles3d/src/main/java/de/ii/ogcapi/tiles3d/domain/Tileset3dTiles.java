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
import de.ii.ogcapi.features.gltf.domain.AssetMetadata;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileset3dTiles.Builder.class)
public interface Tileset3dTiles {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Tileset3dTiles> FUNNEL =
      (from, into) -> {
        AssetMetadata.FUNNEL.funnel(from.getAsset(), into);
        from.getGeometricError().ifPresent(into::putFloat);
        Tile3dTiles.FUNNEL.funnel(from.getRoot(), into);
      };

  AssetMetadata getAsset();

  Optional<Float> getGeometricError();

  Tile3dTiles getRoot();
}
