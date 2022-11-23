/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import static de.ii.ogcapi.tiles.domain.provider.LayerOptionsFeatures.COMBINE_ALL;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tilematrixsets.domain.ImmutableMinMax;
import de.ii.ogcapi.tilematrixsets.domain.MinMax;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetRepository;
import de.ii.ogcapi.tiles.domain.ImmutableTileProviderFeatures;
import de.ii.ogcapi.tiles.domain.ImmutableTilesConfiguration.Builder;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileFormatWithQuerySupportExtension;
import de.ii.ogcapi.tiles.domain.TileProviderFeatures;
import de.ii.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesConfiguration.TileCacheType;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.ogcapi.tiles.domain.provider.Cache;
import de.ii.ogcapi.tiles.domain.provider.Cache.Storage;
import de.ii.ogcapi.tiles.domain.provider.Cache.Type;
import de.ii.ogcapi.tiles.domain.provider.ImmutableCache;
import de.ii.ogcapi.tiles.domain.provider.ImmutableLayerOptionsFeatures;
import de.ii.ogcapi.tiles.domain.provider.ImmutableLayerOptionsFeaturesDefault;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileProviderFeaturesData;
import de.ii.ogcapi.tiles.domain.provider.LayerOptionsFeatures;
import de.ii.ogcapi.tiles.domain.provider.LevelFilter;
import de.ii.ogcapi.tiles.domain.provider.LevelTransformation;
import de.ii.ogcapi.tiles.domain.provider.TileProviderFeaturesData;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.features.domain.FeatureChangeListener;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.store.domain.entities.EntityFactories;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteJDBCLoader;

/**
 * @title Vector Tiles
 * @langEn The *Tiles* module can be activated for any API provided by ldproxy with an SQL feature
 *     provider or with an MBTiles tile provider. It enables the "Tilesets", "Tileset" and "Tile"
 *     resources.
 *     <p>The module is based on the draft of [OGC API - Tiles - Part 1:
 *     Core](https://github.com/opengeospatial/OGC-API-Tiles) and the draft of [OGC Two Dimensional
 *     Tile Matrix Set and Tile Set Metadata](https://docs.ogc.org/DRAFTS/17-083r4.html). The
 *     implementation will change as the draft is further standardized.
 *     <p>The supported tile formats are:
 *     <p>- MVT (Mapbox Vector Tile) - PNG - WebP - JPEG - TIFF
 *     <p>The *Tile Matrix Sets* module must be enabled.
 *     <p>The tile cache is located in the ldproxy data directory under the relative path
 *     `cache/tiles/{apiId}`. If the data for an API or tile configuration has been changed, then
 *     the cache directory for the API should be deleted so that the cache is rebuilt with the
 *     updated data or rules.
 * @langDe Das Modul *Tiles* kann für jede über ldproxy bereitgestellte API mit einem
 *     SQL-Feature-Provider oder mit einem MBTiles-Tile-Provider aktiviert werden. Es aktiviert die
 *     Ressourcen "Tilesets", "Tileset" und "Tile".
 *     <p>Das Modul basiert auf dem Entwurf von [OGC API - Tiles - Part 1:
 *     Core](https://github.com/opengeospatial/OGC-API-Tiles) und dem Entwurf von [OGC Two
 *     Dimensional Tile Matrix Set and Tile Set
 *     Metadata](https://docs.ogc.org/DRAFTS/17-083r4.html). Die Implementierung wird sich im Zuge
 *     der weiteren Standardisierung des Entwurfs noch ändern.
 *     <p>Die unterstützten Kachelformate sind:
 *     <p>- MVT (Mapbox Vector Tile) - PNG - WebP - JPEG - TIFF
 *     <p>Das Modul *Tile Matrix Sets* müssen aktiviert sein.
 *     <p>Der Tile-Cache liegt im ldproxy-Datenverzeichnis unter dem relativen Pfad
 *     `cache/tiles/{apiId}`. Wenn die Daten zu einer API oder Kachelkonfiguration geändert wurden,
 *     dann sollte das Cache-Verzeichnis für die API gelöscht werden, damit der Cache mit den
 *     aktualisierten Daten oder Regeln neu aufgebaut wird.
 * @example {@link de.ii.ogcapi.tiles.domain.TilesConfiguration}
 * @propertyTable {@link de.ii.ogcapi.tiles.domain.ImmutableTilesConfiguration}
 * @endpointTable {@link de.ii.ogcapi.tiles.infra.EndpointTileMultiCollection}, {@link
 *     de.ii.ogcapi.tiles.infra.EndpointTileSetMultiCollection}, {@link
 *     de.ii.ogcapi.tiles.infra.EndpointTileSetSingleCollection}, {@link
 *     de.ii.ogcapi.tiles.infra.EndpointTileSetsMultiCollection}, {@link
 *     de.ii.ogcapi.tiles.infra.EndpointTileSetsSingleCollection}, {@link
 *     de.ii.ogcapi.tiles.infra.EndpointTileSingleCollection}
 * @queryParameter {@link de.ii.ogcapi.tiles.domain.QueryParameterCollections}, {@link
 *     de.ii.ogcapi.tiles.domain.QueryParameterDatetimeTile}, {@link
 *     de.ii.ogcapi.tiles.domain.QueryParameterFTile}, {@link
 *     de.ii.ogcapi.tiles.domain.QueryParameterFTileSet}, {@link
 *     de.ii.ogcapi.tiles.domain.QueryParameterFTileSets}, {@link
 *     de.ii.ogcapi.tiles.domain.QueryParameterLimitTile},
 * @see de.ii.ogcapi.tiles.domain.SeedingOptions
 */
