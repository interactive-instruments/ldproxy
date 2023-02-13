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
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileset.Builder.class)
public interface Tileset {

  String SCHEMA_REF = "#/components/schemas/Tileset3dTiles";

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Tileset> FUNNEL =
      (from, into) -> {
        AssetMetadata.FUNNEL.funnel(from.getAsset(), into);
        from.getGeometricError().ifPresent(into::putFloat);
        Tile.FUNNEL.funnel(from.getRoot(), into);
      };

  AssetMetadata getAsset();

  Optional<Float> getGeometricError();

  Tile getRoot();

  Optional<String> getSchemaUri();

  List<String> getExtensionsUsed();

  List<String> getExtensionsRequired();
}
