/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimits;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;

public interface TileProvider extends PersistentEntity {

  @Override
  TileProviderData getData();

  @Override
  default String getType() {
    return TileProviderData.ENTITY_TYPE;
  }

  TileResult getTile(TileQuery tileQuery);

  // TODO: TileRange?
  void deleteFromCache(String layer, TileMatrixSet tileMatrixSet, TileMatrixSetLimits limits);

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
}
