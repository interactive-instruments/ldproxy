/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import com.google.common.collect.Range;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ChainedTileProvider {
  Logger LOGGER = LoggerFactory.getLogger(ChainedTileProvider.class);

  Map<String, Range<Integer>> getTmsRanges();

  TileResult getTile(TileQuery tile) throws IOException;

  default TileResult get(TileQuery tile) {
    TileResult tileResult = TileResult.notFound();

    if (canProvide(tile)) {
      try {
        tileResult = getTile(tile);
      } catch (IOException e) {
        LOGGER.warn(
            "Failed to retrieve tile {}/{}/{}/{} for layer '{}'. Reason: {}",
            tile.getTileMatrixSet().getId(),
            tile.getLevel(),
            tile.getRow(),
            tile.getCol(),
            tile.getLayer(),
            e.getMessage());
      }
    }

    if (tileResult.isNotFound() && getDelegate().isPresent()) {
      TileResult delegateResult = getDelegate().get().get(tile);

      if (!canProvide(tile)) {
        return delegateResult;
      }

      try {
        return processDelegateResult(tile, delegateResult);
      } catch (IOException e) {
        LOGGER.warn(
            "Failed to retrieve tile {}/{}/{}/{} for layer '{}'. Reason: {}",
            tile.getTileMatrixSet().getId(),
            tile.getLevel(),
            tile.getRow(),
            tile.getCol(),
            tile.getLayer(),
            e.getMessage());
      }

      // delegateResult might be corrupt, recreate
      return getDelegate().get().get(tile);
    }

    return tileResult;
  }

  default Optional<ChainedTileProvider> getDelegate() {
    return Optional.empty();
  }

  default TileResult processDelegateResult(TileQuery tile, TileResult tileResult)
      throws IOException {
    return tileResult;
  }

  default boolean canProvide(TileQuery tile) {
    return getTmsRanges().containsKey(tile.getTileMatrixSet().getId())
        && getTmsRanges().get(tile.getTileMatrixSet().getId()).contains(tile.getLevel());
  }
}
