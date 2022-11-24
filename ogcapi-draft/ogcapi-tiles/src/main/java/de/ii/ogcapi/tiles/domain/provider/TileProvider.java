/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import com.google.common.collect.Range;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimits;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import java.util.Map;
import java.util.Optional;

public interface TileProvider extends PersistentEntity {

  @Override
  TileProviderData getData();

  @Override
  default String getType() {
    return TileProviderData.ENTITY_TYPE;
  }

  TileResult getTile(TileQuery tileQuery);

  // TODO: TileRange?
  default void deleteFromCache(
      String layer, TileMatrixSet tileMatrixSet, TileMatrixSetLimits limits) {}

  // TODO: generation? source? dynamic?
  default boolean supportsGeneration() {
    return this instanceof TileGenerator;
  }

  default TileGenerator generator() {
    if (!supportsGeneration()) {
      throw new UnsupportedOperationException("Generation not supported");
    }
    return (TileGenerator) this;
  }

  default Optional<TileResult> validate(TileQuery tile) {
    if (!getData().getLayers().containsKey(tile.getLayer())) {
      return Optional.of(
          TileResult.error(String.format("Layer '%s' is not supported.", tile.getLayer())));
    }

    Map<String, Range<Integer>> tmsRanges =
        getData().getLayers().get(tile.getLayer()).getTmsRanges();

    if (!tmsRanges.containsKey(tile.getTileMatrixSet().getId())) {
      return Optional.of(
          TileResult.error(
              String.format(
                  "Tile matrix set '%s' is not supported.", tile.getTileMatrixSet().getId())));
    }

    if (!tmsRanges.get(tile.getTileMatrixSet().getId()).contains(tile.getLevel())) {
      return Optional.of(
          TileResult.error("The requested tile is outside the zoom levels for this tile set."));
    }

    BoundingBox boundingBox =
        tile.getGenerationParameters()
            .flatMap(TileGenerationParameters::getClipBoundingBox)
            .orElse(tile.getTileMatrixSet().getBoundingBox());
    TileMatrixSetLimits limits = tile.getTileMatrixSet().getLimits(tile.getLevel(), boundingBox);

    if (!limits.contains(tile.getRow(), tile.getCol())) {
      return Optional.of(
          TileResult.error(
              "The requested tile is outside of the limits for this zoom level and tile set."));
    }

    return Optional.empty();
  }
}
