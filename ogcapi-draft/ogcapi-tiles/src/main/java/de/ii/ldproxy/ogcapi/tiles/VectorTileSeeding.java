/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesQuery;
import de.ii.ldproxy.ogcapi.infra.rest.ImmutableOgcApiRequestContext;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.xtraplatform.codelists.CodelistRegistry;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.server.CoreServerConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class is responsible for a automatic generation of the Tiles.
 * The range is specified in the config.
 * The automatic generation is executed, when the server is started/restarted.
 */
@Component
@Provides
@Instantiate
public class VectorTileSeeding implements OgcApiStartupTask {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(VectorTileSeeding.class);
    private Thread t = null;
    private Map<Thread, String> threadMap = new HashMap<>();

    private final I18n i18n;
    private final CrsTransformerFactory crsTransformerFactory;
    private final MetricRegistry metricRegistry;
    private CodelistRegistry codelistRegistry;
    private final OgcApiExtensionRegistry extensionRegistry;
    private final TileMatrixSetLimitsGenerator limitsGenerator;
    private final TilesCache tilesCache;
    private final CoreServerConfig coreServerConfig;
    private final OgcApiFeaturesQuery queryParser;
    private final OgcApiFeatureCoreProviders providers;
    private final TilesQueriesHandler queryHandler;

    public VectorTileSeeding(@Requires I18n i18n,
                             @Requires CrsTransformerFactory crsTransformerFactory,
                             @Requires Dropwizard dropwizard,
                             @Requires CodelistRegistry codelistRegistry,
                             @Requires OgcApiExtensionRegistry extensionRegistry,
                             @Requires TileMatrixSetLimitsGenerator limitsGenerator,
                             @Requires TilesCache tilesCache,
                             @Requires CoreServerConfig coreServerConfig,
                             @Requires OgcApiFeaturesQuery queryParser,
                             @Requires OgcApiFeatureCoreProviders providers,
                             @Requires TilesQueriesHandler queryHandler) {
        this.i18n = i18n;
        this.crsTransformerFactory = crsTransformerFactory;
        this.codelistRegistry = codelistRegistry;

        this.metricRegistry = dropwizard.getEnvironment()
                .metrics();
        this.extensionRegistry = extensionRegistry;
        this.limitsGenerator = limitsGenerator;
        this.tilesCache = tilesCache;
        this.coreServerConfig = coreServerConfig;
        this.queryParser = queryParser;
        this.providers = providers;
        this.queryHandler = queryHandler;
    }


