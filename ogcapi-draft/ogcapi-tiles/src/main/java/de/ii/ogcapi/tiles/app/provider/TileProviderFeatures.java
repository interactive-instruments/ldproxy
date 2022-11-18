/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.DATETIME_INTERVAL_SEPARATOR;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.tiles.domain.TileCache;
import de.ii.ogcapi.tiles.domain.provider.ImmutableLayerOptions;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileGenerationContext;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileProviderFeaturesData;
import de.ii.ogcapi.tiles.domain.provider.LayerOptions;
import de.ii.ogcapi.tiles.domain.provider.TileCoordinates;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationContext;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationSchema;
import de.ii.ogcapi.tiles.domain.provider.TileGenerator;
import de.ii.ogcapi.tiles.domain.provider.TileProvider;
import de.ii.ogcapi.tiles.domain.provider.TileProviderFeaturesData;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.cql.domain.Geometry.Envelope;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.SIntersects;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureStream;
import de.ii.xtraplatform.features.domain.FeatureStream.ResultReduced;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReduced;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.Unit;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.kortforsyningen.proj.Units;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: who creates this? entity factory?
@Singleton
@AutoBind
public class TileProviderFeatures implements TileProvider, TileGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileProviderFeatures.class);
  private static final Map<
          MediaType, Function<TileGenerationContext, ? extends FeatureTokenEncoder<?>>>
      ENCODERS = ImmutableMap.of(FeatureEncoderMVT.FORMAT, FeatureEncoderMVT::new);
  private static final double BUFFER_DEGREE = 0.00001;
  private static final double BUFFER_METRE = 10.0;

  private final CrsInfo crsInfo;
  private final TileCache tileCache;
  private final EntityRegistry entityRegistry;
  // TODO
  private final TileProviderFeaturesData data =
      ImmutableTileProviderFeaturesData.builder()
          .layerDefaults(new ImmutableLayerOptions.Builder().featureProvider("bergbau").build())
          .build();

  @Inject
  public TileProviderFeatures(CrsInfo crsInfo, TileCache tileCache, EntityRegistry entityRegistry) {
    this.crsInfo = crsInfo;
    this.tileCache = tileCache;
    this.entityRegistry = entityRegistry;
  }

  @Override
  public TileResult getTile(TileQuery tileQuery) {
    // TODO: check cache(s) (or delegate)

    TileResult cacheResult = TileResult.notFound();

    return cacheResult;
  }

  @Override
  public boolean supports(MediaType mediaType) {
    return ENCODERS.containsKey(mediaType);
  }

  // TODO: streaming?
  @Override
  public byte[] generateTile(
      TileQuery tileQuery, MediaType mediaType, Optional<BoundingBox> bounds) {
    if (!ENCODERS.containsKey(mediaType)) {
      throw new IllegalArgumentException(String.format("Encoding not supported: %s", mediaType));
    }

    FeatureStream tileSource = getTileSource(tileQuery, bounds);

    TileGenerationContext tileGenerationContext =
        new ImmutableTileGenerationContext.Builder()
            .parameters(data.getLayerDefaults())
            .coordinates(tileQuery)
            .collectionId(tileQuery.getLayer())
            // .fields
            // .limit(query.getLimit())
            .build();

    FeatureTokenEncoder<?> encoder = ENCODERS.get(mediaType).apply(tileGenerationContext);

    ResultReduced<byte[]> resultReduced = generateTile(tileSource, encoder, tileQuery, Map.of());

    return resultReduced.reduced();
  }

  @Override
  public FeatureStream getTileSource(TileQuery tileQuery, Optional<BoundingBox> bounds) {
    String featureProviderId = data.getLayerDefaults().getFeatureProvider().get();
    FeatureProvider2 featureProvider =
        entityRegistry
            .getEntity(FeatureProvider2.class, featureProviderId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        String.format(
                            "Feature provider with id '%s' not found.", featureProviderId)));

    if (!featureProvider.supportsQueries()) {
      throw new IllegalStateException("Feature provider has no Queries support.");
    }
    if (!featureProvider.supportsCrs()) {
      throw new IllegalStateException("Feature provider has no CRS support.");
    }

    EpsgCrs nativeCrs = featureProvider.crs().getNativeCrs();
    Map<String, FeatureSchema> types = featureProvider.getData().getTypes();
    FeatureQuery featureQuery =
        getFeatureQuery(tileQuery, data.getLayerDefaults(), types, nativeCrs, bounds);

    return featureProvider.queries().getFeatureStream(featureQuery);
  }

  private ResultReduced<byte[]> generateTile(
      FeatureStream featureStream,
      FeatureTokenEncoder<?> encoder,
      TileCoordinates tile,
      Map<String, PropertyTransformations> propertyTransformations) {

    SinkReduced<Object, byte[]> featureSink = encoder.to(Sink.reduceByteArray());

    try {
      ResultReduced<byte[]> result =
          featureStream.runWith(featureSink, propertyTransformations).toCompletableFuture().join();

      if (result.isSuccess()) {
        try {
          // write/update tile in cache
          // tileCache.storeTile(tile, result.reduced());
        } catch (Throwable e) {
          String msg =
              "Failure to write the multi-layer file of tile {}/{}/{}/{} in dataset '{}', format '{}' to the cache";
          LogContext.errorAsInfo(
              LOGGER,
              e,
              msg,
              tile.getTileMatrixSet().getId(),
              tile.getTileLevel(),
              tile.getTileRow(),
              tile.getTileCol(),
              "TODO",
              "TODO");
        }
      } else {
        result.getError().ifPresent(QueriesHandler::processStreamError);
      }

      return result;

    } catch (CompletionException e) {
      if (e.getCause() instanceof WebApplicationException) {
        throw (WebApplicationException) e.getCause();
      }
      throw new IllegalStateException("Feature stream error.", e.getCause());
    }
  }

  // TODO: create on startup for all layers
  @Override
  public TileGenerationSchema getGenerationSchema(String layer, Map<String, String> queryables) {
    String featureProviderId = data.getLayerDefaults().getFeatureProvider().get();
    FeatureProvider2 featureProvider =
        entityRegistry
            .getEntity(FeatureProvider2.class, featureProviderId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        String.format(
                            "Feature provider with id '%s' not found.", featureProviderId)));
    Map<String, FeatureSchema> featureTypes = featureProvider.getData().getTypes();
    FeatureSchema featureSchema = featureTypes.get(layer);
    return new TileGenerationSchema() {
      @Override
      public String getSpatialProperty() {
        return featureSchema.getPrimaryGeometry().orElseThrow().getFullPathAsString();
      }

      @Override
      public Optional<String> getTemporalProperty() {
        return featureSchema
            .getPrimaryInterval()
            .map(
                interval ->
                    String.format(
                        "%s%s%s",
                        interval.first().getFullPathAsString(),
                        DATETIME_INTERVAL_SEPARATOR,
                        interval.second().getFullPathAsString()))
            .or(() -> featureSchema.getPrimaryInstant().map(SchemaBase::getFullPathAsString));
      }

      // TODO
      @Override
      public Map<String, String> getProperties() {
        return queryables;
      }
    };
  }

  private FeatureQuery getFeatureQuery(
      TileQuery tile,
      LayerOptions options,
      Map<String, FeatureSchema> featureTypes,
      EpsgCrs nativeCrs,
      Optional<BoundingBox> bounds) {
    String featureType = tile.getLayer();
    FeatureSchema featureSchema = featureTypes.get(featureType);

    ImmutableFeatureQuery.Builder queryBuilder =
        ImmutableFeatureQuery.builder()
            .type(featureType)
            .limit(options.getFeatureLimit())
            .offset(0)
            .crs(tile.getTileMatrixSet().getCrs())
            .maxAllowableOffset(getMaxAllowableOffset(tile, nativeCrs));

    // TODO: add predefFilter from config

    String spatialProperty = featureSchema.getPrimaryGeometry().orElseThrow().getFullPathAsString();
    BoundingBox bbox = clip(tile.getBoundingBox(), bounds);
    Cql2Expression spatialPredicate =
        SIntersects.of(Property.of(spatialProperty), SpatialLiteral.of(Envelope.of(bbox)));
    queryBuilder.addFilters(spatialPredicate);

    tile.userParameters()
        .ifPresent(
            userParameters -> {
              userParameters.getLimit().ifPresent(queryBuilder::limit);
              queryBuilder.addAllFilters(userParameters.getFilters());
              if (!userParameters.getFields().isEmpty()) {
                queryBuilder.addAllFields(userParameters.getFields());
              }
            });

    // TODO: properties from rules if fields still empty

    return queryBuilder.build();
  }

  public double getMaxAllowableOffset(TileCoordinates tile, EpsgCrs nativeCrs) {
    double maxAllowableOffsetTileMatrixSet =
        tile.getTileMatrixSet()
            .getMaxAllowableOffset(tile.getTileLevel(), tile.getTileRow(), tile.getTileCol());
    Unit<?> tmsCrsUnit = crsInfo.getUnit(tile.getTileMatrixSet().getCrs());
    Unit<?> nativeCrsUnit = crsInfo.getUnit(nativeCrs);
    if (tmsCrsUnit.equals(nativeCrsUnit)) {
      return maxAllowableOffsetTileMatrixSet;
    } else if (tmsCrsUnit.equals(Units.DEGREE) && nativeCrsUnit.equals(Units.METRE)) {
      return maxAllowableOffsetTileMatrixSet * 111333.0;
    } else if (tmsCrsUnit.equals(Units.METRE) && nativeCrsUnit.equals(Units.DEGREE)) {
      return maxAllowableOffsetTileMatrixSet / 111333.0;
    }

    LOGGER.warn(
        "Tile generation: cannot convert between axis units '{}' and '{}'.",
        tmsCrsUnit.getName(),
        nativeCrsUnit.getName());
    return 0;
  }

  /**
   * Reduce bbox to the area in which there is data to avoid coordinate transformation issues with
   * large scale and data that is stored in a regional, projected CRS. A small buffer is used to
   * avoid issues with point features and queries in other CRSs where features on the boundary of
   * the spatial extent are suddenly no longer included in the result.
   */
  private BoundingBox clip(BoundingBox bbox, Optional<BoundingBox> limits) {
    if (limits.isEmpty()) {
      return bbox;
    }

    return BoundingBox.intersect2d(bbox, limits.get(), getBuffer(bbox.getEpsgCrs()));
  }

  private double getBuffer(EpsgCrs crs) {
    List<Unit<?>> units = crsInfo.getAxisUnits(crs);
    if (!units.isEmpty()) {
      return Units.METRE.equals(units.get(0)) ? BUFFER_METRE : BUFFER_DEGREE;
    }
    // fallback to meters
    return BUFFER_METRE;
  }
}
