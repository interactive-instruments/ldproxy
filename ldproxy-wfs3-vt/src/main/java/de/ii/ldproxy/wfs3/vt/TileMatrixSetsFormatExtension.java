/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;

import javax.ws.rs.core.Response;
import java.util.Map;

public interface TileMatrixSetsFormatExtension extends FormatExtension {

    @Override
    default String getPathPattern() {
        return "^/tileMatrixSets(?:/\\w+)?/?$";
    }

    Response getTileMatrixSetsResponse(Map<String, Object> tileMatrixSets,
                                       OgcApiDataset api,
                                       OgcApiRequestContext requestContext);

    Response getTileMatrixSetResponse(Map<String, Object> tileMatrixSet,
                                      OgcApiDataset api,
                                      OgcApiRequestContext requestContext);

}
