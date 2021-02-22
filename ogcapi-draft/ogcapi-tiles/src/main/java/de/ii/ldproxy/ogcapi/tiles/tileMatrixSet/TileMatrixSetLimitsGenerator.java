/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.tiles.MinMax;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;

import java.util.List;

public interface TileMatrixSetLimitsGenerator {

    List<TileMatrixSetLimits> getCollectionTileMatrixSetLimits(OgcApiDataV2 data, String collectionId,
                                                               TileMatrixSet tileMatrixSet, MinMax tileMatrixRange,
                                                               CrsTransformerFactory crsTransformation);

    List<TileMatrixSetLimits> getTileMatrixSetLimits(OgcApiDataV2 data, TileMatrixSet tileMatrixSet,
                                                     MinMax tileMatrixRange, CrsTransformerFactory crsTransformerFactory);
}
