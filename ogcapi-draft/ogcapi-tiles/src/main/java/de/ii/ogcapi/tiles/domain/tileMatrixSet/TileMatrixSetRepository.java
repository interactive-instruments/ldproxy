/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.tileMatrixSet;

import de.ii.ldproxy.ogcapi.foundation.domain.ApiExtension;

import java.util.Map;
import java.util.Optional;

/**
 * Access to the cache for tile matrix set files.
 */
public interface TileMatrixSetRepository extends ApiExtension {

    Optional<TileMatrixSet> get(String tileMatrixSetId);
    Map<String,TileMatrixSet> getAll();
}
