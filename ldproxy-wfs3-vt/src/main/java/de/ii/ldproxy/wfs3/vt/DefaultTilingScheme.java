package de.ii.ldproxy.wfs3.vt;

import de.ii.xtraplatform.crs.api.*;

/**
 * This is the most commonly used tiling scheme. It is used by Google Maps and most other web mapping applications.
 * In WMTS it is called "Google Maps Compatible".
 *
 * TODO: add Javadoc
 *
 * @author portele
 */
public class DefaultTilingScheme implements TilingScheme {

    // TODO: make extent, name, zoom levels, etc. configurable; support configuration of additional tiling schemes

    private static final EpsgCrs CRS = new EpsgCrs(3857);
    private static final int TILE_SIZE = 256;
    private static final int TILE_EXTENT = 4096;

    public String getId() {
        return "default";
    };

    public EpsgCrs getCrs() {
        return CRS;
    };

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

    public double getMaxAllowableOffset(int level, int row, int col) {
        return 40075016.6856 / Math.pow(2, level) / TILE_EXTENT;
    }

    public double getMaxAllowableOffset(int level, int row, int col, EpsgCrs crs, CrsTransformation crsTransformation) throws CrsTransformationException {
        BoundingBox bbox = getBoundingBox(level, row, col);
        if (crs!=null && !crs.equals(CRS)) {
            CrsTransformer transformer = crsTransformation.getTransformer(CRS, crs);
            BoundingBox bboxCrs = transformer.transformBoundingBox(bbox);
            bbox = bboxCrs;
        }
        return (bbox.getXmax()-bbox.getXmin())/TILE_EXTENT;
    }

    public int getMaxLevel() {
        return 24;
    };

    public int getMinLevel() {
        return 0;
    };

    public int getTileSize() { return TILE_SIZE; };

    public int getTileExtent() {
        return TILE_EXTENT;
    };
}
