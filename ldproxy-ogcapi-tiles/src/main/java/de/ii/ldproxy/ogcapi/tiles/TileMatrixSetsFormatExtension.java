/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;

import javax.ws.rs.core.Response;

public interface TileMatrixSetsFormatExtension extends FormatExtension {

    @Override
    default String getPathPattern() {
        return "^/tileMatrixSets(?:/\\w+)?/?$";
    }

    Response getTileMatrixSetsResponse(TileMatrixSets tileMatrixSets,
                                       OgcApiApi api,
                                       OgcApiRequestContext requestContext);

    Response getTileMatrixSetResponse(TileMatrixSetData tileMatrixSet,
                                      OgcApiApi api,
                                      OgcApiRequestContext requestContext);

}
