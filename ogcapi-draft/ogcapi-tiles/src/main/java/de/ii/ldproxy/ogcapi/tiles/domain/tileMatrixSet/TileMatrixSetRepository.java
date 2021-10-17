/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet;

import de.ii.ldproxy.ogcapi.domain.ApiExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.tiles.domain.Tile;
import de.ii.xtraplatform.crs.domain.BoundingBox;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/**
 * Access to the cache for tile matrix set files.
 */
public interface TileMatrixSetRepository extends ApiExtension {

    Optional<TileMatrixSet> get(String tileMatrixSetId);
    Map<String,TileMatrixSet> getAll();
}
