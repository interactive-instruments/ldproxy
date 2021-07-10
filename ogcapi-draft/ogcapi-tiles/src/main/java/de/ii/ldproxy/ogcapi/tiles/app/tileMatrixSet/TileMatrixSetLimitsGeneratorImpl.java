/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.tileMatrixSet;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.tiles.app.TilesHelper;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * This class is responsible for generating a list of tileMatrixSetLimits in json responses for /tiles and /collections/{collectionsId}/tiles requests.
 */
@Component
@Provides
@Instantiate
public class TileMatrixSetLimitsGeneratorImpl implements TileMatrixSetLimitsGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TileMatrixSetLimitsGeneratorImpl.class);
    private final CrsTransformerFactory crsTransformerFactory;

    public TileMatrixSetLimitsGeneratorImpl(@Requires CrsTransformerFactory crsTransformerFactory) {
        this.crsTransformerFactory = crsTransformerFactory;
    }

    /**
     * Return a list of tileMatrixSetLimits for a single collection.
     * @param data service dataset
     * @param collectionId name of the collection
     * @param tileMatrixSet the tile matrix set
     * @return list of TileMatrixSetLimits
     */
    @Override
    public List<TileMatrixSetLimits> getCollectionTileMatrixSetLimits(OgcApiDataV2 data, String collectionId,
                                                                      TileMatrixSet tileMatrixSet, MinMax tileMatrixRange) {

        Optional<BoundingBox> bbox = data.getSpatialExtent(collectionId);
        if (bbox.isPresent())
            bbox = TilesHelper.getBoundingBoxInTargetCrs(bbox.get(), tileMatrixSet.getCrs(), crsTransformerFactory);

        if (bbox.isEmpty()) {
            // fallback to bbox of the tile matrix set
            LOGGER.debug("No bounding box found or bounding box cannot be transformed to the CRS of the tile matrix set for collection '{}'. Use the tile matrix set bounding box.", collectionId);
            bbox = Optional.of(tileMatrixSet.getBoundingBox());
        }

        return tileMatrixSet.getLimitsList(tileMatrixRange, bbox.get());

    }

    /**
     * Return a list of tileMatrixSetLimits for all collections in the dataset.
     * @param data service dataset
     * @param tileMatrixSet the tile matrix set
     * @return list of TileMatrixSetLimits
     */
    @Override
    public List<TileMatrixSetLimits> getTileMatrixSetLimits(OgcApiDataV2 data, TileMatrixSet tileMatrixSet,
                                                            MinMax tileMatrixRange) {

        Optional<BoundingBox> bbox = data.getSpatialExtent();
        if (bbox.isPresent())
            bbox = TilesHelper.getBoundingBoxInTargetCrs(bbox.get(), tileMatrixSet.getCrs(), crsTransformerFactory);

        if (bbox.isEmpty()) {
            // fallback to bbox of the tile matrix set
            LOGGER.debug("No bounding box found or bounding box cannot be transformed to the CRS of the tile matrix set. Use the tile matrix set bounding box.");
            bbox = Optional.of(tileMatrixSet.getBoundingBox());
        }

        return tileMatrixSet.getLimitsList(tileMatrixRange, bbox.get());
    }

    /**
     * Return a list of tileMatrixSetLimits for all collections in the dataset.
     * @param boundingBox a bounding box
     * @param tileMatrixSet the tile matrix set
     * @return list of TileMatrixSetLimits
     */
    @Override
    public List<TileMatrixSetLimits> getTileMatrixSetLimits(BoundingBox boundingBox, TileMatrixSet tileMatrixSet,
                                                            MinMax tileMatrixRange) {

        Optional<BoundingBox> bbox = TilesHelper.getBoundingBoxInTargetCrs(boundingBox, tileMatrixSet.getCrs(), crsTransformerFactory);

        if (bbox.isEmpty()) {
            // fallback to bbox of the tile matrix set
            LOGGER.debug("Bounding box cannot be transformed to the CRS of the tile matrix set. Use the tile matrix set bounding box.");
            bbox = Optional.of(tileMatrixSet.getBoundingBox());
        }

        return tileMatrixSet.getLimitsList(tileMatrixRange, bbox.get());
    }
}
