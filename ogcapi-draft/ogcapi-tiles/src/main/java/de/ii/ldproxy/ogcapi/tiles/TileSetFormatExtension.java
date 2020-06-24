/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.PathParameterTileMatrixSetId.TMS_REGEX;

public interface TileSetFormatExtension extends FormatExtension {

    @Override
    default String getPathPattern() {
        return "^(?:/collections/[\\w\\-]+)?/tiles/"+TMS_REGEX+"/?$";
    }

    Object getTileSetEntity(OgcApiApiDataV2 apiData, OgcApiRequestContext requestContext,
                            Optional<String> collectionId,
                            TileMatrixSet tileMatrixSet, MinMax zoomLevels, double[] center,
                            List<OgcApiLink> links);
}
