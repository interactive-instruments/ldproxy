/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import java.util.Optional;
import org.immutables.value.Value;

public interface TileCoordinates {
  TileMatrixSet getTileMatrixSet();

  int getTileLevel();

  int getTileRow();

  int getTileCol();

  @Value.Lazy
  default BoundingBox getBoundingBox() {
    return getTileMatrixSet().getTileBoundingBox(getTileLevel(), getTileCol(), getTileRow());
  }

  default BoundingBox getBoundingBox(EpsgCrs crs, CrsTransformerFactory crsTransformerFactory)
      throws CrsTransformationException {
    BoundingBox bboxTileMatrixSetCrs = getBoundingBox();
    Optional<CrsTransformer> transformer =
        crsTransformerFactory.getTransformer(getTileMatrixSet().getCrs(), crs, true);

    if (transformer.isEmpty()) {
      return bboxTileMatrixSetCrs;
    }

    return transformer.get().transformBoundingBox(bboxTileMatrixSetCrs);
  }
}
