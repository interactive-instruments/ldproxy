/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import de.ii.ogcapi.tiles.domain.*;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import de.ii.ogcapi.tiles.domain.ImmutableTilesConfiguration.Builder;
import de.ii.ogcapi.tiles.infra.*;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.sqlite.SQLiteJDBCLoader;

/**
 * # Vector Tiles (TILES)
 * @lang_en The "Tiles" module can be activated for any API provided by ldproxy with an SQL
 * feature provider or with an MBTiles tile provider. It enables the "Tilesets",
 * "Tileset", "Tile", "Tile Matrix Sets" and "Tile Matrix Set" resources.
 *
 * The module is based on the draft of
 * [OGC API - Tiles - Part 1: Core](https://github.com/opengeospatial/OGC-API-Tiles)
 * and the draft of [OGC Two Dimensional Tile Matrix Set and Tile Set Metadata](https://docs.ogc.org/DRAFTS/17-083r3.html).
 * The implementation will change as the draft is further standardized.
 *
 * The supported tile formats are:
 *
 * - MVT (Mapbox Vector Tile)
 * - PNG
 * - WebP
 * - JPEG
 * - TIFF
 *
 * As preconfigured tiling schemes are available:
 *
 * - [WebMercatorQuad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#62)
 * - [WorldCRS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#63)
 * - [WorldMercatorWGS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#64)
 * - AdV_25832 (Tiling scheme of the AdV for Germany)
 * - EU_25832 (Tiling scheme of the BKG, based on AdV_25832, extended to Europe)
 * - gdi_de_25832 (tile scheme recommended by the GDI-DE)
 *
 * Additional tile schemas can be configured as JSON files according to the current draft OGC standard [Two Dimensional Tile Matrix Set and Tile Set Metadata 2.0](https://docs.ogc.org/DRAFTS/17-083r3.html) in the data directory at `api-resources/tile-matrix-sets/{tileMatrixSetId}.json`.
 *
 * The tile cache is located in the ldproxy data directory under the relative path `cache/tiles/{apiId}`. If the data for an API or tile configuration has been changed, then the cache directory for the API should be deleted so that the cache is rebuilt with the updated data or rules.
 * @lang_de Das Modul "Tiles" kann für jede über ldproxy bereitgestellte API mit einem
 * SQL-Feature-Provider oder mit einem MBTiles-Tile-Provider aktiviert werden.
 * Es aktiviert die Ressourcen "Tilesets", "Tileset", "Tile", "Tile Matrix Sets"
 * und "Tile Matrix Set".
 *
 * Das Modul basiert auf dem Entwurf von [OGC API - Tiles - Part 1: Core](https://github.com/opengeospatial/OGC-API-Tiles)
 * und dem Entwurf von [OGC Two Dimensional Tile Matrix Set and Tile Set
 * Metadata](https://docs.ogc.org/DRAFTS/17-083r3.html). Die Implementierung wird
 * sich im Zuge der weiteren Standardisierung des Entwurfs noch ändern.
 *
 * Die unterstützten Kachelformate sind:

 * - MVT (Mapbox Vector Tile)
 * - PNG
 * - WebP
 * - JPEG
 * - TIFF
 *
 * Als vorkonfigurierte Kachelschemas stehen zur Verfügung:
 *
 * - [WebMercatorQuad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#62)
 * - [WorldCRS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#63)
 * - [WorldMercatorWGS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#64)
 * - AdV_25832 (Kachelschema der AdV für Deutschland)
 * - EU_25832 (Kachelschema des BKG, basierend auf AdV_25832, erweitert auf Europa)
 * - gdi_de_25832 (von der GDI-DE empfohlenes Kachelschema)
 *
 * Weitere Kachelschemas können als JSON-Datei gemäß dem aktuellen Entwurf für den OGC-Standard [Two Dimensional Tile Matrix Set and Tile Set Metadata 2.0](https://docs.ogc.org/DRAFTS/17-083r3.html) im Datenverzeichnis unter `api-resources/tile-matrix-sets/{tileMatrixSetId}.json` konfiguriert werden.
 *
 * Der Tile-Cache liegt im ldproxy-Datenverzeichnis unter dem relativen Pfad `cache/tiles/{apiId}`. Wenn die Daten zu einer API oder Kachelkonfiguration geändert wurden, dann sollte das Cache-Verzeichnis für die API gelöscht werden, damit der Cache mit den aktualisierten Daten oder Regeln neu aufgebaut wird.
 * @see EndpointTileMatrixSets
 * @see EndpointTileMultiCollection
 * @see EndpointTileSetMultiCollection
 * @see EndpointTileSetSingleCollection
 * @see EndpointTileSetsMultiCollection
 * @see EndpointTileSetsSingleCollection
 * @see EndpointTileSingleCollection
 * @see SeedingOptions
 *
 */
