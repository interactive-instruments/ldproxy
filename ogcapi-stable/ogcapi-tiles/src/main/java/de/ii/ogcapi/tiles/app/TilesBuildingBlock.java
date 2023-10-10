/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.ExternalDocumentation;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SpecificationMaturity;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetsConfiguration;
import de.ii.ogcapi.tiles.domain.ImmutableTilesConfiguration.Builder;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileProviderFeatures;
import de.ii.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesMigrationV4;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.docs.DocDefs;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.docs.DocVar;
import de.ii.xtraplatform.entities.domain.EntityFactories;
import de.ii.xtraplatform.entities.domain.ImmutableValidationResult;
import de.ii.xtraplatform.entities.domain.ValidationResult;
import de.ii.xtraplatform.entities.domain.ValidationResult.MODE;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.tiles.domain.ImmutableMinMax;
import de.ii.xtraplatform.tiles.domain.LevelFilter;
import de.ii.xtraplatform.tiles.domain.LevelTransformation;
import de.ii.xtraplatform.tiles.domain.MinMax;
import de.ii.xtraplatform.tiles.domain.TileMatrixSet;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetRepository;
import de.ii.xtraplatform.tiles.domain.TileProviderData;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @title Tiles
 * @langEn Publish geographic data as tiles.
 * @langDe Veröffentlichen von Geodaten als Kacheln.
 * @scopeEn This building block supports tiles derived from feature data or tiles that are provided
 *     by an external source.
 *     <p>The supported tile formats are:
 *     <p><code>
 * - MVT (Mapbox Vector Tile)
 * - PNG
 * - WebP
 * - JPEG
 * - TIFF
 *     </code>
 *     <p>For tiles that are derived from feature data, only Mapbox Vector Tiles are supported as a
 *     file format.
 *     <p>All tiles of an API are sourced from a single tile provider.
 * @scopeDe Dieses Modul unterstützt Kacheln, die aus Features abgeleitet sind, oder Kacheln, die
 *     von einer externen Quelle bereitgestellt werden.
 *     <p>Die unterstützten Kachelformate sind:
 *     <p><code>
 * - MVT (Mapbox Vector Tile)
 * - PNG
 * - WebP
 * - JPEG
 * - TIFF
 *     </code>
 *     <p>Für Kacheln, die aus Features abgeleitet werden, wird nur Mapbox Vector Tiles als
 *     Kachelformat unterstützt.
 *     <p>Alle Kacheln einer API kommen vom selben Tile-Provider.
 * @conformanceEn The module implements the conformance classes "Core", "TileSet", "TileSets List",
 *     "Dataset TileSets", "GeoData TileSets", "Collections Selection", "DateTime", "OpenAPI
 *     Specification 3.0 API definition", "Mapbox Vector Tiles", "PNG", "JPEG", and "TIFF" of the
 *     [OGC API - Tiles - Part 1: Core 1.0 Standard](https://docs.ogc.org/is/20-057/20-057.html) and
 *     the conformance classes "TileSetMetadata", "TileMatrixSetLimits", and
 *     "JSONTileMatrixSetLimits" of the [OGC Two Dimensional Tile Matrix Set and Tile Set Metadata
 *     2.0 Standard](https://docs.ogc.org/is/17-083r4/17-083r4.html).
 * @conformanceDe Das Modul implementiert die Konformitätsklassen "Core", "TileSet", "TileSets
 *     List", * "Dataset TileSets", "GeoData TileSets", "Collections Selection", "DateTime",
 *     "OpenAPI * Specification 3.0 API definition", "Mapbox Vector Tiles", "PNG", "JPEG" und "TIFF"
 *     des Standards [OGC API - Tiles - Part 1: Core
 *     1.0](https://docs.ogc.org/is/20-057/20-057.html) und die Konformitätsklassen
 *     "TileSetMetadata", "TileMatrixSetLimits" und "JSONTileMatrixSetLimits" des Standards [OGC Two
 *     * Dimensional Tile Matrix Set and Tile Set Metadata
 *     2.0](https://docs.ogc.org/is/17-083r4/17-083r4.html).
 * @cfgPropertiesAdditionalEn ### Tile Provider
 *     <p>{@docVar:provider}
 *     <p>#### Features
 *     <p>{@docVar:providerFeatures}
 *     <p>{@docTable:providerFeaturesProperties}
 *     <p>#### MbTiles
 *     <p>{@docVar:providerMbTiles}
 *     <p>{@docTable:providerMbTilesProperties}
 *     <p>#### TileServer
 *     <p>{@docVar:providerTileServer}
 *     <p>{@docTable:providerTileServerProperties}
 * @cfgPropertiesAdditionalDe ### Tile Provider
 *     <p>{@docVar:provider}
 *     <p>#### Features
 *     <p>{@docVar:providerFeatures}
 *     <p>{@docTable:providerFeaturesProperties}
 *     <p>#### MbTiles
 *     <p>{@docVar:providerMbTiles}
 *     <p>{@docTable:providerMbTilesProperties}
 *     <p>#### TileServer
 *     <p>{@docVar:providerTileServer}
 *     <p>{@docTable:providerTileServerProperties}
 * @ref:cfg {@link de.ii.ogcapi.tiles.domain.TilesConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.tiles.domain.ImmutableTilesConfiguration}
 * @ref:endpoints {@link de.ii.ogcapi.tiles.infra.EndpointTileSetsMultiCollection}, {@link
 *     de.ii.ogcapi.tiles.infra.EndpointTileSetMultiCollection}, {@link
 *     de.ii.ogcapi.tiles.infra.EndpointTileMultiCollection}, {@link
 *     de.ii.ogcapi.tiles.infra.EndpointTileSetsSingleCollection}, {@link
 *     de.ii.ogcapi.tiles.infra.EndpointTileSetSingleCollection}, {@link
 *     de.ii.ogcapi.tiles.infra.EndpointTileSingleCollection}
 * @ref:queryParameters {@link de.ii.ogcapi.tiles.domain.QueryParameterCollections}, {@link
 *     de.ii.ogcapi.tiles.domain.QueryParameterDatetimeTile}, {@link
 *     de.ii.ogcapi.tiles.domain.QueryParameterFTile}, {@link
 *     de.ii.ogcapi.tiles.domain.QueryParameterFTileSet}, {@link
 *     de.ii.ogcapi.tiles.domain.QueryParameterFTileSets}, {@link
 *     de.ii.ogcapi.tiles.domain.QueryParameterLimitTile}
 * @ref:pathParameters {@link de.ii.ogcapi.tiles.domain.PathParameterCollectionIdTiles}, {@link
 *     de.ii.ogcapi.tiles.domain.PathParameterTileMatrixSetId}, {@link
 *     de.ii.ogcapi.tiles.domain.PathParameterTileMatrix}, {@link
 *     de.ii.ogcapi.tiles.domain.PathParameterTileRow}, {@link
 *     de.ii.ogcapi.tiles.domain.PathParameterTileCol}
 * @ref:provider {@link de.ii.ogcapi.tiles.domain.TileProvider}
 * @ref:providerFeatures {@link de.ii.ogcapi.tiles.domain.TileProviderFeatures}
 * @ref:providerFeaturesProperties {@link de.ii.ogcapi.tiles.domain.ImmutableTileProviderFeatures}
 * @ref:providerMbTiles {@link de.ii.ogcapi.tiles.domain.TileProviderMbtiles}
 * @ref:providerMbTilesProperties {@link de.ii.ogcapi.tiles.domain.ImmutableTileProviderMbtiles}
 * @ref:providerTileServer {@link de.ii.ogcapi.tiles.domain.TileProviderTileServer}
 * @ref:providerTileServerProperties {@link
 *     de.ii.ogcapi.tiles.domain.ImmutableTileProviderTileServer}
 */
@DocDefs(
    tables = {
      @DocTable(
          name = "providerFeaturesProperties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:providerFeaturesProperties}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "providerMbTilesProperties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:providerMbTilesProperties}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "providerTileServerProperties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:providerTileServerProperties}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
    },
    vars = {
      @DocVar(
          name = "provider",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:provider}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "providerFeatures",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:providerFeatures}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "providerMbTiles",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:providerMbTiles}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "providerTileServer",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:providerTileServer}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
    })
@Singleton
@AutoBind
public class TilesBuildingBlock implements ApiBuildingBlock {

  public static final Optional<SpecificationMaturity> MATURITY =
      Optional.of(SpecificationMaturity.STABLE_OGC);
  public static final Optional<ExternalDocumentation> SPEC =
      Optional.of(
          ExternalDocumentation.of(
              "https://docs.ogc.org/is/20-057/20-057.html", "OGC API - Tiles - Part 1: Core"));
  private static final Logger LOGGER = LoggerFactory.getLogger(TilesBuildingBlock.class);
  public static final int LIMIT_DEFAULT = 100000;
  public static final double MINIMUM_SIZE_IN_PIXEL = 0.5;
  public static final String DATASET_TILES = "__all__";
  private static final String TILES_DIR_NAME = "tiles";

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
        .collectionTiles(true)
        .datasetTiles(true)
        .tileSetEncodings(
            extensionRegistry.getExtensionsForType(TileSetFormatExtension.class).stream()
                .filter(FormatExtension::isEnabledByDefault)
                .map(format -> format.getMediaType().label())
                .collect(ImmutableList.toImmutableList()))
        .style("DEFAULT")
        .build();
  }

  @Override
  public <T extends ExtensionConfiguration> T hydrateConfiguration(T cfg) {
    if (cfg instanceof TilesConfiguration) {
      TilesConfiguration config = (TilesConfiguration) cfg;
      Map<String, List<PropertyTransformation>> transformations =
          config.extendWithFlattenIfMissing();

      if (Objects.equals(transformations, config.getTransformations())) {
        return (T) config;
      }

      return (T) new Builder().from(config).transformations(transformations).build();
    }

    return cfg;
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
  public int getStartupPriority() {
    return 10;
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    // since building block / capability components are currently always enabled,
    // we need to test, if the TILES and TILE MATRIX SETS module are enabled for the API and stop,
    // if not
    OgcApiDataV2 apiData = api.getData();

    // TODO currently not yet really required, since Tiles always enables Tile Matrix Sets
    if (!apiData
        .getExtension(TileMatrixSetsConfiguration.class)
        .map(ExtensionConfiguration::isEnabled)
        .orElse(false)) {
      return ValidationResult.of();
    }

    Optional<TilesConfiguration> tilesConfiguration =
        apiData.getExtension(TilesConfiguration.class).filter(ExtensionConfiguration::isEnabled);

    if (tilesConfiguration.isEmpty()) {
      return ValidationResult.of();
    }
    if (!tilesProviders.hasTileProvider(apiData)) {
      if (Objects.nonNull(tilesConfiguration.get().getTileProviderId())) {
        throw new IllegalStateException(
            String.format(
                "Tile provider with id '%s' not found.",
                tilesConfiguration.get().getTileProviderId()));

      } else {
        boolean tilesIdExists =
            entityFactories.getAll("providers").stream()
                .anyMatch(
                    entityFactory ->
                        TileProviderData.class.isAssignableFrom(entityFactory.dataClass())
                            && entityFactory
                                .instance(TilesProviders.toTilesId(apiData.getId()))
                                .isPresent());
        if (tilesIdExists) {
          throw new IllegalStateException(
              String.format(
                  "Tile provider with id '%s' not found.",
                  TilesProviders.toTilesId(apiData.getId())));
        }

        TilesMigrationV4 tilesMigrationV4 = new TilesMigrationV4(null);
        Optional<Tuple<Class<? extends TileProviderData>, ? extends TileProviderData>>
            tileProviderData = tilesMigrationV4.getTileProviderData(apiData);
        if (tileProviderData.isPresent()) {
          entityFactories
              .get(tileProviderData.get().first())
              .createInstance(tileProviderData.get().second())
              .join();
          LogContext.put(LogContext.CONTEXT.SERVICE, apiData.getId());
        }
      }
    }

    if (apiValidation == MODE.NONE) {
      return ValidationResult.of();
    }

    return validate(apiData, apiValidation);
  }

  public ValidationResult validate(OgcApiDataV2 apiData, MODE apiValidation) {

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
            .filter(entry -> entry.getValue().isEnabled())
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
      Set<String> tileEncodings =
          tilesProviders
              .getTilesetMetadataOrThrow(apiData, apiData.getCollectionData(collectionId))
              .getEncodings();
      if (Objects.isNull(tileEncodings)) {
        builder.addStrictErrors(
            MessageFormat.format(
                "No tile encoding has been specified in the TILES module configuration of collection ''{0}''.",
                collectionId));
      } else {
        for (String encoding : tileEncodings) {
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
                // try to parse the filter as CQL2 Text
                String expression = filter.getFilter();
                FeatureTypeConfigurationOgcApi collectionData =
                    apiData.getCollections().get(collectionId);
                if (collectionData != null) {
                  Map<String, FeatureSchema> queryables =
                      collectionData
                          .getExtension(QueryablesConfiguration.class)
                          .map(cfg -> cfg.getQueryables(apiData, collectionData, providers))
                          .orElse(ImmutableMap.of());
                  queryParser
                      .validateFilter(expression, Cql.Format.TEXT, OgcCrs.CRS84, queryables)
                      .ifPresent(
                          error ->
                              builder.addErrors(
                                  MessageFormat.format(
                                      "A filter ''{0}'' in the TILES module of collection ''{1}'' for tile matrix set ''{2}'' is invalid. Reason: {3}",
                                      expression, collectionId, tileMatrixSetId, error)));
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
}
