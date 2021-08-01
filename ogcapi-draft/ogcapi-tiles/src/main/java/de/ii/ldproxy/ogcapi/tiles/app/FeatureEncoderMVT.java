/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.tiles.domain.FeatureTransformationContextTiles;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableMvtFeature;
import de.ii.ldproxy.ogcapi.tiles.domain.MvtFeature;
import de.ii.ldproxy.ogcapi.tiles.domain.Rule;
import de.ii.ldproxy.ogcapi.tiles.domain.Tile;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.features.domain.FeatureObjectEncoder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import no.ecc.vectortile.VectorTileEncoder;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureEncoderMVT extends FeatureObjectEncoder<PropertyMVT, FeatureMVT> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEncoderMVT.class);

  private final FeatureTransformationContextTiles encodingContext;
  private final TilesConfiguration tilesConfiguration;
  private final String collectionId;
  private final Tile tile;
  private final TileMatrixSet tileMatrixSet;
  private final VectorTileEncoder tileEncoder;
  private final AffineTransformation affineTransformation;
  private final double maxRelativeAreaChangeInPolygonRepair;
  private final double maxAbsoluteAreaChangeInPolygonRepair;
  private final double minimumSizeInPixel;
  private final String layerName;
  private final List<String> properties;
  private final boolean allProperties;
  private final PrecisionModel tilePrecisionModel;
  private final GeometryPrecisionReducer reducer;
  private final GeometryFactory geometryFactoryTile;
  private final GeometryFactory geometryFactoryWorld;
  private final Polygon clipGeometry;
  private final List<String> groupBy;
  private final Set<MvtFeature> mergeFeatures;

  private long mergeCount = 0;
  private final long transformerStart = System.nanoTime();
  private long processingStart;
  private Long featureStart = null;
  private long featureCount = 0;
  private long featureDuration = 0;
  private long returned = 0;
  private long written = 0;

  public FeatureEncoderMVT(FeatureTransformationContextTiles encodingContext) {
    this.encodingContext = encodingContext;
    this.tilesConfiguration = encodingContext.tilesConfiguration();
    this.collectionId = encodingContext.getCollectionId();
    this.tile = encodingContext.tile();
    this.tileMatrixSet = tile.getTileMatrixSet();
    this.tileEncoder = new VectorTileEncoder(tileMatrixSet.getTileExtent());
    this.affineTransformation = tile.createTransformNativeToTile();
    this.maxRelativeAreaChangeInPolygonRepair = tilesConfiguration.getMaxRelativeAreaChangeInPolygonRepairDerived();
    this.maxAbsoluteAreaChangeInPolygonRepair = tilesConfiguration.getMaxAbsoluteAreaChangeInPolygonRepairDerived();
    this.minimumSizeInPixel = tilesConfiguration.getMinimumSizeInPixelDerived();
    this.layerName = Objects.requireNonNullElse(collectionId, "layer");
    this.properties = encodingContext.getFields();
    this.allProperties = properties.contains("*");
    this.tilePrecisionModel = new PrecisionModel((double)tileMatrixSet.getTileExtent() / (double)tileMatrixSet.getTileSize());
    this.reducer = new GeometryPrecisionReducer(tilePrecisionModel);
    this.geometryFactoryTile = new GeometryFactory(tilePrecisionModel);
    this.geometryFactoryWorld = new GeometryFactory();

    final int size = tileMatrixSet.getTileSize();
    final int buffer = 8;
    CoordinateXY[] coords = new CoordinateXY[5];
    coords[0] = new CoordinateXY(-buffer, size+buffer);
    coords[1] = new CoordinateXY(size+buffer, size+buffer);
    coords[2] = new CoordinateXY(size+buffer, -buffer);
    coords[3] = new CoordinateXY(-buffer, -buffer);
    coords[4] = coords[0];
    this.clipGeometry = geometryFactoryTile.createPolygon(coords);

    final Map<String, List<Rule>> rules = tilesConfiguration.getRulesDerived();
    this.groupBy = (Objects.nonNull(rules) && rules.containsKey(tileMatrixSet.getId())) ?
        rules.get(tileMatrixSet.getId()).stream()
            .filter(rule -> rule.getMax()>=tile.getTileLevel() && rule.getMin()<=tile.getTileLevel() && rule.getMerge().orElse(false))
            .map(Rule::getGroupBy)
            .findAny()
            .orElse(null) :
        null;
    this.mergeFeatures = new HashSet<>();
  }

  @Override
  public FeatureMVT createFeature() {
    return ModifiableFeatureMVT.create();
  }

  @Override
  public PropertyMVT createProperty() {
    return ModifiablePropertyMVT.create();
  }

  @Override
  public void onStart(ModifiableContext context) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Start generating tile for collection {}, tile {}/{}/{}/{}.", collectionId,
          tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol());
    }
    this.processingStart = System.nanoTime();
  }

  @Override
  public void onFeature(FeatureMVT feature) {
    Optional<Geometry> featureGeometry = feature.getJtsGeometry(geometryFactoryWorld);

    if (featureGeometry.isEmpty()) {
      return;
    }

    try {
      Geometry tileGeometry = TileGeometryUtil
          .getTileGeometry(featureGeometry.get(), affineTransformation, clipGeometry, reducer, tilePrecisionModel, minimumSizeInPixel, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair);
      if (Objects.isNull(tileGeometry)) {
        return;
      }

      // if polygons have to be merged, store them for now and process at the end
      if (Objects.nonNull(groupBy) && tileGeometry.getGeometryType().contains("Polygon")) {
        mergeFeatures.add(new ImmutableMvtFeature.Builder()
            .id(++mergeCount)
            .properties(feature.getPropertiesAsMap())
            .geometry(tileGeometry)
            .build());
        return;
      }

      // Geometry is invalid -> log this information and skip it, if that option is used
      if (!tileGeometry.isValid()) {
        LOGGER.info("Feature {} in collection {} has an invalid tile geometry in tile {}/{}/{}/{}. Size in pixels: {}.", feature.getIdValue(), collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), featureGeometry.get().getArea());
        if (encodingContext.tilesConfiguration().isIgnoreInvalidGeometriesDerived()) {
          return;
        }
      }

      // If we have an id that happens to be a long value, use it
      Long id = null;
      if (feature.getIdValue() != null) {
        try {
          id = Long.parseLong(feature.getIdValue());
        } catch (Exception e) {
          // nothing to do
        }
      }

      // Add the feature with the layer name, a Map with attributes and the JTS Geometry.
      if (Objects.nonNull(id)) {
        tileEncoder.addFeature(layerName, feature.getPropertiesAsMap(), tileGeometry, id);
      } else {
        tileEncoder.addFeature(layerName, feature.getPropertiesAsMap(), tileGeometry);
      }
      written++;

    } catch (Exception e) {
      LOGGER.error("Error while processing feature {} in tile {}/{}/{}/{} in collection {}. The feature is skipped.", feature.getIdValue(), tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), collectionId);
      if(LOGGER.isDebugEnabled()) {
        LOGGER.debug("Stacktrace:", e);
      }
    }
  }

  //TODO: 2585449 feuerwehr size with master
  // 2561287

  @Override
  public void onEnd(ModifiableContext context) {
    long mergerStart = System.nanoTime();
    if (Objects.nonNull(groupBy) && mergeCount >0) {
      FeatureMerger merger = new FeatureMerger(groupBy, allProperties, properties, geometryFactoryTile, tilePrecisionModel, String.format("Collection %s, tile %s/%d/%d/%d", collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol()));
      merger.merge(mergeFeatures).forEach(mergedFeature -> {
        Geometry geom = mergedFeature.getGeometry();
        // Geometry is invalid? -> log this information and skip it, if that option is used
        if (!geom.isValid()) {
          LOGGER.info("A merged feature in collection {} has an invalid tile geometry in tile {}/{}/{}/{}. Properties: {}", collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), mergedFeature.getProperties());
          if (tilesConfiguration.isIgnoreInvalidGeometriesDerived())
            return;
        }
        tileEncoder.addFeature(layerName, mergedFeature.getProperties(), geom);
        written++;
      });
    }
    long mergerDuration = (System.nanoTime() - mergerStart) / 1000000;

    long encoderStart = System.nanoTime();

    byte[] mvt = tileEncoder.encode();
    push(mvt);

    if (LOGGER.isDebugEnabled()) {
      long encoderDuration = (System.nanoTime() - encoderStart) / 1000000;
      long transformerDuration = (System.nanoTime() - transformerStart) / 1000000;
      long processingDuration = (System.nanoTime() - processingStart) / 1000000;
      String text = String.format("Collection %s, tile %s/%d/%d/%d written. Features returned: %d, written: %d, total duration: %dms, processing: %dms, feature post-processing: %dms, average feature post-processing: %dms, merging: %dms, encoding: %dms.",
          collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), context.metadata().getNumberReturned().orElse(0), written,
          transformerDuration, processingDuration, featureDuration / 1000000, featureCount == 0 ? 0 : featureDuration / featureCount / 1000000, mergerDuration, encoderDuration);
      //if (processingDuration > 200)
        LOGGER.debug(text);
      //else
      //  LOGGER.trace(text);
    }
  }
}