@Singleton
@AutoBind
public class TilesBuildingBlock implements ApiBuildingBlock {

    public static final int LIMIT_DEFAULT = 100000;
    public static final double MINIMUM_SIZE_IN_PIXEL = 0.5;
    public static final String DATASET_TILES = "__all__";

    private final ExtensionRegistry extensionRegistry;
    private final FeaturesCoreProviders providers;
    private final FeaturesQuery queryParser;
    private final SchemaInfo schemaInfo;
    private final TileMatrixSetRepository tileMatrixSetRepository;

    @Inject
    public TilesBuildingBlock(ExtensionRegistry extensionRegistry, FeaturesQuery queryParser,
                           FeaturesCoreProviders providers, SchemaInfo schemaInfo,
                           TileMatrixSetRepository tileMatrixSetRepository) {
        this.extensionRegistry = extensionRegistry;
        this.queryParser = queryParser;
        this.providers = providers;
        this.schemaInfo = schemaInfo;
        this.tileMatrixSetRepository = tileMatrixSetRepository;
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {

        return new Builder().enabled(false)
                                                        .tileProvider(ImmutableTileProviderFeatures.builder()
                                                                                                   .tileEncodings(extensionRegistry.getExtensionsForType(
                                                                                                           TileFormatWithQuerySupportExtension.class)
                                                                                                                                   .stream()
                                                                                                                                   .filter(FormatExtension::isEnabledByDefault)
                                                                                                                                   .map(format -> format.getMediaType().label())
                                                                                                                                   .collect(ImmutableList.toImmutableList()))
                                                                                                   .center(ImmutableList.of(0.0, 0.0))
                                                                                                   .zoomLevels(ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder()
                                                                                                           .min(0)
                                                                                                           .max(23)
                                                                                                           .build()))
                                                                                                   .zoomLevelsCache(ImmutableMap.of())
                                                                                                   .seeding(ImmutableMap.of())
                                                                                                   .limit(LIMIT_DEFAULT)
                                                                                                   .singleCollectionEnabled(true)
                                                                                                   .multiCollectionEnabled(true)
                                                                                                   .ignoreInvalidGeometries(false)
                                                                                                   .minimumSizeInPixel(MINIMUM_SIZE_IN_PIXEL)
                                                                                                   .build())
                                                        .tileSetEncodings(extensionRegistry.getExtensionsForType(
                                                                TileSetFormatExtension.class)
                                                                                           .stream()
                                                                                           .filter(FormatExtension::isEnabledByDefault)
                                                                                           .map(format -> format.getMediaType().label())
                                                                                           .collect(ImmutableList.toImmutableList()))
                                                        .cache(TilesConfiguration.TileCacheType.FILES)
                                                        .style("DEFAULT")
                                                        .build();
    }

    private MinMax getZoomLevels(OgcApiDataV2 apiData, TilesConfiguration config, String tileMatrixSetId) {
        if (Objects.nonNull(config.getZoomLevelsDerived()))
            return config.getZoomLevelsDerived().get(tileMatrixSetId);

        Optional<TileMatrixSet> tileMatrixSet = tileMatrixSetRepository.get(tileMatrixSetId)
                                                                       .filter(tms -> config.getTileMatrixSets().contains(tms.getId()));
        return tileMatrixSet.map(matrixSet -> new ImmutableMinMax.Builder()
                .min(matrixSet.getMinLevel())
                .max(matrixSet.getMaxLevel())
                .build()).orElse(null);
    }

