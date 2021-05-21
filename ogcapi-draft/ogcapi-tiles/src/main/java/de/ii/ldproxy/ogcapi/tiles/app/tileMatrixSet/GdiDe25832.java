/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.tileMatrixSet;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.AbstractTileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.ImmutableTileMatrix;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrix;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class GdiDe25832 extends AbstractTileMatrixSet implements TileMatrixSet {

    private static final EpsgCrs CRS = EpsgCrs.of(25832);

    /**
     * The bounding box of the tiling scheme
     */
    private static final double DIFF = 1433600.00;
    private static final double BBOX_MIN_X = -46133.17;
    private static final double BBOX_MAX_Y = 6301219.54;
    private static final double BBOX_MAX_X = BBOX_MIN_X + DIFF;
    private static final double BBOX_MIN_Y = BBOX_MAX_Y - DIFF;
    private static final BoundingBox BBOX = BoundingBox.of(BBOX_MIN_X, BBOX_MIN_Y, BBOX_MAX_X, BBOX_MAX_Y, CRS);
    private static final List<Integer> WIDTH_HEIGHT_PER_LEVEL = new ImmutableList.Builder<Integer>()
            .add(2, 4, 8, 20, 40, 80, 200, 400, 800, 2000, 4000, 8000, 20000, 40000, 80000, 200000)
            .build();

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public String getId() {
        return "gdi_de_25832";
    }

    @Override
    public EpsgCrs getCrs() {
        return CRS;
    }

    @Override
    public Optional<String> getTitle() { return Optional.of("GDI-DE tiling scheme using ETRS89/UTM32N covering Germany"); }

    @Override
    public Optional<URI> getWellKnownScaleSet() { return Optional.empty(); }

    @Override
    public int getMaxLevel() { return 15; }

    @Override
    public double getInitialScaleDenominator() { return 10000000.0; }

    @Override
    public int getInitialWidth() { return 2; }

    @Override
    public int getInitialHeight() { return 2; }

    @Override
    public BoundingBox getBoundingBox() { return BBOX; }

    @Override
    public int getCols(int level) {
        return WIDTH_HEIGHT_PER_LEVEL.get(level);
    }

    @Override
    public int getRows(int level) {
        return WIDTH_HEIGHT_PER_LEVEL.get(level);
    }

    @Override
    public TileMatrix getTileMatrix(int level) {
        double initScaleDenominator = getInitialScaleDenominator();
        int initialWidth = getInitialWidth();
        int width = getCols(level);
        int height = getRows(level);
        return ImmutableTileMatrix.builder()
                                  .tileLevel(level)
                                  .tileWidth(getTileSize())
                                  .tileHeight(getTileSize())
                                  .matrixWidth(width)
                                  .matrixHeight(height)
                                  .scaleDenominator(initScaleDenominator * initialWidth / width)
                                  .topLeftCorner(new double[]{BBOX_MIN_X, BBOX_MAX_Y})
                                  .build();
    }
}
