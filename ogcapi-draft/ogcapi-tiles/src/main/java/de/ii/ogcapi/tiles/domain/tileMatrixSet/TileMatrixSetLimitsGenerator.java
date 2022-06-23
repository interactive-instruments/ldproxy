/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.tileMatrixSet;

import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.tiles.domain.MinMax;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.util.List;

public interface TileMatrixSetLimitsGenerator {

  List<TileMatrixSetLimits> getCollectionTileMatrixSetLimits(
      OgcApi api, String collectionId, TileMatrixSet tileMatrixSet, MinMax tileMatrixRange);

  List<TileMatrixSetLimits> getTileMatrixSetLimits(
      OgcApi api, TileMatrixSet tileMatrixSet, MinMax tileMatrixRange);

  List<TileMatrixSetLimits> getTileMatrixSetLimits(
      BoundingBox boundingBox, TileMatrixSet tileMatrixSet, MinMax tileMatrixRange);
}
