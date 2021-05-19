/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import de.ii.ldproxy.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;

import java.util.List;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;
import static de.ii.ldproxy.ogcapi.tiles.domain.PathParameterTileMatrixSetId.TMS_REGEX;

public interface TileSetFormatExtension extends GenericFormatExtension {

    @Override
    default String getPathPattern() {
        return "^(?:/collections/"+COLLECTION_ID_PATTERN+")?/tiles/"+TMS_REGEX+"/?$";
    }

    Object getTileSetEntity(OgcApiDataV2 apiData, ApiRequestContext requestContext,
                            Optional<String> collectionId,
                            TileMatrixSet tileMatrixSet, MinMax zoomLevels, double[] center,
                            List<Link> links);
}