@Singleton
@AutoBind
public class TilesBuildingBlock implements ApiBuildingBlock {

  private static final Logger LOGGER = LoggerFactory.getLogger(TilesBuildingBlock.class);
  public static final int LIMIT_DEFAULT = 100000;
  public static final double MINIMUM_SIZE_IN_PIXEL = 0.5;
  public static final String DATASET_TILES = "__all__";

  private final ExtensionRegistry extensionRegistry;
  private final FeaturesCoreProviders providers;
  private final FeaturesQuery queryParser;
  private final SchemaInfo schemaInfo;
  private final TileMatrixSetRepository tileMatrixSetRepository;
  private final TilesProviders tilesProviders;
  private final EntityFactories entityFactories;

  @Inject
  public TilesBuildingBlock(
      ExtensionRegistry extensionRegistry,
      FeaturesQuery queryParser,
      FeaturesCoreProviders providers,
      SchemaInfo schemaInfo,
      TileMatrixSetRepository tileMatrixSetRepository,
      TilesProviders tilesProviders,
      EntityFactories entityFactories) {
    this.extensionRegistry = extensionRegistry;
    this.queryParser = queryParser;
    this.providers = providers;
    this.schemaInfo = schemaInfo;
    this.tileMatrixSetRepository = tileMatrixSetRepository;
    this.tilesProviders = tilesProviders;
    this.entityFactories = entityFactories;
  }

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {

    return new Builder()
        .enabled(false)
        .tileProvider(
            ImmutableTileProviderFeatures.builder()
                .tileEncodings(
                    extensionRegistry
                        .getExtensionsForType(TileFormatWithQuerySupportExtension.class)
                        .stream()
                        .filter(FormatExtension::isEnabledByDefault)
                        .map(format -> format.getMediaType().label())
                        .collect(ImmutableList.toImmutableList()))
                .center(ImmutableList.of(0.0, 0.0))
                .zoomLevels(
                    ImmutableMap.of(
                        "WebMercatorQuad", new ImmutableMinMax.Builder().min(0).max(23).build()))
                .zoomLevelsCache(ImmutableMap.of())
                .seeding(ImmutableMap.of())
                .limit(LIMIT_DEFAULT)
                .singleCollectionEnabled(true)
                .multiCollectionEnabled(true)
                .ignoreInvalidGeometries(false)
                .minimumSizeInPixel(MINIMUM_SIZE_IN_PIXEL)
                .build())
        .tileSetEncodings(
            extensionRegistry.getExtensionsForType(TileSetFormatExtension.class).stream()
                .filter(FormatExtension::isEnabledByDefault)
                .map(format -> format.getMediaType().label())
                .collect(ImmutableList.toImmutableList()))
        .cache(TilesConfiguration.TileCacheType.FILES)
        .style("DEFAULT")
        .build();
  }

