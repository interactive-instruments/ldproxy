/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet;

import de.ii.xtraplatform.crs.domain.BoundingBox;

import java.util.Objects;

public abstract class AbstractTileMatrixSet implements TileMatrixSet {

    private TileMatrixSetData data;

    @Override
    public TileMatrixSetData getTileMatrixSetData() {

        if (Objects.nonNull(data)) {
            return data;
        }

        BoundingBox bbox = getBoundingBox();
        data = ImmutableTileMatrixSetData.builder()
                .identifier(getId())
                .title(getTitle())
                .supportedCRS(getCrs().toUriString())
                .wellKnownScaleSet(getWellKnownScaleSet())
                .boundingBox(ImmutableTileMatrixSetBoundingBox.builder()
                        .lowerCorner(bbox.getXmin(), bbox.getYmin())
                        .upperCorner(bbox.getXmax(), bbox.getYmax())
                        .build())
                .tileMatrix(getTileMatrices(getMinLevel(), getMaxLevel()))
                .build();

        return data;

    }

}
