package de.ii.ldproxy.wfs3.vt;

import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.EpsgCrs;

/**
 * This interface specifies the characteristics of tiling schemes / tile matrix sets for vector tiles.
 * NOTE: This is preliminary and will change as a result of discussions in the OGC Vector Tile Pilot.
 *
 * TODO: add Javadoc
 *
 * @author portele
 */
public interface TilingScheme {
    String getId();
    EpsgCrs getCrs();
    BoundingBox getBoundingBox(int level, int row, int col);
    double getMaxAllowableOffset(int level, int row, int col);
    double getMaxAllowableOffset(int level, int row, int col, EpsgCrs crs, CrsTransformation crsTransformation) throws CrsTransformationException;
    int getMaxLevel();
    int getMinLevel();
    int getTileSize();
    int getTileExtent();
}