    private MinMax getZoomLevels(OgcApiDataV2 apiData, String tileMatrixSetId) {
        Optional<TileMatrixSet> tileMatrixSet = tileMatrixSetRepository.get(tileMatrixSetId)
                                                                       .filter(tms -> apiData.getExtension(TilesConfiguration.class)
                                                                                        .map(TilesConfiguration::getTileMatrixSets)
                                                                                        .filter(set -> set.contains(tms.getId()))
                                                                                        .isPresent());
        return tileMatrixSet.map(matrixSet -> new ImmutableMinMax.Builder()
                .min(matrixSet.getMinLevel())
                .max(matrixSet.getMaxLevel())
                .build()).orElse(null);
    }

    @Override
    public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
        // since building block / capability components are currently always enabled,
        // we need to test, if the TILES module is enabled for the API and stop, if not
        OgcApiDataV2 apiData = api.getData();
        if (!apiData.getExtension(TilesConfiguration.class)
                    .map(ExtensionConfiguration::isEnabled)
                    .orElse(false)) {
            return ValidationResult.of();
        }

        try {
            SQLiteJDBCLoader.initialize();
        } catch (Exception e) {
            return ImmutableValidationResult.builder()
                                            .mode(apiValidation)
                                            .addStrictErrors(MessageFormat.format("Could not load SQLite: {}", e.getMessage()))
                                            .build();
        }

        if (apiValidation== MODE.NONE) {
            return ValidationResult.of();
        }

        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                .mode(apiValidation);

