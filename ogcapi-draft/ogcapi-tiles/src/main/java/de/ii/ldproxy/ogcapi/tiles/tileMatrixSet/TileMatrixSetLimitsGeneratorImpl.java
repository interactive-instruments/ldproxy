/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.tiles.MinMax;
import de.ii.xtraplatform.crs.domain.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class is responsible for generating a list of tileMatrixSetLimits in json responses for /tiles and /collections/{collectionsId}/tiles requests.
 */
@Component
@Provides
@Instantiate
public class TileMatrixSetLimitsGeneratorImpl implements TileMatrixSetLimitsGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TileMatrixSetLimitsGeneratorImpl.class);

    /**
     * Return a list of tileMatrixSetLimits for a single collection.
     * @param data service dataset
     * @param collectionId name of the collection
     * @param tileMatrixSet the tile matrix set
     * @param crsTransformation crs transfromation
     * @return list of TileMatrixSetLimits
     */
    public List<TileMatrixSetLimits> getCollectionTileMatrixSetLimits(OgcApiDataV2 data, String collectionId,
                                                                      TileMatrixSet tileMatrixSet, MinMax tileMatrixRange,
                                                                      CrsTransformerFactory crsTransformation) {

        List<FeatureTypeConfigurationOgcApi> collectionData = data.getCollections()
                .values()
                .stream()
                .filter(featureType -> collectionId.equals(featureType.getId()))
                .collect(Collectors.toList());

        Optional<BoundingBox> bbox = data.getSpatialExtent(collectionData.get(0).getId()).isPresent()? data.getSpatialExtent(collectionData.get(0).getId()) : Optional.empty();

        if (!bbox.isPresent()) {
            return ImmutableList.of();
        }

        EpsgCrs sourceCrs = OgcCrs.CRS84;
        EpsgCrs targetCrs = tileMatrixSet.getCrs();
        if (!(targetCrs.toSimpleString().equalsIgnoreCase(sourceCrs.toSimpleString()) && targetCrs.getForceAxisOrder().equals(sourceCrs.getForceAxisOrder()))) {
            Optional<CrsTransformer> transformer = crsTransformation.getTransformer(sourceCrs, targetCrs);
            if (transformer.isPresent()) {
                try {
                    bbox = Optional.of(transformer.get().transformBoundingBox(bbox.get()));
                    if (!bbox.isPresent()) {
                        return ImmutableList.of();
                    }
                } catch (CrsTransformationException e) {
                    LOGGER.error(String.format(Locale.US, "Cannot generate tile matrix set limits. Error converting bounding box (%f, %f, %f, %f) to %s.", bbox.get().getXmin(), bbox.get().getYmin(), bbox.get().getXmax(), bbox.get().getYmax(), bbox.get().getEpsgCrs().toSimpleString()));
                    return ImmutableList.of();
                }
            }
        }
        return tileMatrixSet.getLimitsList(tileMatrixRange, bbox.get());
    }

    /**
     * Return a list of tileMatrixSetLimits for all collections in the dataset.
     * @param data service dataset
     * @param tileMatrixSet the tile matrix set
     * @param crsTransformerFactory crs transfromation
     * @return list of TileMatrixSetLimits
     */
    public List<TileMatrixSetLimits> getTileMatrixSetLimits(OgcApiDataV2 data, TileMatrixSet tileMatrixSet,
                                                            MinMax tileMatrixRange, CrsTransformerFactory crsTransformerFactory) {

        Optional<BoundingBox> bbox = data.getSpatialExtent();
        if (bbox.isPresent()) {
            Optional<CrsTransformer> transformer = crsTransformerFactory.getTransformer(bbox.get().getEpsgCrs(), tileMatrixSet.getCrs());
            if (transformer.isPresent()) {
                try {
                    bbox = Optional.ofNullable(transformer.get()
                                                          .transformBoundingBox(bbox.get()));
                } catch (CrsTransformationException e) {
                    LOGGER.error(String.format(Locale.US, "Cannot generate tile matrix set limits. Error converting bounding box (%f, %f, %f, %f) to %s.", bbox.get().getXmin(), bbox.get().getYmin(), bbox.get().getXmax(), bbox.get().getYmax(), bbox.get().getEpsgCrs()
                                                                                                                                                                                                                               .toSimpleString()));
                    return ImmutableList.of();
                }
            }
        }

        if (bbox.isPresent())
            return tileMatrixSet.getLimitsList(tileMatrixRange, bbox.get());

        return ImmutableList.of();
    }
}
