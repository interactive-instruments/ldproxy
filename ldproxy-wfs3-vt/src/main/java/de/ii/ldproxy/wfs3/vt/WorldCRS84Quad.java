/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;

import java.net.URI;
import java.util.Objects;

public class WorldCRS84Quad implements TileMatrixSet {

    private static final EpsgCrs CRS = new EpsgCrs(4326);
    private TileMatrixSetData data;

    /**
     * The bounding box of the tiling scheme
     */
    private static final double BBOX_MIN_X = -180.0;
    private static final double BBOX_MAX_X = 180.0;
    private static final double BBOX_MIN_Y = -90.0;
    private static final double BBOX_MAX_Y = 90.0;
    private static final BoundingBox BBOX = new BoundingBox(BBOX_MIN_X, BBOX_MIN_Y, BBOX_MAX_X, BBOX_MAX_Y, CRS);
    private static final double BBOX_DX = BBOX_MAX_X - BBOX_MIN_X;
    private static final double BBOX_DY = BBOX_MAX_Y - BBOX_MIN_Y;
    private static final double[] TOP_LEFT_CORNER = {BBOX_MIN_X, BBOX_MAX_Y};


    @Override
    public String getId() {
        return "WorldCRS84Quad";
    }

    @Override
    public EpsgCrs getCrs() {
        return CRS;
    }

    @Override
    public BoundingBox getTileBoundingBox(int level, int col, int row) {

        double rows = Math.pow(2, level);
        double cols = Math.pow(2, level) * getInitialWidth();
        double tileWidth = BBOX_DX / cols;
        double tileHeight = BBOX_DY / rows;
        double minX = BBOX_MIN_X + tileWidth * col;
        double maxX = minX + tileWidth;
        double maxY = BBOX_MAX_Y - tileHeight * row;
        double minY = maxY - tileHeight;
        return new BoundingBox(minY, minX, maxY, maxX, CRS);

    }

    @Override
    public double getMaxAllowableOffset(int level, int row, int col) {
        return BBOX_DX / Math.pow(2, level) / getTileExtent();
    }

    @Override
    public double getMaxAllowableOffset(int level, int row, int col, EpsgCrs crs, CrsTransformation crsTransformation)
            throws CrsTransformationException {
        BoundingBox bbox = getTileBoundingBox(level, col, row);
        if (crs != null && !crs.equals(CRS)) {
            CrsTransformer transformer = crsTransformation.getTransformer(CRS, crs);
            bbox = transformer.transformBoundingBox(bbox);
        }
        return (bbox.getXmax() - bbox.getXmin()) / getTileExtent();
    }

    @Override
    public int getMinLevel() {
        return 0;
    }

    @Override
    public int getMaxLevel() {
        return 23;
    }

    @Override
    public int getTileSize() {
        return 256;
    }

    @Override
    public int getTileExtent() {
        return 4096;
    }

    @Override
    public double getInitialScaleDenominator() {
        return 279541132.0143589;
    }

    @Override
    public int getInitialWidth() {
        return 2;
    }

    @Override
    public int getInitialHeight() {
        return 1;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return BBOX;
    }

    @Override
    public TileMatrixSetData getTileMatrixSetData() {

        if (Objects.nonNull(data)) {
            return data;
        }

        data = ImmutableTileMatrixSetData.builder()
                .identifier(getId())
                .title("CRS84 for the World")
                .supportedCRS(CRS.getAsUri())
                .wellKnownScaleSet(URI.create("http://www.opengis.net/def/wkss/OGC/1.0/GoogleCRS84Quad"))
                .boundingBox(ImmutableTileMatrixSetBoundingBox.builder()
                        .lowerCorner(BBOX_MIN_X, BBOX_MIN_Y)
                        .upperCorner(BBOX_MAX_X, BBOX_MAX_Y)
                        .build())
                .tileMatrix(generateTileMatrices(getMaxLevel(), getInitialScaleDenominator(), TOP_LEFT_CORNER))
                .build();

        return data;
    }
}
