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
import java.nio.file.Path;
import java.util.Objects;

public interface TileStore extends TileStoreReadOnly {

  void put(TileQuery tile, InputStream content) throws IOException;

  void delete(TileQuery tile) throws IOException;

  void delete(
      String layer, TileMatrixSet tileMatrixSet, TileMatrixSetLimits limits, boolean inverse)
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

  static boolean isInsideBounds(
      Path tilePath,
      String layer,
      String tileMatrixSet,
      TileMatrixSetLimits tmsLimits,
      boolean inverse) {

    if (tilePath.getNameCount() < 5) {
      return false;
    }

    String layerSegment = tilePath.getName(0).toString();

    if (!Objects.equals(layer, layerSegment)) {
      return false;
    }

    String tmsId = tilePath.getName(1).toString();

    if (!Objects.equals(tileMatrixSet, tmsId)) {
      return false;
    }

    String level = tilePath.getName(2).toString();

    if (!Objects.equals(tmsLimits.getTileMatrix(), level)) {
      return false;
    }

    int row = Integer.parseInt(tilePath.getName(3).toString());

    if (row < tmsLimits.getMinTileRow() || row > tmsLimits.getMaxTileRow()) {
      return inverse;
    }

    String file = tilePath.getName(4).toString();

    int col = Integer.parseInt(com.google.common.io.Files.getNameWithoutExtension(file));

    if (col < tmsLimits.getMinTileCol() || col > tmsLimits.getMaxTileCol()) {
      return inverse;
    }

    return !inverse;
  }
}