    // TODO private final VectorTileMapGenerator vectorTileMapGenerator = new VectorTileMapGenerator();

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<TilesConfiguration> extension = getExtensionConfiguration(apiData, TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::getEnabled)
                .filter(config -> !config.getSeeding().isEmpty())
                .isPresent();
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
        Optional<TilesConfiguration> extension = featureType!=null ?
                this.getExtensionConfiguration(apiData, featureType, TilesConfiguration.class) :
                this.getExtensionConfiguration(apiData, TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::getEnabled)
                .filter(config -> !config.getSeeding().isEmpty())
                .isPresent();
    }

    private boolean isEnabledForApiMultiCollection(OgcApiApiDataV2 apiData) {
        Optional<TilesConfiguration> extension = this.getExtensionConfiguration(apiData, TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::getEnabled)
                .filter(TilesConfiguration::getMultiCollectionEnabled)
                .filter(config -> !config.getSeeding().isEmpty())
                .isPresent();
    }

    /**
     * The runnable Task which starts the seeding.
     *
     * @param api               the API
     * @return the runnable process
     */
    @Override
    public Runnable getTask(OgcApiApi api) {

        OgcApiApiDataV2 apiData = api.getData();

        List<TileFormatExtension> outputFormats = extensionRegistry.getExtensionsForType(TileFormatExtension.class);
        if (outputFormats.isEmpty()) {
            return () -> {
            };
        }

        Runnable startSeeding = () -> {

            // first seed the multi-layer tiles, which also generates the necessary single-layer tiles
            seedMultiLayerTiles(api, outputFormats);

            // add any additional single-layer tiles
            seedSingleLayerTiles(api, outputFormats);

        };
        t = new Thread(startSeeding);
        t.setDaemon(true);
        t.start();
        threadMap.put(t, apiData.getId());
        return startSeeding;

    }

    /**
     * @return a Map with all ongoing threads
     */
    public Map<Thread, String> getThreadMap() {
        return threadMap;
    }

    /**
     * removes a specific thread from the threadMap.
     *
     * @param t the thread which should be removed
     */
    public void removeThreadMapEntry(Thread t) {
        threadMap.remove(t);
    }

    /**
     * checks if the tiles extension is available and returns a Map with entrys for each collection and their zoomLevel or seeding
     * @param apiData       the service data of the Wfs3 Service
     * @return a map with all collectionIds and the seeding configuration
     */
    private Map<String, Map<String, MinMax>> getMinMaxMap(OgcApiApiDataV2 apiData) {

        Map<String, Map<String, MinMax>> minMaxMap = new HashMap<>();
        for (FeatureTypeConfigurationOgcApi featureType : apiData.getCollections().values()) {
            final Optional<TilesConfiguration> tilesConfiguration = featureType==null ?
                    getExtensionConfiguration(apiData, TilesConfiguration.class) :
                    getExtensionConfiguration(apiData, featureType, TilesConfiguration.class);
            if (tilesConfiguration.isPresent()) {
                Map<String, MinMax> seedingConfig = tilesConfiguration.get().getSeeding();
                if (seedingConfig != null && !seedingConfig.isEmpty())
                    minMaxMap.put(featureType.getId(), seedingConfig);
            }
        }
        return minMaxMap;
    }

    private void seedSingleLayerTiles(OgcApiApi api, List<TileFormatExtension> outputFormats) {
        OgcApiApiDataV2 apiData = api.getData();
        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData);
        Map<String, Map<String, MinMax>> seedingMap = getMinMaxMap(apiData);
        for (Map.Entry<String, Map<String, MinMax>> collectionEntry : seedingMap.entrySet()) {
            String collectionId = collectionEntry.getKey();
            FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
            Optional<TilesConfiguration> tilesConfiguration = featureType!=null ?
                    this.getExtensionConfiguration(apiData, featureType, TilesConfiguration.class) :
                    this.getExtensionConfiguration(apiData, TilesConfiguration.class);
            if (!tilesConfiguration.map(TilesConfiguration::getEnabled).filter(enabled -> enabled == true).isPresent())
                continue;
            Map<String, MinMax> seedingConfig = collectionEntry.getValue();
            for (TileFormatExtension outputFormat : outputFormats) {
                for (Map.Entry<String, MinMax> entry : seedingConfig.entrySet()) {
                    TileMatrixSet tileMatrixSet = getTileMatrixSetById(entry.getKey());
                    MinMax zoomLevels = entry.getValue();
                    List<TileMatrixSetLimits> allLimits = limitsGenerator.getCollectionTileMatrixSetLimits(apiData, collectionId, tileMatrixSet, zoomLevels, crsTransformerFactory);
                    for (TileMatrixSetLimits limits : allLimits) {
                        int level = Integer.parseInt(limits.getTileMatrix());
                        for (int row = limits.getMinTileRow(); row <= limits.getMaxTileRow(); row++) {
                            for (int col = limits.getMinTileCol(); col <= limits.getMaxTileCol(); col++) {
                                Tile tile = new ImmutableTile.Builder()
                                        .collectionIds(ImmutableList.of(collectionId))
                                        .tileMatrixSet(tileMatrixSet)
                                        .tileLevel(level)
                                        .tileRow(row)
                                        .tileCol(col)
                                        .api(api)
                                        .temporary(false)
                                        .featureProvider(featureProvider)
                                        .outputFormat(outputFormat)
                                        .build();
                                File tileFile = tilesCache.getFile(tile);
                                if (tileFile.exists())
                                    // already there, nothing to create
                                    continue;

                                URI uri;
                                String uriString = coreServerConfig.getExternalUrl() + "/collections/" + collectionId + "/tiles/" +
                                        String.join("/", tileMatrixSet.getId(), String.valueOf(level), String.valueOf(row), String.valueOf(col));
                                try {
                                    uri = new URI(uriString);
                                } catch (URISyntaxException e) {
                                    LOGGER.error("Stopping seeding. Invalid request URI during seeding: " + uriString);
                                    return;
                                }

                                URICustomizer uriCustomizer = new URICustomizer(uri);
                                OgcApiRequestContext requestContext = new ImmutableOgcApiRequestContext.Builder()
                                        .api(api)
                                        .requestUri(uri)
                                        .mediaType(outputFormat.getMediaType())
                                        .build();

                                // generate a query template for an arbitrary collection
                                FeatureQuery query = outputFormat.getQuery(tile, ImmutableList.of(), ImmutableMap.of(), tilesConfiguration.get(), uriCustomizer);

                                OgcApiFeaturesCoreConfiguration coreConfiguration = getExtensionConfiguration(apiData, OgcApiFeaturesCoreConfiguration.class).get();

                                TilesQueriesHandler.OgcApiQueryInputTileSingleLayer queryInput = new ImmutableOgcApiQueryInputTileSingleLayer.Builder()
                                        .tile(tile)
                                        .query(query)
                                        .outputStream(new ByteArrayOutputStream())
                                        .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
                                        .build();

                                String msg = "Seed single-layer tile {}/{}/{}/{} in API '{}', collection '{}', format '{}'.";
                                LOGGER.debug(msg, tileMatrixSet.getId(), level, row, col, api.getId(), collectionId, outputFormat.getExtension());


                                queryHandler.handle(TilesQueriesHandler.Query.SINGLE_LAYER_TILE, queryInput, requestContext);
                            }
                        }
                    }
                }
            }
        }

    }

    private void seedMultiLayerTiles(OgcApiApi api, List<TileFormatExtension> outputFormats) {
        OgcApiApiDataV2 apiData = api.getData();
        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData);
        Map<String, MinMax> multiLayerTilesSeeding = ImmutableMap.of();
        Optional<TilesConfiguration> tilesConfiguration = getExtensionConfiguration(apiData, TilesConfiguration.class);
        if (tilesConfiguration.isPresent()) {
            Map<String, MinMax> seedingConfig = tilesConfiguration.get().getSeeding();
            if (seedingConfig != null && !seedingConfig.isEmpty())
                multiLayerTilesSeeding = seedingConfig;
        }

        List<String> collectionIds = apiData.getCollections()
                .values()
                .stream()
                .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
                .filter(collection -> {
                    Optional<TilesConfiguration> config = getExtensionConfiguration(apiData, collection, TilesConfiguration.class);
                    return config.map(TilesConfiguration::getEnabled).filter(enabled -> enabled == true).isPresent();
                })
                .map(collection -> collection.getId())
                .collect(Collectors.toList());

        for (TileFormatExtension outputFormat : outputFormats) {
            if (!outputFormat.canMultiLayer() || collectionIds.isEmpty())
                continue;
            for (Map.Entry<String, MinMax> entry : multiLayerTilesSeeding.entrySet()) {
                TileMatrixSet tileMatrixSet = getTileMatrixSetById(entry.getKey());
                MinMax zoomLevels = entry.getValue();
                List<TileMatrixSetLimits> allLimits = limitsGenerator.getTileMatrixSetLimits(apiData, tileMatrixSet, zoomLevels, crsTransformerFactory);
                for (TileMatrixSetLimits limits : allLimits) {
                    int level = Integer.parseInt(limits.getTileMatrix());
                    for (int row = limits.getMinTileRow(); row <= limits.getMaxTileRow(); row++) {
                        for (int col = limits.getMinTileCol(); col <= limits.getMaxTileCol(); col++) {
                            Tile multiLayerTile = new ImmutableTile.Builder()
                                    .collectionIds(collectionIds)
                                    .tileMatrixSet(tileMatrixSet)
                                    .tileLevel(level)
                                    .tileRow(row)
                                    .tileCol(col)
                                    .api(api)
                                    .temporary(false)
                                    .featureProvider(featureProvider)
                                    .outputFormat(outputFormat)
                                    .build();
                            File tileFile = tilesCache.getFile(multiLayerTile);
                            if (tileFile.exists())
                                // already there, nothing to create
                                continue;

                            URI uri;
                            String uriString = coreServerConfig.getExternalUrl() + "/tiles/" +
                                    String.join("/", tileMatrixSet.getId(), String.valueOf(level), String.valueOf(row), String.valueOf(col));
                            try {
                                uri = new URI(uriString);
                            } catch (URISyntaxException e) {
                                LOGGER.error("Stopping seeding. Invalid request URI during seeding: " + uriString);
                                return;
                            }

                            URICustomizer uriCustomizer = new URICustomizer(uri);
                            OgcApiRequestContext requestContext = new ImmutableOgcApiRequestContext.Builder()
                                    .api(api)
                                    .requestUri(uri)
                                    .mediaType(outputFormat.getMediaType())
                                    .build();

                            Map<String, Tile> singleLayerTileMap = collectionIds.stream()
                                    .collect(ImmutableMap.toImmutableMap(collectionId -> collectionId, collectionId -> new ImmutableTile.Builder()
                                            .from(multiLayerTile)
                                            .collectionIds(ImmutableList.of(collectionId))
                                            .build()));

                            // generate a query template for an arbitrary collection
                            FeatureQuery query = outputFormat.getQuery(singleLayerTileMap.get(collectionIds.get(0)), ImmutableList.of(), ImmutableMap.of(), tilesConfiguration.get(), uriCustomizer);

                            Map<String, FeatureQuery> queryMap = collectionIds.stream()
                                    .collect(ImmutableMap.toImmutableMap(collectionId -> collectionId, collectionId -> ImmutableFeatureQuery.builder()
                                            .from(query)
                                            .type(collectionId)
                                            .build()));

                            OgcApiFeaturesCoreConfiguration coreConfiguration = getExtensionConfiguration(apiData, OgcApiFeaturesCoreConfiguration.class).get();

                            TilesQueriesHandler.OgcApiQueryInputTileMultiLayer queryInput = new ImmutableOgcApiQueryInputTileMultiLayer.Builder()
                                    .tile(multiLayerTile)
                                    .singleLayerTileMap(singleLayerTileMap)
                                    .queryMap(queryMap)
                                    .outputStream(new ByteArrayOutputStream())
                                    .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
                                    .build();

                            String msg = "Seed multi-layer tile {}/{}/{}/{} in API '{}', format '{}'.";
                            LOGGER.debug(msg, tileMatrixSet.getId(), level, row, col, api.getId(), outputFormat.getExtension());


                            queryHandler.handle(TilesQueriesHandler.Query.MULTI_LAYER_TILE, queryInput, requestContext);
                        }
                    }
                }
            }
        }
    }

    private TileMatrixSet getTileMatrixSetById(String tileMatrixSetId) {
        TileMatrixSet tileMatrixSet = null;
        for (OgcApiContentExtension contentExtension : extensionRegistry.getExtensionsForType(OgcApiContentExtension.class)) {
            if (contentExtension instanceof TileMatrixSet && ((TileMatrixSet) contentExtension).getId().equals(tileMatrixSetId)) {
                tileMatrixSet = (TileMatrixSet) contentExtension;
                break;
            }
        }

        return tileMatrixSet;
    }
}