  private MinMax getZoomLevels(
      OgcApiDataV2 apiData, TilesConfiguration config, String tileMatrixSetId) {
    if (Objects.nonNull(config.getZoomLevelsDerived()))
      return config.getZoomLevelsDerived().get(tileMatrixSetId);

    Optional<TileMatrixSet> tileMatrixSet =
        tileMatrixSetRepository
            .get(tileMatrixSetId)
            .filter(tms -> config.getTileMatrixSets().contains(tms.getId()));
    return tileMatrixSet
        .map(
            matrixSet ->
                new ImmutableMinMax.Builder()
                    .min(matrixSet.getMinLevel())
                    .max(matrixSet.getMaxLevel())
                    .build())
        .orElse(null);
  }

  private MinMax getZoomLevels(OgcApiDataV2 apiData, String tileMatrixSetId) {
    Optional<TileMatrixSet> tileMatrixSet =
        tileMatrixSetRepository
            .get(tileMatrixSetId)
            .filter(
                tms ->
                    apiData
                        .getExtension(TilesConfiguration.class)
                        .map(TilesConfiguration::getTileMatrixSets)
                        .filter(set -> set.contains(tms.getId()))
                        .isPresent());
    return tileMatrixSet
        .map(
            matrixSet ->
                new ImmutableMinMax.Builder()
                    .min(matrixSet.getMinLevel())
                    .max(matrixSet.getMaxLevel())
                    .build())
        .orElse(null);
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    // since building block / capability components are currently always enabled,
    // we need to test, if the TILES and TILE MATRIX SETS module are enabled for the API and stop,
    // if not
    OgcApiDataV2 apiData = api.getData();

    // TODO: enable in hydrator???
    /*if (!apiData
        .getExtension(TileMatrixSetsConfiguration.class)
        .map(ExtensionConfiguration::isEnabled)
        .orElse(false)) {
      return ValidationResult.of();
    }*/

    Optional<TilesConfiguration> tilesConfiguration =
        apiData.getExtension(TilesConfiguration.class).filter(ExtensionConfiguration::isEnabled);

    if (tilesConfiguration.isEmpty()) {
      return ValidationResult.of();
    }

    // TODO
    Optional<TileProviderFeaturesData> tileProviderFeaturesData = getTileProviderData(apiData);
    if (tileProviderFeaturesData.isPresent()) {
      entityFactories
          .get(TileProviderFeaturesData.class)
          .createInstance(tileProviderFeaturesData.get())
          .join();
      LogContext.put(LogContext.CONTEXT.SERVICE, apiData.getId());
    }

    try {
      SQLiteJDBCLoader.initialize();
    } catch (Exception e) {
      return ImmutableValidationResult.builder()
          .mode(apiValidation)
          .addStrictErrors(MessageFormat.format("Could not load SQLite: {}", e.getMessage()))
          .build();
    }

    providers
        .getFeatureProvider(apiData)
        .ifPresent(
            provider -> provider.getFeatureChangeHandler().addListener(onFeatureChange(api)));

    if (apiValidation == MODE.NONE) {
      return ValidationResult.of();
    }

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    for (Map.Entry<String, TilesConfiguration> entry :
        apiData.getCollections().entrySet().stream()
            .filter(
                entry ->
                    entry.getValue().getEnabled()
                        && entry.getValue().getExtension(TilesConfiguration.class).isPresent())
            .map(
                entry ->
                    new AbstractMap.SimpleImmutableEntry<>(
                        entry.getKey(),
                        entry.getValue().getExtension(TilesConfiguration.class).get()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            .entrySet()) {
      String collectionId = entry.getKey();
      TilesConfiguration config = entry.getValue();

      Optional<FeatureSchema> schema =
          providers.getFeatureSchema(apiData, apiData.getCollections().get(collectionId));
      List<String> featureProperties =
          schema.isPresent()
              ? schemaInfo.getPropertyNames(schema.get(), false, false)
              : ImmutableList.of();

      List<String> formatLabels =
          extensionRegistry.getExtensionsForType(TileFormatExtension.class).stream()
              .filter(formatExtension -> formatExtension.isEnabledForApi(apiData))
              .map(format -> format.getMediaType().label())
              .collect(Collectors.toUnmodifiableList());
      List<String> tileEncodings = config.getTileEncodingsDerived();
      if (Objects.isNull(tileEncodings)) {
        builder.addStrictErrors(
            MessageFormat.format(
                "No tile encoding has been specified in the TILES module configuration of collection ''{0}''.",
                collectionId));
      } else {
        for (String encoding : config.getTileEncodingsDerived()) {
          if (!formatLabels.contains(encoding)) {
            builder.addStrictErrors(
                MessageFormat.format(
                    "The tile encoding ''{0}'' is specified in the TILES module configuration of collection ''{1}'', but the format does not exist.",
                    encoding, collectionId));
          }
        }
      }

      formatLabels =
          extensionRegistry.getExtensionsForType(TileSetFormatExtension.class).stream()
              .filter(formatExtension -> formatExtension.isEnabledForApi(apiData))
              .map(format -> format.getMediaType().label())
              .collect(Collectors.toUnmodifiableList());
      for (String encoding : config.getTileSetEncodings()) {
        if (!formatLabels.contains(encoding)) {
          builder.addStrictErrors(
              MessageFormat.format(
                  "The tile set encoding ''{0}'' is specified in the TILES module configuration of collection ''{1}'', but the format does not exist.",
                  encoding, collectionId));
        }
      }

      List<Double> center = config.getCenterDerived();
      if (center.size() != 0 && center.size() != 2)
        builder.addStrictErrors(
            MessageFormat.format(
                "The center has been specified in the TILES module configuration of collection ''{1}'', but the array length is ''{0}'', not 2.",
                center.size(), collectionId));

      Map<String, MinMax> zoomLevels = config.getZoomLevelsDerived();
      for (Map.Entry<String, MinMax> entry2 : zoomLevels.entrySet()) {
        String tileMatrixSetId = entry2.getKey();
        Optional<TileMatrixSet> tileMatrixSet =
            tileMatrixSetRepository
                .get(tileMatrixSetId)
                .filter(tms -> config.getTileMatrixSets().contains(tms.getId()));
        if (tileMatrixSet.isEmpty()) {
          builder.addStrictErrors(
              MessageFormat.format(
                  "The configuration in the TILES module of collection ''{0}'' references a tile matrix set ''{1}'' that is not available in this API.",
                  collectionId, tileMatrixSetId));
        } else {
          if (tileMatrixSet.get().getMinLevel() > entry2.getValue().getMin()) {
            builder.addStrictErrors(
                MessageFormat.format(
                    "The configuration in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level of the tile matrix set is ''{3}''.",
                    collectionId,
                    tileMatrixSetId,
                    entry2.getValue().getMin(),
                    tileMatrixSet.get().getMinLevel()));
          }
          if (tileMatrixSet.get().getMaxLevel() < entry2.getValue().getMax()) {
            builder.addStrictErrors(
                MessageFormat.format(
                    "The configuration in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level of the tile matrix set is ''{3}''.",
                    collectionId,
                    tileMatrixSetId,
                    entry2.getValue().getMax(),
                    tileMatrixSet.get().getMaxLevel()));
          }
          if (entry2.getValue().getDefault().isPresent()) {
            Integer defaultLevel = entry2.getValue().getDefault().get();
            if (defaultLevel < entry2.getValue().getMin()
                || defaultLevel > entry2.getValue().getMax()) {
              builder.addStrictErrors(
                  MessageFormat.format(
                      "The configuration in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' specifies a default level ''{2}'' that is outside of the range [ ''{3}'' : ''{4}'' ].",
                      tileMatrixSetId,
                      defaultLevel,
                      entry2.getValue().getMin(),
                      entry2.getValue().getMax()));
            }
          }
        }
      }

      if (config.getTileProvider() instanceof TileProviderFeatures) {

        Map<String, MinMax> zoomLevelsCache = config.getZoomLevelsCacheDerived();
        if (Objects.nonNull(zoomLevelsCache)) {
          for (Map.Entry<String, MinMax> entry2 : zoomLevelsCache.entrySet()) {
            String tileMatrixSetId = entry2.getKey();
            MinMax zoomLevelsTms = getZoomLevels(apiData, tileMatrixSetId);
            if (Objects.isNull(zoomLevelsTms)) {
              builder.addStrictErrors(
                  MessageFormat.format(
                      "The cache in the TILES module of collection ''{0}'' references a tile matrix set ''{1}'' that is not configured for this API.",
                      collectionId, tileMatrixSetId));
            } else {
              if (zoomLevelsTms.getMin() > entry2.getValue().getMin()) {
                builder.addStrictErrors(
                    MessageFormat.format(
                        "The cache in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level is ''{3}''.",
                        collectionId,
                        tileMatrixSetId,
                        entry2.getValue().getMin(),
                        zoomLevelsTms.getMin()));
              }
              if (zoomLevelsTms.getMax() < entry2.getValue().getMax()) {
                builder.addStrictErrors(
                    MessageFormat.format(
                        "The cache in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level is ''{3}''.",
                        collectionId,
                        tileMatrixSetId,
                        entry2.getValue().getMax(),
                        zoomLevelsTms.getMax()));
              }
            }
          }
        }

        Map<String, MinMax> seeding = config.getSeedingDerived();
        if (Objects.nonNull(seeding)) {
          for (Map.Entry<String, MinMax> entry2 : seeding.entrySet()) {
            String tileMatrixSetId = entry2.getKey();
            MinMax zoomLevelsTms = getZoomLevels(apiData, tileMatrixSetId);
            if (Objects.isNull(zoomLevelsTms)) {
              builder.addStrictErrors(
                  MessageFormat.format(
                      "The seeding in the TILES module of collection ''{0}'' references a tile matrix set ''{1}'' that is not configured for this API.",
                      collectionId, tileMatrixSetId));
            } else {
              if (zoomLevelsTms.getMin() > entry2.getValue().getMin()) {
                builder.addStrictErrors(
                    MessageFormat.format(
                        "The seeding in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level is ''{3}''.",
                        collectionId,
                        tileMatrixSetId,
                        entry2.getValue().getMin(),
                        zoomLevelsTms.getMin()));
              }
              if (zoomLevelsTms.getMax() < entry2.getValue().getMax()) {
                builder.addStrictErrors(
                    MessageFormat.format(
                        "The seeding in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level is ''{3}''.",
                        collectionId,
                        tileMatrixSetId,
                        entry2.getValue().getMax(),
                        zoomLevelsTms.getMax()));
              }
            }
          }
        }

        final Integer limit = Objects.requireNonNullElse(config.getLimitDerived(), 0);
        if (limit < 1) {
          builder.addStrictErrors(
              MessageFormat.format(
                  "The feature limit in the TILES module must be a positive integer. Found in collection ''{1}'': {0}.",
                  limit, collectionId));
        }

        final Map<String, List<LevelFilter>> filters = config.getFiltersDerived();
        if (Objects.nonNull(filters)) {
          for (Map.Entry<String, List<LevelFilter>> entry2 : filters.entrySet()) {
            String tileMatrixSetId = entry2.getKey();
            MinMax zoomLevelsCfg = getZoomLevels(apiData, config, tileMatrixSetId);
            if (Objects.isNull(zoomLevelsCfg)) {
              builder.addStrictErrors(
                  MessageFormat.format(
                      "The filters in the TILES module of collection ''{0}'' references a tile matrix set ''{0}'' that is not configured for this API.",
                      collectionId, tileMatrixSetId));
            } else {
              for (LevelFilter filter : entry2.getValue()) {
                if (zoomLevelsCfg.getMin() > filter.getMin()) {
                  builder.addStrictErrors(
                      MessageFormat.format(
                          "A filter in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level is ''{3}''.",
                          collectionId, tileMatrixSetId, filter.getMin(), zoomLevelsCfg.getMin()));
                }
                if (zoomLevelsCfg.getMax() < filter.getMax()) {
                  builder.addStrictErrors(
                      MessageFormat.format(
                          "A filter in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level is ''{3}''.",
                          collectionId, tileMatrixSetId, filter.getMax(), zoomLevelsCfg.getMax()));
                }
                // try to convert the filter to CQL2-text
                String expression = filter.getFilter();
                FeatureTypeConfigurationOgcApi collectionData =
                    apiData.getCollections().get(collectionId);
                final Map<String, String> filterableFields =
                    queryParser.getFilterableFields(apiData, collectionData);
                final Map<String, String> queryableTypes =
                    queryParser.getQueryableTypes(apiData, collectionData);
                try {
                  queryParser.getFilterFromQuery(
                      ImmutableMap.of("filter", expression),
                      filterableFields,
                      ImmutableSet.of("filter"),
                      queryableTypes,
                      Cql.Format.TEXT);
                } catch (Exception e) {
                  builder.addErrors(
                      MessageFormat.format(
                          "A filter ''{0}'' in the TILES module of collection ''{1}'' for tile matrix set ''{2}'' is invalid. Reason: {3}",
                          expression, collectionId, tileMatrixSetId, e.getMessage()));
                }
              }
            }
          }
        }

        final Map<String, List<LevelTransformation>> rules = config.getRulesDerived();
        if (Objects.nonNull(rules)) {
          for (Map.Entry<String, List<LevelTransformation>> entry2 : rules.entrySet()) {
            String tileMatrixSetId = entry2.getKey();
            MinMax zoomLevelsCfg = getZoomLevels(apiData, config, tileMatrixSetId);
            if (Objects.isNull(zoomLevelsCfg)) {
              builder.addStrictErrors(
                  MessageFormat.format(
                      "The rules in the TILES module of collection ''{0}'' references a tile matrix set ''{0}'' that is not configured for this API.",
                      collectionId, tileMatrixSetId));
            } else {
              for (LevelTransformation rule : entry2.getValue()) {
                if (zoomLevelsCfg.getMin() > rule.getMin()) {
                  builder.addStrictErrors(
                      MessageFormat.format(
                          "A rule in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level is ''{3}''.",
                          collectionId, tileMatrixSetId, rule.getMin(), zoomLevelsCfg.getMin()));
                }
                if (zoomLevelsCfg.getMax() < rule.getMax()) {
                  builder.addStrictErrors(
                      MessageFormat.format(
                          "A rule in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level is ''{3}''.",
                          collectionId, tileMatrixSetId, rule.getMax(), zoomLevelsCfg.getMax()));
                }
                for (String property : rule.getProperties()) {
                  if (!featureProperties.contains(property)) {
                    builder.addErrors(
                        MessageFormat.format(
                            "A rule in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' references property ''{2}'' that is not part of the feature schema.",
                            collectionId, tileMatrixSetId, property));
                  }
                }
                for (String property : rule.getGroupBy()) {
                  if (!featureProperties.contains(property)) {
                    builder.addErrors(
                        MessageFormat.format(
                            "A rule in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' references group-by property ''{2}'' that is not part of the feature schema.",
                            collectionId, tileMatrixSetId, property));
                  }
                }
              }
            }
          }
        }
      }
    }

    return builder.build();
  }

  private Optional<TileProviderFeaturesData> getTileProviderData(OgcApiDataV2 apiData) {
    Optional<FeaturesCoreConfiguration> featuresCore =
        apiData.getExtension(FeaturesCoreConfiguration.class);

    Map<String, FeatureTypeConfigurationOgcApi> collections =
        apiData.getCollections().entrySet().stream()
            .filter(
                entry ->
                    entry
                        .getValue()
                        .getExtension(TilesConfiguration.class)
                        .filter(TilesConfiguration::isEnabled)
                        .filter(TilesConfiguration::isSingleCollectionEnabled)
                        .isPresent())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // TODO: seeding*, zoomLevelCache
    return apiData
        .getExtension(TilesConfiguration.class)
        .filter(ExtensionConfiguration::isEnabled)
        .map(
            tilesConfiguration ->
                new ImmutableTileProviderFeaturesData.Builder()
                    .id(String.format("%s-tiles", apiData.getId()))
                    .addAllCaches(getCaches(tilesConfiguration))
                    .layerDefaults(
                        new ImmutableLayerOptionsFeaturesDefault.Builder()
                            .featureProvider(
                                featuresCore.flatMap(FeaturesCoreConfiguration::getFeatureProvider))
                            .putAllLevels(tilesConfiguration.getZoomLevelsDerived())
                            .putAllTransformations(tilesConfiguration.getRulesDerived())
                            .featureLimit(tilesConfiguration.getLimitDerived())
                            .minimumSizeInPixel(tilesConfiguration.getMinimumSizeInPixelDerived())
                            .ignoreInvalidGeometries(
                                tilesConfiguration.isIgnoreInvalidGeometriesDerived())
                            .build())
                    .putAllLayers(
                        tilesConfiguration.isMultiCollectionEnabled()
                            ? Map.of(
                                DATASET_TILES,
                                new ImmutableLayerOptionsFeatures.Builder()
                                    .id(DATASET_TILES)
                                    .addCombine(COMBINE_ALL)
                                    .putAllLevels(tilesConfiguration.getZoomLevelsDerived())
                                    .build())
                            : Map.of())
                    .putAllLayers(
                        collections.entrySet().stream()
                            .map(
                                entry ->
                                    new SimpleImmutableEntry<>(
                                        entry.getKey(),
                                        entry
                                            .getValue()
                                            .getExtension(TilesConfiguration.class)
                                            .get()))
                            .map(
                                entry ->
                                    new SimpleImmutableEntry<>(
                                        entry.getKey(),
                                        getLayer(entry.getKey(), entry.getValue(), collections)))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .build());
  }

  private static List<Cache> getCaches(TilesConfiguration tilesConfiguration) {

    if (Objects.equals(tilesConfiguration.getCache(), TileCacheType.FILES)) {
      return List.of(
          new ImmutableCache.Builder()
              .type(Type.DYNAMIC)
              .storage(Storage.FILES)
              .putAllLevels(
                  tilesConfiguration.getZoomLevelsDerived()) // TODO: per collection/layer?
              .build());
    } else if (Objects.equals(tilesConfiguration.getCache(), TileCacheType.MBTILES)) {
      return List.of(
          new ImmutableCache.Builder()
              .type(Type.DYNAMIC)
              .storage(Storage.MBTILES)
              .putAllLevels(
                  tilesConfiguration.getZoomLevelsDerived()) // TODO: per collection/layer?
              .build());
    }

    return List.of();
  }

  private static LayerOptionsFeatures getLayer(
      String id, TilesConfiguration cfg, Map<String, FeatureTypeConfigurationOgcApi> collections) {
    return new ImmutableLayerOptionsFeatures.Builder()
        .id(id)
        .featureProvider(
            collections
                .get(id)
                .getExtension(FeaturesCoreConfiguration.class)
                .flatMap(FeaturesCoreConfiguration::getFeatureProvider))
        .featureType(
            collections
                .get(id)
                .getExtension(FeaturesCoreConfiguration.class)
                .flatMap(FeaturesCoreConfiguration::getFeatureType))
        .putAllLevels(cfg.getZoomLevelsDerived())
        .putAllTransformations(cfg.getRulesDerived())
        .putAllFilters(cfg.getFiltersDerived())
        .featureLimit(cfg.getLimitDerived())
        .minimumSizeInPixel(cfg.getMinimumSizeInPixelDerived())
        .ignoreInvalidGeometries(cfg.isIgnoreInvalidGeometriesDerived())
        .build();
  }

  private FeatureChangeListener onFeatureChange(OgcApi api) {
    return change -> {
      String collectionId =
          getCollectionId(api.getData().getCollections().values(), change.getFeatureType());
      switch (change.getAction()) {
        case CREATE:
        case UPDATE:
          change
              .getBoundingBox()
              .ifPresent(
                  bbox -> {
                    try {
                      tilesProviders.deleteTiles(
                          api, Optional.of(collectionId), Optional.empty(), Optional.of(bbox));
                    } catch (Exception e) {
                      if (LOGGER.isErrorEnabled()) {
                        LOGGER.error(
                            "Error while deleting tiles from the tile cache after a feature change.",
                            e);
                      }
                    }
                  });
          break;
        case DELETE:
          // TODO we would need the extent of the deleted feature to update the cache
          break;
      }
    };
  }

  // TODO centralize
  private String getCollectionId(
      Collection<FeatureTypeConfigurationOgcApi> collections, String featureType) {
    return collections.stream()
        .map(
            collection ->
                collection
                    .getExtension(FeaturesCoreConfiguration.class)
                    .flatMap(FeaturesCoreConfiguration::getFeatureType))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(ft -> Objects.equals(ft, featureType))
        .findFirst()
        .orElse(featureType);
  }
}
