package de.ii.ldproxy.wfs3.vt;

import de.ii.xtraplatform.crs.api.*;

/**
 * This is the most commonly used tiling scheme. It is used by Google Maps and most other web mapping applications.
 * In WMTS it is called "Google Maps Compatible".
 *
 * @author portele
 */
public class DefaultTilingScheme implements TilingScheme {

    /**
     * Web Mercator is the coordinate reference system of the tiling scheme, EPSG code is 3857
     */
    private static final EpsgCrs CRS = new EpsgCrs(3857);

    /**
     * The tile size is fixed to 256x256
     */
    private static final int TILE_SIZE = 256;

    /**
     * Based on experience the tile extent is set to 4096x4096 for a smoother look, since most display can show
     * more details in a "pixel" of the 256x256 tile
     */
    private static final int TILE_EXTENT = 4096;

    /**
     * @return for the default tiling scheme, a fixed id "default" is used
     */
    @Override
    public String getId() {
        return "default";
    };

    @Override
    public EpsgCrs getCrs() {
        return CRS;
    };

    @Override
    public BoundingBox getBoundingBox(int level, int row, int col) {

        // TODO optimize computations
        double x1 = -20037508.3427892;
        double x2 = 20037508.3427892;
        double y1 = -20037508.3427892;
        double y2 = 20037508.3427892;
        double rows = Math.pow(2, level);
        double cols = Math.pow(2, level);
        double tileWidth = (x2 - x1) / rows;
        double tileHeight = (y2 - y1) / cols;
        double minX = x1 + tileWidth * row;
        double maxX = minX + tileWidth;
        double maxY = y2 - tileHeight * col;
        double minY = maxY - tileHeight;
        return new BoundingBox(minX, minY, maxX, maxY, CRS);
    };

    @Override
    public double getMaxAllowableOffset(int level, int row, int col) {
        return 40075016.6856 / Math.pow(2, level) / TILE_EXTENT;
    }

    @Override
    public double getMaxAllowableOffset(int level, int row, int col, EpsgCrs crs, CrsTransformation crsTransformation) throws CrsTransformationException {
        BoundingBox bbox = getBoundingBox(level, row, col);
        if (crs!=null && !crs.equals(CRS)) {
            CrsTransformer transformer = crsTransformation.getTransformer(CRS, crs);
            BoundingBox bboxCrs = transformer.transformBoundingBox(bbox);
            bbox = bboxCrs;
        }
        return (bbox.getXmax()-bbox.getXmin())/TILE_EXTENT;
    }

    @Override
    public int getMaxLevel() {
        return 24;
    };

    @Override
    public int getMinLevel() {
        return 0;
    };

    @Override
    public int getTileSize() { return TILE_SIZE; };

    @Override
    public int getTileExtent() {
        return TILE_EXTENT;
    }

    @Override
    public boolean validateRow(int level, int row) {
        if (level<getMinLevel() || level>getMaxLevel())
            return false;
        return true;
    }

    @Override
    public boolean validateCol(int level, int row) {
        if (level<getMinLevel() || level>getMaxLevel())
            return false;
        return true;
    }
}
