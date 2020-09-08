/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import org.immutables.value.Value;
import org.locationtech.jts.geom.util.AffineTransformation;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * This class represents a vector tile
 */
@Value.Immutable
public abstract class Tile {

    /**
     *
     * @return the tiling scheme / tile matrix set of the tile
     */
    public abstract TileMatrixSet getTileMatrixSet();

    /**
     *
     * @return the zoom level / tile matrix of the tile
     */
    public abstract int getTileLevel();

    /**
     *
     * @return the row of the tile
     */
    public abstract int getTileRow();

    /**
     *
     * @return the column of the tile
     */
    public abstract int getTileCol();

    /**
     *
     * @return the ids of the collections included as layers in the tile, empty set means all collections
     */
    public abstract List<String> getCollectionIds();

    /**
     *
     * @return the API that produces the tile
     */
    public abstract OgcApi getApi();

    /**
     *
     * @return the feature provider for the features in the tile
     */
    public abstract FeatureProvider2 getFeatureProvider();

    /**
     *
     * @return the output format to generate the tile
     */
    public abstract TileFormatExtension getOutputFormat();

    /**
     *
     * @return {@code true}, if the tile is not stored in the persistent cache, it is only stored for a few minutes
     *         to support generating multi-layer tiles from multiple single-layer tiles
     */
    public abstract boolean getTemporary();

    @Nullable
    @Value.Derived
    @Value.Auxiliary
    public String getCollectionId() {
        return getCollectionIds().size()==1 ?
                getCollectionIds().get(0) :
                null;
    }

    @Value.Derived
    @Value.Auxiliary
    public Path getRelativePath() {
        Path tilePath;
        String extension = getOutputFormat().getExtension();
        if (this.getTemporary()) {
            tilePath = Paths.get(String.format("%s.%s", UUID.randomUUID()
                                                            .toString(), extension));
        } else {
            tilePath = Paths.get(String.valueOf(getTileLevel()), String.valueOf(getTileRow()), String.format("%d.%s", getTileCol(), extension));
        }

        return tilePath;
    }

    /**
     * Verify that the zoom level is in the valid range for the tile matrix set.
     * Verify that the row number is in the valid range for the tile matrix.
     * Verify that the column number is in the valid range for the tile matrix.
     * Otherwise throw an IllegalStateException exception.
     */
    @Value.Derived
    @Value.Auxiliary
    public void check() {
        if (getTileLevel() > getTileMatrixSet().getMaxLevel() || getTileLevel() < getTileMatrixSet().getMinLevel())
            throw new IllegalStateException(MessageFormat.format("Tile is not valid in tiling scheme {0}, zoom level {1} is outside of the range {2}..{3}.", getTileMatrixSet().getId(), getTileLevel(), getTileLevel() < getTileMatrixSet().getMinLevel(), getTileMatrixSet().getMaxLevel()));
        if (!getTileMatrixSet().validateRow(getTileLevel(), getTileRow()))
            throw new IllegalStateException(MessageFormat.format("Tile is not valid in tiling scheme {0}, row {1} is outside of the range for zoom level {2}.", getTileMatrixSet().getId(), getTileRow(), getTileLevel()));
        if (!getTileMatrixSet().validateCol(getTileLevel(), getTileCol()))
            throw new IllegalStateException(MessageFormat.format("Tile is not valid in tiling scheme {0}, column {1} is outside of the range for zoom level {2}.", getTileMatrixSet().getId(), getTileCol(), getTileLevel()));
    }

    /**
     * Creates an affine transformation for converting geometries in native CRS of the tiling scheme to tile coordinates.
     *
     * @return the transform
     */
    @Value.Derived
    @Value.Auxiliary
    public AffineTransformation createTransformNativeToTile() {

        BoundingBox bbox = getBoundingBox();

        double xMin = bbox.getXmin();
        double xMax = bbox.getXmax();
        double yMin = bbox.getYmin();
        double yMax = bbox.getYmax();

        double tileSize = getTileMatrixSet().getTileSize();

        double xScale = tileSize / (xMax - xMin);
        double yScale = tileSize / (yMax - yMin);

        double xOffset = -xMin * xScale;
        double yOffset = yMin * yScale + tileSize;

        return new AffineTransformation(xScale, 0.0d, xOffset, 0.0d, -yScale, yOffset);
    }

    /**
     * Creates an affine transformation for converting geometries in lon/lat to tile coordinates.
     *
     * @param crsTransformerFactory the coordinate reference system transformation object to transform coordinates
     * @return the transform
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     */
    @Value.Derived
    @Value.Auxiliary
    public AffineTransformation createTransformLonLatToTile(
            CrsTransformerFactory crsTransformerFactory) throws CrsTransformationException {

        BoundingBox bbox = getBoundingBox(OgcCrs.CRS84, crsTransformerFactory);

        double lonMin = bbox.getXmin();
        double lonMax = bbox.getXmax();
        double latMin = bbox.getYmin();
        double latMax = bbox.getYmax();

        double tileSize = getTileMatrixSet().getTileSize();
        double xScale = tileSize / (lonMax - lonMin);
        double yScale = tileSize / (latMax - latMin);

        double xOffset = -lonMin * xScale;
        double yOffset = latMin * yScale + tileSize;

        return new AffineTransformation(xScale, 0.0d, xOffset, 0.0d, -yScale, yOffset);
    }

    /**
     * @return the bounding box of the tile in the native CRS of the tile
     */
    @Value.Derived
    @Value.Auxiliary
    public BoundingBox getBoundingBox() {
        return getTileMatrixSet().getTileBoundingBox(getTileLevel(), getTileCol(), getTileRow());
    }

    /**
     * @param crs               the target coordinate references system
     * @param crsTransformerFactory the coordinate reference system transformation object to transform coordinates
     * @return the bounding box of the tile matrix set in the form of the target crs
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     */
    @Value.Derived
    @Value.Auxiliary
    public BoundingBox getBoundingBox(EpsgCrs crs,
                                       CrsTransformerFactory crsTransformerFactory) throws CrsTransformationException {
        BoundingBox bboxTileMatrixSetCrs = getBoundingBox();
        Optional<CrsTransformer> transformer = crsTransformerFactory.getTransformer(getTileMatrixSet().getCrs(), crs);

        if (!transformer.isPresent()) {
            return bboxTileMatrixSetCrs;
        }

        return transformer.get().transformBoundingBox(bboxTileMatrixSetCrs);
    }
}

