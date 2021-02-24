/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.cql.domain.Cql;
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
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class CapabilityVectorTiles implements ApiBuildingBlock {

    private static final int LIMIT_DEFAULT = 100000;
    private static final int MAX_POLYGON_PER_TILE_DEFAULT = 10000;
    private static final int MAX_LINE_STRING_PER_TILE_DEFAULT = 10000;
    private static final int MAX_POINT_PER_TILE_DEFAULT = 10000;

    @Requires
    ExtensionRegistry extensionRegistry;
    @Requires
    FeaturesCoreProviders providers;
    @Requires
    FeaturesQuery queryParser;

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {

        return new ImmutableTilesConfiguration.Builder().enabled(false)
                                                        .limit(LIMIT_DEFAULT)
                                                        .maxPolygonPerTileDefault(MAX_POLYGON_PER_TILE_DEFAULT)
                                                        .maxLineStringPerTileDefault(MAX_LINE_STRING_PER_TILE_DEFAULT)
                                                        .maxPointPerTileDefault(MAX_POINT_PER_TILE_DEFAULT)
                                                        .singleCollectionEnabled(true)
                                                        .multiCollectionEnabled(true)
                                                        .ignoreInvalidGeometries(false)
                                                        .maxRelativeAreaChangeInPolygonRepair(0.1)
                                                        .maxAbsoluteAreaChangeInPolygonRepair(1.0)
                                                        .minimumSizeInPixel(0.5)
                                                        .zoomLevels(ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder()
                                                                .min(0)
                                                                .max(23)
                                                                .build()))
                                                        .zoomLevelsCache(ImmutableMap.of())
                                                        .tileEncodings(extensionRegistry.getExtensionsForType(TileFormatExtension.class)
                                                                                        .stream()
                                                                                        .filter(FormatExtension::isEnabledByDefault)
                                                                                        .map(format -> format.getMediaType().label())
                                                                                        .collect(ImmutableList.toImmutableList()))
                                                        .tileSetEncodings(extensionRegistry.getExtensionsForType(TileSetFormatExtension.class)
                                                                                        .stream()
                                                                                        .filter(FormatExtension::isEnabledByDefault)
                                                                                        .map(format -> format.getMediaType().label())
                                                                                        .collect(ImmutableList.toImmutableList()))
                                                        .build();
    }

    private MinMax getZoomLevels(OgcApiDataV2 apiData, TilesConfiguration config, String tileMatrixSetId) {
        if (Objects.nonNull(config.getZoomLevels()))
            return config.getZoomLevels().get(tileMatrixSetId);

        Optional<TileMatrixSet> tileMatrixSet = extensionRegistry.getExtensionsForType(TileMatrixSet.class)
                                                                 .stream()
                                                                 .filter(tms -> tms.getId().equals(tileMatrixSetId) && tms.isEnabledForApi(apiData))
                                                                 .findAny();
        if (tileMatrixSet.isPresent())
            return new ImmutableMinMax.Builder()
                    .min(tileMatrixSet.get().getMinLevel())
                    .max(tileMatrixSet.get().getMaxLevel())
                    .build();

        return null;
    }

    private MinMax getZoomLevels(OgcApiDataV2 apiData, String tileMatrixSetId) {
        Optional<TileMatrixSet> tileMatrixSet = extensionRegistry.getExtensionsForType(TileMatrixSet.class)
                                                                 .stream()
                                                                 .filter(tms -> tms.getId().equals(tileMatrixSetId) && tms.isEnabledForApi(apiData))
                                                                 .findAny();
        if (tileMatrixSet.isPresent())
            return new ImmutableMinMax.Builder()
                    .min(tileMatrixSet.get().getMinLevel())
                    .max(tileMatrixSet.get().getMaxLevel())
                    .build();

        return null;
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, MODE apiValidation) {
        // since building block / capability components are currently always enabled,
        // we need to test, if the TILES module is enabled for the API and stop, if not
        if (!apiData.getExtension(TilesConfiguration.class)
                    .map(cfg -> cfg.getEnabled())
                    .orElse(false))
            return ValidationResult.of();

        if (apiValidation== MODE.NONE)
            return ValidationResult.of();

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

            List<String> featureProperties = SchemaInfo.getPropertyNames(providers.getFeatureSchema(apiData, apiData.getCollections().get(collectionId)), false);

            List<String> formatLabels = extensionRegistry.getExtensionsForType(TileFormatExtension.class)
                                                         .stream()
                                                         .filter(formatExtension -> formatExtension.isEnabledForApi(apiData))
                                                         .map(format -> format.getMediaType().label())
                                                         .collect(Collectors.toUnmodifiableList());
            for (String encoding : config.getTileEncodings()) {
                if (!formatLabels.contains(encoding)) {
                    builder.addStrictErrors(MessageFormat.format("The tile encoding ''{0}'' is specified in the TILES module configuration of collection ''{1}'', but the format does not exist.", encoding, collectionId));
                }
            }

            formatLabels = extensionRegistry.getExtensionsForType(TileSetFormatExtension.class)
                                            .stream()
                                            .filter(formatExtension -> formatExtension.isEnabledForApi(apiData))
                                            .map(format -> format.getMediaType().label())
                                            .collect(Collectors.toUnmodifiableList());
            for (String encoding : config.getTileSetEncodings()) {
                if (!formatLabels.contains(encoding)) {
                    builder.addStrictErrors(MessageFormat.format("The tile set encoding ''{0}'' is specified in the TILES module configuration of collection ''{1}'', but the format does not exist.", encoding, collectionId));
                }
            }

            if (config.getLimit() < 1) {
                builder.addStrictErrors(MessageFormat.format("The feature limit in the TILES module must be a positive integer. Found in collection ''{1}'': {0}.",config.getLimit(), collectionId));
            }

            if (Objects.nonNull(config.getZoomLevels())) {
                for (Map.Entry<String, MinMax> entry2 : config.getZoomLevels().entrySet()) {
                    String tileMatrixSetId = entry2.getKey();
                    Optional<TileMatrixSet> tileMatrixSet = extensionRegistry.getExtensionsForType(TileMatrixSet.class)
                                                                             .stream()
                                                                             .filter(tms -> tms.getId().equals(tileMatrixSetId) && tms.isEnabledForApi(apiData))
                                                                             .findAny();
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
            }

            if (Objects.nonNull(config.getZoomLevelsCache())) {
                for (Map.Entry<String, MinMax> entry2 : config.getZoomLevelsCache().entrySet()) {
                    String tileMatrixSetId = entry2.getKey();
                    MinMax zoomLevels = getZoomLevels(apiData, tileMatrixSetId);
                    if (Objects.isNull(zoomLevels)) {
                        builder.addStrictErrors(MessageFormat.format("The cache in the TILES module of collection ''{0}'' references a tile matrix set ''{1}'' that is not configured for this API.", collectionId, tileMatrixSetId));
                    } else {
                        if (zoomLevels.getMin() > entry2.getValue().getMin()) {
                            builder.addStrictErrors(MessageFormat.format("The cache in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level is ''{3}''.", collectionId, tileMatrixSetId, entry2.getValue().getMin(), zoomLevels.getMin()));
                        }
                        if (zoomLevels.getMax() < entry2.getValue().getMax()) {
                            builder.addStrictErrors(MessageFormat.format("The cache in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level is ''{3}''.", collectionId, tileMatrixSetId, entry2.getValue().getMax(), zoomLevels.getMax()));
                        }
                    }
                }
            }

            if (Objects.nonNull(config.getSeeding())) {
                for (Map.Entry<String, MinMax> entry2 : config.getSeeding().entrySet()) {
                    String tileMatrixSetId = entry2.getKey();
                    MinMax zoomLevels = getZoomLevels(apiData, tileMatrixSetId);
                    if (Objects.isNull(zoomLevels)) {
                        builder.addStrictErrors(MessageFormat.format("The seeding in the TILES module of collection ''{0}'' references a tile matrix set ''{1}'' that is not configured for this API.", collectionId, tileMatrixSetId));
                    } else {
                        if (zoomLevels.getMin() > entry2.getValue().getMin()) {
                            builder.addStrictErrors(MessageFormat.format("The seeding in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level is ''{3}''.", collectionId, tileMatrixSetId, entry2.getValue().getMin(), zoomLevels.getMin()));
                        }
                        if (zoomLevels.getMax() < entry2.getValue().getMax()) {
                            builder.addStrictErrors(MessageFormat.format("The seeding in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level is ''{3}''.", collectionId, tileMatrixSetId, entry2.getValue().getMax(), zoomLevels.getMax()));
                        }
                    }
                }
            }

            if (Objects.nonNull(config.getFilters())) {
                for (Map.Entry<String, List<PredefinedFilter>> entry2 : config.getFilters().entrySet()) {
                    String tileMatrixSetId = entry2.getKey();
                    MinMax zoomLevels = getZoomLevels(apiData, config, tileMatrixSetId);
                    if (Objects.isNull(zoomLevels)) {
                        builder.addStrictErrors(MessageFormat.format("The filters in the TILES module of collection ''{0}'' references a tile matrix set ''{0}'' that is not configured for this API.", collectionId, tileMatrixSetId));
                    } else {
                        for (PredefinedFilter filter : entry2.getValue()) {
                            if (zoomLevels.getMin() > filter.getMin()) {
                                builder.addStrictErrors(MessageFormat.format("A filter in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level is ''{3}''.", collectionId, tileMatrixSetId, filter.getMin(), zoomLevels.getMin()));
                            }
                            if (zoomLevels.getMax() < filter.getMax()) {
                                builder.addStrictErrors(MessageFormat.format("A filter in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level is ''{3}''.", collectionId, tileMatrixSetId, filter.getMax(), zoomLevels.getMax()));
                            }
                            if (filter.getFilter().isPresent()) {
                                // try to convert the filter to CQL-text
                                String expression = filter.getFilter().get();
                                FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
                                final Map<String, String> filterableFields = collectionData.getExtension(FeaturesCoreConfiguration.class)
                                                                                           .map(FeaturesCoreConfiguration::getAllFilterParameters)
                                                                                           .orElse(ImmutableMap.of());
                                try {
                                    queryParser.getFilterFromQuery(ImmutableMap.of("filter", expression), filterableFields, ImmutableSet.of("filter"), Cql.Format.TEXT);
                                } catch (Exception e) {
                                    builder.addErrors(MessageFormat.format("A filter ''{0}'' in the TILES module of collection ''{1}'' for tile matrix set ''{2}'' is invalid. Reason: {3}", expression, collectionId, tileMatrixSetId, e.getMessage()));
                                }
                            }
                        }
                    }
                }
            }

            if (Objects.nonNull(config.getRules())) {
                for (Map.Entry<String, List<Rule>> entry2 : config.getRules().entrySet()) {
                    String tileMatrixSetId = entry2.getKey();
                    MinMax zoomLevels = getZoomLevels(apiData, config, tileMatrixSetId);
                    if (Objects.isNull(zoomLevels)) {
                        builder.addStrictErrors(MessageFormat.format("The rules in the TILES module of collection ''{0}'' references a tile matrix set ''{0}'' that is not configured for this API.", collectionId, tileMatrixSetId));
                    } else {
                        for (Rule rule : entry2.getValue()) {
                            if (zoomLevels.getMin() > rule.getMin()) {
                                builder.addStrictErrors(MessageFormat.format("A rule in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to start at level ''{2}'', but the minimum level is ''{3}''.", collectionId, tileMatrixSetId, rule.getMin(), zoomLevels.getMin()));
                            }
                            if (zoomLevels.getMax() < rule.getMax()) {
                                builder.addStrictErrors(MessageFormat.format("A rule in the TILES module of collection ''{0}'' for tile matrix set ''{1}'' is specified to end at level ''{2}'', but the maximum level is ''{3}''.", collectionId, tileMatrixSetId, rule.getMax(), zoomLevels.getMax()));
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

        return builder.build();
    }
}
