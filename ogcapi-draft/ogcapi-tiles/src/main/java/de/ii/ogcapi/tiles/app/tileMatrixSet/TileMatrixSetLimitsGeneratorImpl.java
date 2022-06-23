/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.tileMatrixSet;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.tiles.app.TilesHelper;
import de.ii.ogcapi.tiles.domain.MinMax;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for generating a list of tileMatrixSetLimits in json responses for
 * /tiles and /collections/{collectionsId}/tiles requests.
 */
@Singleton
@AutoBind
public class TileMatrixSetLimitsGeneratorImpl implements TileMatrixSetLimitsGenerator {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(TileMatrixSetLimitsGeneratorImpl.class);
  private final CrsTransformerFactory crsTransformerFactory;

  @Inject
  public TileMatrixSetLimitsGeneratorImpl(CrsTransformerFactory crsTransformerFactory) {
    this.crsTransformerFactory = crsTransformerFactory;
  }

  /**
   * Return a list of tileMatrixSetLimits for a single collection.
   *
   * @param api service dataset
   * @param collectionId name of the collection
   * @param tileMatrixSet the tile matrix set
   * @return list of TileMatrixSetLimits
   */
  @Override
  public List<TileMatrixSetLimits> getCollectionTileMatrixSetLimits(
      OgcApi api, String collectionId, TileMatrixSet tileMatrixSet, MinMax tileMatrixRange) {

    Optional<BoundingBox> bbox = api.getSpatialExtent(collectionId, tileMatrixSet.getCrs());

    if (bbox.isEmpty()) {
      // fallback to bbox of the tile matrix set
      LOGGER.debug(
          "No bounding box found or bounding box cannot be transformed to the CRS of the tile matrix set for collection '{}'. Using the tile matrix set bounding box.",
          collectionId);
      bbox = Optional.of(tileMatrixSet.getBoundingBox());
    }

    return tileMatrixSet.getLimitsList(tileMatrixRange, bbox.get());
  }

  /**
   * Return a list of tileMatrixSetLimits for all collections in the dataset.
   *
   * @param api service dataset
   * @param tileMatrixSet the tile matrix set
   * @return list of TileMatrixSetLimits
   */
  @Override
  public List<TileMatrixSetLimits> getTileMatrixSetLimits(
      OgcApi api, TileMatrixSet tileMatrixSet, MinMax tileMatrixRange) {

    Optional<BoundingBox> bbox = api.getSpatialExtent(tileMatrixSet.getCrs());

    if (bbox.isEmpty()) {
      // fallback to bbox of the tile matrix set
      LOGGER.debug(
          "No bounding box found or bounding box cannot be transformed to the CRS of the tile matrix set. Using the tile matrix set bounding box.");
      bbox = Optional.of(tileMatrixSet.getBoundingBox());
    }

    return tileMatrixSet.getLimitsList(tileMatrixRange, bbox.get());
  }

  /**
   * Return a list of tileMatrixSetLimits for all collections in the dataset.
   *
   * @param boundingBox a bounding box
   * @param tileMatrixSet the tile matrix set
   * @return list of TileMatrixSetLimits
   */
  @Override
  public List<TileMatrixSetLimits> getTileMatrixSetLimits(
      BoundingBox boundingBox, TileMatrixSet tileMatrixSet, MinMax tileMatrixRange) {

    Optional<BoundingBox> bbox =
        TilesHelper.getBoundingBoxInTargetCrs(
            boundingBox, tileMatrixSet.getCrs(), crsTransformerFactory);

    if (bbox.isEmpty()) {
      // fallback to bbox of the tile matrix set
      LOGGER.debug(
          "Bounding box cannot be transformed to the CRS of the tile matrix set. Using the tile matrix set bounding box.");
      bbox = Optional.of(tileMatrixSet.getBoundingBox());
    }

    return tileMatrixSet.getLimitsList(tileMatrixRange, bbox.get());
  }
}
