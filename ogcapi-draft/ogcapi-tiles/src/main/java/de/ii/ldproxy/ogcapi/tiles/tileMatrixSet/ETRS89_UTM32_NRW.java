/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.tiles.TilesConfiguration;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.net.URI;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class ETRS89_UTM32_NRW extends AbstractTileMatrixSet implements TileMatrixSet {

    private static final EpsgCrs CRS = EpsgCrs.of(25832);

    /**
     * The bounding box of the tiling scheme
     */
    private static final double DIFF = 1252344.271424;
    private static final double BBOX_MIN_X = -46133.17;
    private static final double BBOX_MAX_Y = 6301219.54;
    private static final double BBOX_MAX_X = BBOX_MIN_X + DIFF;
    private static final double BBOX_MIN_Y = BBOX_MAX_Y - DIFF;
    private static final BoundingBox BBOX = BoundingBox.of(BBOX_MIN_X, BBOX_MIN_Y, BBOX_MAX_X, BBOX_MAX_Y, CRS);

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public String getId() {
        return "ETRS89_UTM32_NRW";
    }

    @Override
    public EpsgCrs getCrs() {
        return CRS;
    }

    @Override
    public Optional<String> getTitle() { return Optional.of("ETRS89 UTM32 in North-Rhine Westphalia"); }

    @Override
    public Optional<URI> getWellKnownScaleSet() { return Optional.empty(); }

    @Override
    public int getMaxLevel() { return 14; }

    @Override
    public double getInitialScaleDenominator() { return 17471320.750897426; }

    @Override
    public int getInitialWidth() { return 1; }

    @Override
    public int getInitialHeight() { return 1; }

    @Override
    public BoundingBox getBoundingBox() { return BBOX; }

    @Override
    public TileMatrix getTileMatrix(int level) {
        double initScaleDenominator = getInitialScaleDenominator();
        int width;
        switch (level) {
            case 9:
                width = 511;
                break;
            case 10:
                width = 1021;
                break;
            case 11:
                width = 2042;
                break;
            case 12:
                width = 4084;
                break;
            case 13:
                width = 8167;
                break;
            case 14:
                width = 16334;
                break;
            default:
                 width = getCols(level);
                 break;
        }
        TileMatrix tileMatrix = ImmutableTileMatrix.builder()
                .tileLevel(level)
                .tileWidth(getTileSize())
                .tileHeight(getTileSize())
                .matrixWidth(width)
                .matrixHeight(getRows(level))
                .scaleDenominator(initScaleDenominator / Math.pow(2, level))
                .topLeftCorner(new double[]{BBOX_MIN_X, BBOX_MAX_Y})
                .build();
        return tileMatrix;
    }

}
