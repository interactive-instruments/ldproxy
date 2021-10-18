/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.tileMatrixSet;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionDynamicMetadataRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
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
    private final CollectionDynamicMetadataRegistry metadataRegistry;

    public TileMatrixSetLimitsGeneratorImpl(@Requires CrsTransformerFactory crsTransformerFactory,
                                            @Requires CollectionDynamicMetadataRegistry metadataRegistry) {
        this.crsTransformerFactory = crsTransformerFactory;
        this.metadataRegistry = metadataRegistry;
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

        Optional<BoundingBox> bbox = Optional.empty();
        try {
            bbox = metadataRegistry.getSpatialExtent(data.getId(), collectionId, tileMatrixSet.getCrs());
        } catch (CrsTransformationException e) {
            LOGGER.error(String.format("Error converting bounding box to CRS %s.", tileMatrixSet.getCrs()));
        }

        if (bbox.isEmpty()) {
            LOGGER.error("Cannot generate tile matrix set limits.");
            return ImmutableList.of();
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

        Optional<BoundingBox> bbox = Optional.empty();
        try {
            bbox = metadataRegistry.getSpatialExtent(data.getId(), tileMatrixSet.getCrs());
        } catch (CrsTransformationException e) {
            LOGGER.error(String.format("Error converting bounding box to CRS %s.", tileMatrixSet.getCrs()));
        }

        if (bbox.isEmpty()) {
            LOGGER.error("Cannot generate tile matrix set limits.");
            return ImmutableList.of();
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

        Optional<BoundingBox> bbox = getBoundingBoxInTileMatrixSetCrs(boundingBox, tileMatrixSet);

        if (bbox.isEmpty()) {
            LOGGER.error("Cannot generate tile matrix set limits.");
            return ImmutableList.of();
        }

        return tileMatrixSet.getLimitsList(tileMatrixRange, bbox.get());
    }

    @Override
    public Optional<BoundingBox> getBoundingBoxInTileMatrixSetCrs(BoundingBox bbox, TileMatrixSet tileMatrixSet) {
        EpsgCrs sourceCrs = bbox.getEpsgCrs();
        EpsgCrs targetCrs = tileMatrixSet.getCrs();
        if (sourceCrs.getCode()== targetCrs.getCode() && sourceCrs.getForceAxisOrder()==targetCrs.getForceAxisOrder())
            return Optional.of(bbox);

        Optional<CrsTransformer> transformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
        if (transformer.isPresent()) {
            try {
                return Optional.ofNullable(transformer.get()
                                                      .transformBoundingBox(bbox));
            } catch (CrsTransformationException e) {
                LOGGER.error(String.format(Locale.US, "Cannot convert bounding box (%f, %f, %f, %f) from %s to %s. Reason: %s", bbox.getXmin(), bbox.getYmin(), bbox.getXmax(), bbox.getYmax(), sourceCrs, targetCrs, e.getMessage()));
                return Optional.empty();
            }
        }
        LOGGER.error(String.format(Locale.US, "Cannot convert bounding box (%f, %f, %f, %f) from %s to %s. Reason: no applicable transformer found.", bbox.getXmin(), bbox.getYmin(), bbox.getXmax(), bbox.getYmax(), sourceCrs, targetCrs));
        return Optional.empty();
    }
}
