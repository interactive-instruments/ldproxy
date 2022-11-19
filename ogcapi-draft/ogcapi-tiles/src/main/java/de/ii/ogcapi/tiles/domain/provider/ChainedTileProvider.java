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
import java.util.Optional;

public interface ChainedTileProvider {

  Range<Integer> getLevels();

  TileResult getTile(TileQuery tileQuery) throws IOException;

  default TileResult get(TileQuery tileQuery) throws IOException {
    TileResult tileResult = canProvide(tileQuery) ? getTile(tileQuery) : TileResult.notFound();

    if (tileResult.isNotFound() && getDelegate().isPresent()) {
      TileResult delegateResult = getDelegate().get().get(tileQuery);

      return canProvide(tileQuery)
          ? processDelegateResult(tileQuery, delegateResult)
          : delegateResult;
    }

    return tileResult;
  }

  default Optional<ChainedTileProvider> getDelegate() {
    return Optional.empty();
  }

  default TileResult processDelegateResult(TileQuery tileQuery, TileResult tileResult)
      throws IOException {
    return tileResult;
  }

  default boolean canProvide(TileQuery tile) {
    return getLevels().contains(tile.getTileLevel());
  }
}
