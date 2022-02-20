/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.tileMatrixSet;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.OgcApi;

@AutoMultiBind
public interface TileMatrixSetsFormatExtension extends GenericFormatExtension {

    @Override
    default String getPathPattern() {
        return "^/tileMatrixSets(?:/\\w+)?/?$";
    }

    Object getTileMatrixSetsEntity(TileMatrixSets tileMatrixSets,
                                   OgcApi api,
                                   ApiRequestContext requestContext);

    Object getTileMatrixSetEntity(TileMatrixSetData tileMatrixSet,
                                  OgcApi api,
                                  ApiRequestContext requestContext);

}
