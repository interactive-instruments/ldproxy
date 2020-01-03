/**
 * Copyright 2020 interactive instruments GmbH
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
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;

import java.util.List;
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
                                                                           String tileMatrixSetId, MinMax tileMatrixRange,
                                                                           CrsTransformation crsTransformation) {

        TileMatrixSet tileMatrixSet = TileMatrixSetCache.getTileMatrixSet(tileMatrixSetId);

        List<FeatureTypeConfigurationOgcApi> collectionData = data.getFeatureTypes()
                .values()
                .stream()
                .filter(featureType -> collectionId.equals(featureType.getId()))
                .collect(Collectors.toList());
        BoundingBox bbox = collectionData.get(0).getExtent().getSpatial();

        if (!bbox.getEpsgCrs().equals(tileMatrixSet.getCrs())) {
            CrsTransformer transformer = crsTransformation.getTransformer(bbox.getEpsgCrs(), tileMatrixSet.getCrs());
            try {
                bbox = transformer.transformBoundingBox(new BoundingBox(bbox.getYmin(), bbox.getXmin(), bbox.getYmax(), bbox.getXmax(), bbox.getEpsgCrs()));
            } catch (CrsTransformationException e) {
                e.printStackTrace();
            }
        }
        return generateLimitsList(tileMatrixRange, bbox, tileMatrixSet);
    }

    /**
     * Return a list of tileMatrixSetLimits for all collections in the dataset.
     * @param data service dataset
     * @param tileMatrixSetId identifier of a specific tile matrix set
     * @param crsTransformation crs transfromation
     * @return list of TileMatrixSetLimits
     */
    public List<TileMatrixSetLimits> getTileMatrixSetLimits(OgcApiDatasetData data, String tileMatrixSetId,
                                                            MinMax tileMatrixRange, CrsTransformation crsTransformation) {

        TileMatrixSet tileMatrixSet = TileMatrixSetCache.getTileMatrixSet(tileMatrixSetId);

        double[] spatialExtent = data.getSpatialExtent();
        BoundingBox bbox = new BoundingBox(spatialExtent[0], spatialExtent[1], spatialExtent[2], spatialExtent[3], new EpsgCrs(4326));
        if (!bbox.getEpsgCrs().equals(tileMatrixSet.getCrs())) {
            CrsTransformer transformer = crsTransformation.getTransformer(bbox.getEpsgCrs(), tileMatrixSet.getCrs());
            try {
                bbox = transformer.transformBoundingBox(new BoundingBox(spatialExtent[1], spatialExtent[0], spatialExtent[3], spatialExtent[2], new EpsgCrs(4326)));
            } catch (CrsTransformationException e) {
                e.printStackTrace();
            }
        }

        return generateLimitsList(tileMatrixRange, bbox, tileMatrixSet);
    }


    /**
     * Construct a list of TileMatrixSetLimits for the given bounding box and tileMatrix range
     * @param tileMatrixRange range of tileMatrix values
     * @param bbox bounding box
     * @return list of TileMatrixSetLimits
     */
    private List<TileMatrixSetLimits> generateLimitsList(MinMax tileMatrixRange, BoundingBox bbox, TileMatrixSet tileMatrixSet) {
        ImmutableList.Builder<TileMatrixSetLimits> limits = new ImmutableList.Builder<>();
        for (int tileMatrix = tileMatrixRange.getMin(); tileMatrix <= tileMatrixRange.getMax(); tileMatrix++) {
            List<Integer> upperLeftCornerTile = MultitilesUtils.pointToTile(bbox.getXmin(), bbox.getYmax(), tileMatrix, tileMatrixSet);
            List<Integer> lowerRightCornerTile = MultitilesUtils.pointToTile(bbox.getXmax(), bbox.getYmin(), tileMatrix, tileMatrixSet);
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
