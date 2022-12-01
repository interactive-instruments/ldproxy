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
import java.io.IOException;
import java.io.InputStream;

public interface TileStore extends TileStoreReadOnly {

  void put(TileQuery tile, InputStream content) throws IOException;

  void delete(TileQuery tile) throws IOException;

  void delete(String layer, TileMatrixSet tileMatrixSet, TileMatrixSetLimits limits)
      throws IOException;

  interface Staging {

    boolean inProgress();

    boolean init() throws IOException;

    void promote() throws IOException;

    void cleanup() throws IOException;

    void abort() throws IOException;
  }

  default boolean canStage() {
    return this instanceof Staging;
  }

  default Staging staging() {
    if (!canStage()) {
      throw new UnsupportedOperationException("Staging not supported");
    }
    return (Staging) this;
  }
}
