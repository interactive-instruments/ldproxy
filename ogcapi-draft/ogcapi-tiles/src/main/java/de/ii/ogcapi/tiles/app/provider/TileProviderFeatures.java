/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.PARAMETER_BBOX;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.crs.domain.CrsSupport;
import de.ii.ogcapi.features.core.domain.FeatureQueryTransformer;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.tiles.app.TilesBuildingBlock;
import de.ii.ogcapi.tiles.domain.PredefinedFilter;
import de.ii.ogcapi.tiles.domain.Tile;
import de.ii.ogcapi.tiles.domain.TileCache;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.provider.ImmutableLayerOptions;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileGenerationContext;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileProviderFeaturesData;
import de.ii.ogcapi.tiles.domain.provider.LayerOptions;
import de.ii.ogcapi.tiles.domain.provider.Rule;
import de.ii.ogcapi.tiles.domain.provider.TileCoordinates;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationContext;
import de.ii.ogcapi.tiles.domain.provider.TileGenerator;
import de.ii.ogcapi.tiles.domain.provider.TileProvider;
import de.ii.ogcapi.tiles.domain.provider.TileProviderFeaturesData;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.Cql;
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
import de.ii.xtraplatform.features.domain.FeatureStream;
import de.ii.xtraplatform.features.domain.FeatureStream.ResultReduced;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReduced;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.Unit;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.apache.http.NameValuePair;
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

  private final CrsSupport crsSupport;
  private final CrsInfo crsInfo;
  private final TileCache tileCache;
  // TODO
  private final FeaturesQuery queryParser;
  private final EntityRegistry entityRegistry;
  // TODO
  private final TileProviderFeaturesData data =
      ImmutableTileProviderFeaturesData.builder()
          .layerDefaults(new ImmutableLayerOptions.Builder().featureProvider("bergbau").build())
          .build();

  @Inject
  public TileProviderFeatures(
      CrsSupport crsSupport,
      CrsInfo crsInfo,
      TileCache tileCache,
      FeaturesQuery queryParser,
      EntityRegistry entityRegistry) {
    this.crsSupport = crsSupport;
    this.crsInfo = crsInfo;
    this.tileCache = tileCache;
    this.queryParser = queryParser;
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
  public byte[] generateTile(TileQuery tileQuery, MediaType mediaType) {
    if (!ENCODERS.containsKey(mediaType)) {
      throw new IllegalArgumentException(String.format("Encoding not supported: %s", mediaType));
    }

    FeatureStream tileSource = getTileSource(tileQuery);

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
  public FeatureStream getTileSource(TileQuery tileQuery) {
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

    FeatureQuery featureQuery = getQuery(tileQuery, data.getLayerDefaults(), nativeCrs);

    // TODO:
    // FeatureQuery featureQuery = getQuery(null, null, null, null, null);

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

  public FeatureQuery getQuery(TileQuery tile, LayerOptions options, EpsgCrs nativeCrs) {
    String featureType = tile.getLayer();

    ImmutableFeatureQuery.Builder queryBuilder =
        ImmutableFeatureQuery.builder()
            .type(featureType)
            .limit(options.getFeatureLimit())
            .offset(0)
            .crs(tile.getTileMatrixSet().getCrs())
            .maxAllowableOffset(getMaxAllowableOffset(tile, nativeCrs));

    tile.userParameters()
        .ifPresent(
            userParameters -> {
              userParameters.getLimit().ifPresent(queryBuilder::limit);
              queryBuilder.addAllFilters(userParameters.getFilters());
              if (!userParameters.getFields().isEmpty()) {
                queryBuilder.addAllFields(userParameters.getFields());
              }
            });

    return queryBuilder.build();
  }

  // TODO: which params are allowed? limit[done],properties,filter,datetime,
  // TODO: first capture in TileQuery as UserOptions?, then transform to FeatureQuery
  // TODO: introduce QueryParameterSet for filter,filter-lang,filter-crs
  public FeatureQuery getQuery(
      Tile tile,
      List<OgcApiQueryParameter> allowedParameters,
      Map<String, String> queryParameters,
      TilesConfiguration tilesConfiguration,
      URICustomizer uriCustomizer) {

    String collectionId = tile.getCollectionId();
    String tileMatrixSetId = tile.getTileMatrixSet().getId();
    int level = tile.getTileLevel();

    final Map<String, List<PredefinedFilter>> predefFilters =
        tilesConfiguration.getFiltersDerived();
    final String predefFilter =
        (Objects.nonNull(predefFilters) && predefFilters.containsKey(tileMatrixSetId))
            ? predefFilters.get(tileMatrixSetId).stream()
                .filter(
                    filter ->
                        filter.getMax() >= level
                            && filter.getMin() <= level
                            && filter.getFilter().isPresent())
                .map(filter -> filter.getFilter().get())
                .findAny()
                .orElse(null)
            : null;

    String featureTypeId =
        tile.getApiData()
            .getCollections()
            .get(collectionId)
            .getExtension(FeaturesCoreConfiguration.class)
            .map(cfg -> cfg.getFeatureType().orElse(collectionId))
            .orElse(collectionId);
    ImmutableFeatureQuery.Builder queryBuilder =
        ImmutableFeatureQuery.builder()
            .type(featureTypeId)
            .limit(
                Objects.requireNonNullElse(
                    tilesConfiguration.getLimitDerived(), TilesBuildingBlock.LIMIT_DEFAULT))
            .offset(0)
            .crs(tile.getTileMatrixSet().getCrs())
            .maxAllowableOffset(getMaxAllowableOffset(tile));

    final Map<String, List<Rule>> rules = tilesConfiguration.getRulesDerived();
    if (!queryParameters.containsKey("properties")
        && (Objects.nonNull(rules) && rules.containsKey(tileMatrixSetId))) {
      List<String> properties =
          rules.get(tileMatrixSetId).stream()
              .filter(rule -> rule.getMax() >= level && rule.getMin() <= level)
              .map(Rule::getProperties)
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
      if (!properties.isEmpty()) {
        queryParameters =
            ImmutableMap.<String, String>builder()
                .putAll(queryParameters)
                .put("properties", String.join(",", properties))
                .build();
      }
    }

    OgcApiDataV2 apiData = tile.getApiData();
    FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);

    final Map<String, String> filterableFields =
        queryParser.getFilterableFields(apiData, collectionData);
    final Map<String, String> queryableTypes =
        queryParser.getQueryableTypes(apiData, collectionData);

    Set<String> filterParameters = ImmutableSet.of();
    for (OgcApiQueryParameter parameter : allowedParameters) {
      filterParameters =
          parameter.getFilterParameters(filterParameters, apiData, collectionData.getId());
      queryParameters = parameter.transformParameters(collectionData, queryParameters, apiData);
    }

    final Set<String> finalFilterParameters = filterParameters;
    final Map<String, String> filters =
        queryParameters.entrySet().stream()
            .filter(
                entry ->
                    finalFilterParameters.contains(entry.getKey())
                        || filterableFields.containsKey(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // TODO
    for (OgcApiQueryParameter parameter : allowedParameters) {
      if (parameter instanceof FeatureQueryTransformer) {
        ((FeatureQueryTransformer) parameter)
            .transformQuery(queryBuilder, queryParameters, apiData, collectionData);
      }
    }

    BoundingBox bbox = tile.getBoundingBox();
    // reduce bbox to the area in which there is data (to avoid coordinate transformation issues
    // with large scale and data that is stored in a regional, projected CRS)
    final EpsgCrs crs = bbox.getEpsgCrs();
    final Optional<BoundingBox> dataBbox = tile.getApi().getSpatialExtent(collectionId, crs);
    if (dataBbox.isPresent()) {
      bbox =
          ImmutableList.of(bbox, dataBbox.get()).stream()
              .map(BoundingBox::toArray)
              .reduce(
                  (doubles, doubles2) ->
                      new double[] {
                        Math.max(doubles[0], doubles2[0]),
                        Math.max(doubles[1], doubles2[1]),
                        Math.min(doubles[2], doubles2[2]),
                        Math.min(doubles[3], doubles2[3])
                      })
              .map(doubles -> BoundingBox.of(doubles[0], doubles[1], doubles[2], doubles[3], crs))
              .orElse(bbox);
    }

    Cql2Expression spatialPredicate =
        SIntersects.of(
            Property.of(filterableFields.get(PARAMETER_BBOX)),
            SpatialLiteral.of(Envelope.of(bbox)));
    if (predefFilter != null || !filters.isEmpty()) {
      Optional<Cql2Expression> otherFilter = Optional.empty();
      Optional<Cql2Expression> configFilter = Optional.empty();
      if (!filters.isEmpty()) {
        Optional<String> filterLang =
            uriCustomizer.getQueryParams().stream()
                .filter(param -> "filter-lang".equals(param.getName()))
                .map(NameValuePair::getValue)
                .findFirst();
        Cql.Format cqlFormat = Cql.Format.TEXT;
        if (filterLang.isPresent() && "cql2-json".equals(filterLang.get())) {
          cqlFormat = Cql.Format.JSON;
        }
        otherFilter =
            queryParser.getFilterFromQuery(
                filters, filterableFields, ImmutableSet.of("filter"), queryableTypes, cqlFormat);
      }
      if (predefFilter != null) {
        configFilter =
            queryParser.getFilterFromQuery(
                ImmutableMap.of("filter", predefFilter),
                filterableFields,
                ImmutableSet.of("filter"),
                queryableTypes,
                Cql.Format.TEXT);
      }
      Cql2Expression combinedFilter;
      if (otherFilter.isPresent() && configFilter.isPresent()) {
        combinedFilter = And.of(otherFilter.get(), configFilter.get(), spatialPredicate);
      } else if (otherFilter.isPresent()) {
        combinedFilter = And.of(otherFilter.get(), spatialPredicate);
      } else if (configFilter.isPresent()) {
        combinedFilter = And.of(configFilter.get(), spatialPredicate);
      } else {
        combinedFilter = spatialPredicate;
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.trace("Filter: {}", combinedFilter);
      }

      queryBuilder.filter(combinedFilter);
    } else {
      queryBuilder.filter(spatialPredicate);
    }

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

  public double getMaxAllowableOffset(Tile tile) {
    double maxAllowableOffsetTileMatrixSet =
        tile.getTileMatrixSet()
            .getMaxAllowableOffset(tile.getTileLevel(), tile.getTileRow(), tile.getTileCol());
    Unit<?> tmsCrsUnit = crsInfo.getUnit(tile.getTileMatrixSet().getCrs());
    EpsgCrs nativeCrs = crsSupport.getStorageCrs(tile.getApiData(), Optional.empty());
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
}
