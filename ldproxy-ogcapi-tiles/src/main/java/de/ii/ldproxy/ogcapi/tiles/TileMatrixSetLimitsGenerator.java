/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class is responsible for generating a list of tileMatrixSetLimits in json responses for /tiles and /collections/{collectionsId}/tiles requests.
 */
public class TileMatrixSetLimitsGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TileMatrixSetLimitsGenerator.class);

    /**
     * Return a list of tileMatrixSetLimits for a single collection.
     * @param data service dataset
     * @param collectionId name of the collection
     * @param tileMatrixSetId identifier of a specific tile matrix set
     * @param crsTransformation crs transfromation
     * @return list of TileMatrixSetLimits
     */
    public static List<TileMatrixSetLimits> getCollectionTileMatrixSetLimits(OgcApiApiDataV2 data, String collectionId,
                                                                             String tileMatrixSetId, MinMax tileMatrixRange,
                                                                             CrsTransformerFactory crsTransformation) {

        TileMatrixSet tileMatrixSet = TileMatrixSetCache.getTileMatrixSet(tileMatrixSetId);

        List<FeatureTypeConfigurationOgcApi> collectionData = data.getCollections()
                .values()
                .stream()
                .filter(featureType -> collectionId.equals(featureType.getId()))
                .collect(Collectors.toList());
        Optional<BoundingBox> bbox = collectionData.get(0).getExtent().getSpatial();

        if (!bbox.isPresent()) {
            return ImmutableList.of();
        }

        Optional<CrsTransformer> transformer = crsTransformation.getTransformer(bbox.get().getEpsgCrs(), tileMatrixSet.getCrs());

        if (transformer.isPresent()) {
            try {
                BoundingBox transformedBbox = transformer.get().transformBoundingBox(bbox.get());

                return tileMatrixSet.getLimitsList(tileMatrixRange, transformedBbox);
            } catch (CrsTransformationException e) {
                LOGGER.error(String.format(Locale.US, "Cannot generate tile matrix set limits. Error converting bounding box (%f, %f, %f, %f) to %s.", bbox.get().getXmin(), bbox.get().getYmin(), bbox.get().getXmax(), bbox.get().getYmax(), bbox.get().getEpsgCrs().toSimpleString()));
                return ImmutableList.of();
            }
        }
        return ImmutableList.of();
    }

    /**
     * Return a list of tileMatrixSetLimits for all collections in the dataset.
     * @param data service dataset
     * @param tileMatrixSetId identifier of a specific tile matrix set
     * @param crsTransformerFactory crs transfromation
     * @return list of TileMatrixSetLimits
     */
    public static List<TileMatrixSetLimits> getTileMatrixSetLimits(OgcApiApiDataV2 data, String tileMatrixSetId,
                                                                   MinMax tileMatrixRange, CrsTransformerFactory crsTransformerFactory) {

        TileMatrixSet tileMatrixSet = TileMatrixSetCache.getTileMatrixSet(tileMatrixSetId);

        BoundingBox bbox = data.getSpatialExtent();
        Optional<CrsTransformer> transformer = crsTransformerFactory.getTransformer(bbox.getEpsgCrs(), tileMatrixSet.getCrs());
        if (transformer.isPresent()) {
            try {
                bbox = transformer.get().transformBoundingBox(bbox);
            } catch (CrsTransformationException e) {
                LOGGER.error(String.format(Locale.US, "Cannot generate tile matrix set limits. Error converting bounding box (%f, %f, %f, %f) to %s.", bbox.getXmin(), bbox.getYmin(), bbox.getXmax(), bbox.getYmax(), bbox.getEpsgCrs().toSimpleString()));
                return ImmutableList.of();
            }
        }

        return tileMatrixSet.getLimitsList(tileMatrixRange, bbox);
    }
}
