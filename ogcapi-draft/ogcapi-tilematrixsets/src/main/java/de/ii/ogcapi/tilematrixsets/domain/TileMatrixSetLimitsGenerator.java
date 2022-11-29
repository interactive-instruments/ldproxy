/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tilematrixsets.domain;

import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.util.List;
import java.util.Optional;

public interface TileMatrixSetLimitsGenerator {

  List<TileMatrixSetLimits> getTileMatrixSetLimits(
      OgcApi api,
      TileMatrixSet tileMatrixSet,
      MinMax tileMatrixRange,
      Optional<String> collectionId);

  TileMatrixSetLimits getTileMatrixSetLimits(
      OgcApi api, TileMatrixSet tileMatrixSet, int tileMatrix, Optional<String> collectionId);

  List<TileMatrixSetLimits> getTileMatrixSetLimits(
      BoundingBox boundingBox, TileMatrixSet tileMatrixSet, MinMax tileMatrixRange);
}
