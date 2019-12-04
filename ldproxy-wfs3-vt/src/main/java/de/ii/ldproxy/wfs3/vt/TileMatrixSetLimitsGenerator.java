/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.wfs3.vt.TileCollection.TileMatrixSetLimits;
import de.ii.ldproxy.wfs3.vt.TilesConfiguration.MinMax;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;

import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class is responsible for generating a list of tileMatrixSetLimits in json responses for /tiles and /collections/{collectionsId}/tiles requests.
 */
public class TileMatrixSetLimitsGenerator {

    /**
     * Return a list of tileMatrixSetLimits for a single collection.
     * @param data service dataset
     * @param collectionId name of the collection
     * @param tileMatrixSetId identifier of a specific tile matrix set
     * @param crsTransformation crs transfromation
     * @return list of TileMatrixSetLimits
     */
    public List<TileMatrixSetLimits> generateCollectionTileMatrixSetLimits(OgcApiDatasetData data, String collectionId,
                                                                           String tileMatrixSetId,
                                                                           CrsTransformation crsTransformation) {

        List<FeatureTypeConfigurationOgcApi> collectionData = data.getFeatureTypes()
                .values()
                .stream()
                .filter(featureType -> collectionId.equals(featureType.getId()))
                .collect(Collectors.toList());
        BoundingBox bbox = collectionData.get(0).getExtent().getSpatial();
        CrsTransformer transformer = crsTransformation.getTransformer(bbox.getEpsgCrs(), new EpsgCrs(3857));
        CoordinateTuple lowerLeftCorner = transformer.transform(bbox.getYmin(), bbox.getXmin());
        CoordinateTuple upperRightCorner = transformer.transform(bbox.getYmax(), bbox.getXmax());

        MinMax tileMatrixRange = getTileMatrixRange(tileMatrixSetId, data);
        return generateLimitsList(tileMatrixRange, lowerLeftCorner, upperRightCorner);
    }

    /**
     * Return a list of tileMatrixSetLimits for all collections in the dataset.
     * @param data service dataset
     * @param tileMatrixSetId identifier of a specific tile matrix set
     * @param crsTransformation crs transfromation
     * @return list of TileMatrixSetLimits
     */
    public List<TileMatrixSetLimits> getTileMatrixSetLimits(OgcApiDatasetData data, String tileMatrixSetId,
                                                            CrsTransformation crsTransformation) {

        double[] spatialExtent = data.getSpatialExtent();
        BoundingBox bbox = new BoundingBox(spatialExtent[1], spatialExtent[0], spatialExtent[3], spatialExtent[2], new EpsgCrs(4326));
        CrsTransformer transformer = crsTransformation.getTransformer(bbox.getEpsgCrs(), new EpsgCrs(3857));
        CoordinateTuple lowerCorner = transformer.transform(bbox.getXmin(), bbox.getYmin());
        CoordinateTuple upperCorner = transformer.transform(bbox.getXmax(), bbox.getYmax());

        MinMax tileMatrixRange = getTileMatrixRange(tileMatrixSetId, data);
        return generateLimitsList(tileMatrixRange, lowerCorner, upperCorner);
    }

    /**
     * Return minimum and maximum tileMatrix values specified in the configuration.
     * If tileMatrix range is absent in the configuration, returns the whole extent supported by tileMatrixSetId.
     * @param tileMatrixSetId identifier of a specific tile matrix set
     * @param data service dataset
     * @return range of tile matrix values
     */
    private MinMax getTileMatrixRange(String tileMatrixSetId, OgcApiDatasetData data) {

        Optional<MinMax> minMaxFromConfig = data.getCapabilities()
                .stream()
                .filter(extensionConfiguration -> extensionConfiguration instanceof TilesConfiguration)
                .map(tilesConfiguration -> ((TilesConfiguration) tilesConfiguration).getZoomLevels())
                .filter(Objects::nonNull)
                .filter(minMaxMap -> minMaxMap.containsKey(tileMatrixSetId))
                .map(minMaxMap -> minMaxMap.get(tileMatrixSetId))
                .findFirst();

        if (minMaxFromConfig.isPresent()) {
            return minMaxFromConfig.get();
        }

        TileMatrixSet tileMatrixSet = getTileMatrixSet(tileMatrixSetId);
        return new ImmutableMinMax.Builder()
                .min(tileMatrixSet.getMinLevel())
                .max(tileMatrixSet.getMaxLevel())
                .build();
    }

    /**
     * Return tileMatrixSet class for the given tileMatrixSetId
     * @param tileMatrixSetId identifier of a specific tile matrix set
     * @return TileMatrixSet
     */
    private TileMatrixSet getTileMatrixSet(String tileMatrixSetId) {
        if (tileMatrixSetId.equalsIgnoreCase("WebMercatorQuad") || tileMatrixSetId.equalsIgnoreCase("default")) {
            return new WebMercatorQuad();
        } else {
            throw new NotFoundException();
        }
    }

    /**
     * Construct a list of TileMatrixSetLimits for the given bounding box and tileMatrix range
     * @param tileMatrixRange range of tileMatrix values
     * @param lowerCorner coordinates of the lower left corner point of the bounding box
     * @param upperCorner coordinates of the upper right corner point of the bounding box
     * @return list of TileMatrixSetLimits
     */
    private List<TileMatrixSetLimits> generateLimitsList(MinMax tileMatrixRange, CoordinateTuple lowerCorner,
                                                         CoordinateTuple upperCorner) {
        ImmutableList.Builder<TileMatrixSetLimits> limits = new ImmutableList.Builder<>();
        for (int tileMatrix = tileMatrixRange.getMin(); tileMatrix <= tileMatrixRange.getMax(); tileMatrix++) {
            List<Integer> upperLeftCornerTile = MultitilesGenerator.pointToTile(lowerCorner.getX(), upperCorner.getY(), tileMatrix);
            List<Integer> lowerRightCornerTile = MultitilesGenerator.pointToTile(upperCorner.getX(), lowerCorner.getY(), tileMatrix);
            limits.add(ImmutableTileMatrixSetLimits.builder()
                    .minTileRow(upperLeftCornerTile.get(0))
                    .maxTileRow(lowerRightCornerTile.get(0))
                    .minTileCol(upperLeftCornerTile.get(1))
                    .maxTileCol(lowerRightCornerTile.get(1))
                    .tileMatrix(Integer.toString(tileMatrix))
                    .build());
        }
        return limits.build();
    }

}
