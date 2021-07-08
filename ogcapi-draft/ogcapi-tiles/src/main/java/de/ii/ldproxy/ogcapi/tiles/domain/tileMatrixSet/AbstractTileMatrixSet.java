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
                                         .id(getId())
                                         .title(getTitle())
                                         .description(getDescription())
                                         .keywords(getKeywords())
                                         .orderedAxes(getOrderedAxes())
                                         .crs(getCrs().toUriString())
                                         .uri(getURI())
                                         .wellKnownScaleSet(getWellKnownScaleSet())
                                         .boundingBox(ImmutableTilesBoundingBox.builder()
                                                                               .crsEpsg(getCrs())
                                                                               .lowerLeft(getBigDecimal(bbox.getXmin()),
                                                                                          getBigDecimal(bbox.getYmin()))
                                                                               .upperRight(getBigDecimal(bbox.getXmax()),
                                                                                           getBigDecimal(bbox.getYmax()))
                                                                               .build())
                                         .tileMatrices(getTileMatrices(getMinLevel(), getMaxLevel()))
                                         .build();

        return data;

    }

}