        for (Map.Entry<String, TilesConfiguration> entry : apiData.getCollections()
                                                                  .entrySet()
                                                                  .stream()
                                                                  .filter(entry -> entry.getValue().getEnabled() && entry.getValue().getExtension(TilesConfiguration.class).isPresent())
                                                                  .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().getExtension(TilesConfiguration.class).get()))
                                                                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                                                                  .entrySet()) {
            String collectionId = entry.getKey();
            TilesConfiguration config = entry.getValue();

            Optional<FeatureSchema> schema = providers.getFeatureSchema(apiData, apiData.getCollections().get(collectionId));
            List<String> featureProperties = schema.isPresent()
                    ? schemaInfo.getPropertyNames(schema.get(), false, false)
                    : ImmutableList.of();

            List<String> formatLabels = extensionRegistry.getExtensionsForType(TileFormatExtension.class)
                                                         .stream()
                                                         .filter(formatExtension -> formatExtension.isEnabledForApi(
                                                             apiData))
                                                         .map(format -> format.getMediaType().label())
                                                         .collect(Collectors.toUnmodifiableList());
            List<String> tileEncodings = config.getTileEncodingsDerived();
            if (Objects.isNull(tileEncodings)) {
                builder.addStrictErrors(MessageFormat.format("No tile encoding has been specified in the TILES module configuration of collection ''{0}''.", collectionId));
            } else {
                for (String encoding : config.getTileEncodingsDerived()) {
                    if (!formatLabels.contains(encoding)) {
                        builder.addStrictErrors(MessageFormat.format("The tile encoding ''{0}'' is specified in the TILES module configuration of collection ''{1}'', but the format does not exist.", encoding, collectionId));
                    }
                }
            }

            formatLabels = extensionRegistry.getExtensionsForType(TileSetFormatExtension.class)
                                            .stream()
                                            .filter(formatExtension -> formatExtension.isEnabledForApi(
                                                apiData))
                                            .map(format -> format.getMediaType().label())
                                            .collect(Collectors.toUnmodifiableList());
            for (String encoding : config.getTileSetEncodings()) {
                if (!formatLabels.contains(encoding)) {
                    builder.addStrictErrors(MessageFormat.format("The tile set encoding ''{0}'' is specified in the TILES module configuration of collection ''{1}'', but the format does not exist.", encoding, collectionId));
                }
            }

            List<Double> center = config.getCenterDerived();
            if (center.size()!=0 && center.size()!=2)
                builder.addStrictErrors(MessageFormat.format("The center has been specified in the TILES module configuration of collection ''{1}'', but the array length is ''{0}'', not 2.", center.size(), collectionId));

            Map<String, MinMax> zoomLevels = config.getZoomLevelsDerived();
            for (Map.Entry<String, MinMax> entry2 : zoomLevels.entrySet()) {
                String tileMatrixSetId = entry2.getKey();
                Optional<TileMatrixSet> tileMatrixSet = tileMatrixSetRepository.get(tileMatrixSetId)
                                                                               .filter(tms -> config.getTileMatrixSets().contains(tms.getId()));
                if (tileMatrixSet.isEmpty()) {
                    builder.addStrictErrors(MessageFormat.format("The configuration in the TILES module of collection ''{0}'' references a tile matrix set ''{1}'' that is not available in this API.", collectionId, tileMatrixSetId));
                } else {
                    if (tileMatrixSet.get().getMinLevel() > entry2.getValue().getMin()) {
                        builder.addStrictErrors(MessageFormat.format("The configuration in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level of the tile matrix set is ''{3}''.", collectionId, tileMatrixSetId, entry2.getValue().getMin(), tileMatrixSet.get().getMinLevel()));
                    }
                    if (tileMatrixSet.get().getMaxLevel() < entry2.getValue().getMax()) {
                        builder.addStrictErrors(MessageFormat.format("The configuration in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level of the tile matrix set is ''{3}''.", collectionId, tileMatrixSetId, entry2.getValue().getMax(), tileMatrixSet.get().getMaxLevel()));
                    }
                    if (entry2.getValue().getDefault().isPresent()) {
                        Integer defaultLevel = entry2.getValue().getDefault().get();
                        if (defaultLevel < entry2.getValue().getMin() || defaultLevel > entry2.getValue().getMax()) {
                            builder.addStrictErrors(MessageFormat.format("The configuration in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' specifies a default level ''{2}'' that is outside of the range [ ''{3}'' : ''{4}'' ].", tileMatrixSetId, defaultLevel, entry2.getValue().getMin(), entry2.getValue().getMax()));
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
                            builder.addStrictErrors(MessageFormat.format("The cache in the TILES module of collection ''{0}'' references a tile matrix set ''{1}'' that is not configured for this API.", collectionId, tileMatrixSetId));
                        } else {
                            if (zoomLevelsTms.getMin() > entry2.getValue().getMin()) {
                                builder.addStrictErrors(MessageFormat.format("The cache in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level is ''{3}''.", collectionId, tileMatrixSetId, entry2.getValue().getMin(), zoomLevelsTms.getMin()));
                            }
                            if (zoomLevelsTms.getMax() < entry2.getValue().getMax()) {
                                builder.addStrictErrors(MessageFormat.format("The cache in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level is ''{3}''.", collectionId, tileMatrixSetId, entry2.getValue().getMax(), zoomLevelsTms.getMax()));
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
                            builder.addStrictErrors(MessageFormat.format("The seeding in the TILES module of collection ''{0}'' references a tile matrix set ''{1}'' that is not configured for this API.", collectionId, tileMatrixSetId));
                        } else {
                            if (zoomLevelsTms.getMin() > entry2.getValue().getMin()) {
                                builder.addStrictErrors(MessageFormat.format("The seeding in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level is ''{3}''.", collectionId, tileMatrixSetId, entry2.getValue().getMin(), zoomLevelsTms.getMin()));
                            }
                            if (zoomLevelsTms.getMax() < entry2.getValue().getMax()) {
                                builder.addStrictErrors(MessageFormat.format("The seeding in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level is ''{3}''.", collectionId, tileMatrixSetId, entry2.getValue().getMax(), zoomLevelsTms.getMax()));
                            }
                        }
                    }
                }

                final Integer limit = Objects.requireNonNullElse(config.getLimitDerived(), 0);
                if (limit < 1) {
                    builder.addStrictErrors(MessageFormat.format("The feature limit in the TILES module must be a positive integer. Found in collection ''{1}'': {0}.",limit, collectionId));
                }

                final Map<String, List<PredefinedFilter>> filters = config.getFiltersDerived();
                if (Objects.nonNull(filters)) {
                    for (Map.Entry<String, List<PredefinedFilter>> entry2 : filters.entrySet()) {
                        String tileMatrixSetId = entry2.getKey();
                        MinMax zoomLevelsCfg = getZoomLevels(apiData, config, tileMatrixSetId);
                        if (Objects.isNull(zoomLevelsCfg)) {
                            builder.addStrictErrors(MessageFormat.format("The filters in the TILES module of collection ''{0}'' references a tile matrix set ''{0}'' that is not configured for this API.", collectionId, tileMatrixSetId));
                        } else {
                            for (PredefinedFilter filter : entry2.getValue()) {
                                if (zoomLevelsCfg.getMin() > filter.getMin()) {
                                    builder.addStrictErrors(MessageFormat.format("A filter in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level is ''{3}''.", collectionId, tileMatrixSetId, filter.getMin(), zoomLevelsCfg.getMin()));
                                }
                                if (zoomLevelsCfg.getMax() < filter.getMax()) {
                                    builder.addStrictErrors(MessageFormat.format("A filter in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level is ''{3}''.", collectionId, tileMatrixSetId, filter.getMax(), zoomLevelsCfg.getMax()));
                                }
                                if (filter.getFilter().isPresent()) {
                                    // try to convert the filter to CQL2-text
                                    String expression = filter.getFilter().get();
                                    FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
                                    final Map<String, String> filterableFields = queryParser.getFilterableFields(
                                        apiData, collectionData);
                                    final Map<String, String> queryableTypes = queryParser.getQueryableTypes(
                                        apiData, collectionData);
                                    try {
                                        queryParser.getFilterFromQuery(ImmutableMap.of("filter", expression), filterableFields, ImmutableSet.of("filter"), queryableTypes, Cql.Format.TEXT);
                                    } catch (Exception e) {
                                        builder.addErrors(MessageFormat.format("A filter ''{0}'' in the TILES module of collection ''{1}'' for tile matrix set ''{2}'' is invalid. Reason: {3}", expression, collectionId, tileMatrixSetId, e.getMessage()));
                                    }
                                }
                            }
                        }
                    }
                }

                final Map<String, List<Rule>> rules = config.getRulesDerived();
                if (Objects.nonNull(rules)) {
                    for (Map.Entry<String, List<Rule>> entry2 : rules.entrySet()) {
                        String tileMatrixSetId = entry2.getKey();
                        MinMax zoomLevelsCfg = getZoomLevels(apiData, config, tileMatrixSetId);
                        if (Objects.isNull(zoomLevelsCfg)) {
                            builder.addStrictErrors(MessageFormat.format("The rules in the TILES module of collection ''{0}'' references a tile matrix set ''{0}'' that is not configured for this API.", collectionId, tileMatrixSetId));
                        } else {
                            for (Rule rule : entry2.getValue()) {
                                if (zoomLevelsCfg.getMin() > rule.getMin()) {
                                    builder.addStrictErrors(MessageFormat.format("A rule in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level is ''{3}''.", collectionId, tileMatrixSetId, rule.getMin(), zoomLevelsCfg.getMin()));
                                }
                                if (zoomLevelsCfg.getMax() < rule.getMax()) {
                                    builder.addStrictErrors(MessageFormat.format("A rule in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level is ''{3}''.", collectionId, tileMatrixSetId, rule.getMax(), zoomLevelsCfg.getMax()));
                                }
                                for (String property : rule.getProperties()) {
                                    if (!featureProperties.contains(property)) {
                                        builder.addErrors(MessageFormat.format("A rule in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' references property ''{2}'' that is not part of the feature schema.", collectionId, tileMatrixSetId, property));
                                    }
                                }
                                for (String property : rule.getGroupBy()) {
                                    if (!featureProperties.contains(property)) {
                                        builder.addErrors(MessageFormat.format("A rule in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' references group-by property ''{2}'' that is not part of the feature schema.", collectionId, tileMatrixSetId, property));
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
