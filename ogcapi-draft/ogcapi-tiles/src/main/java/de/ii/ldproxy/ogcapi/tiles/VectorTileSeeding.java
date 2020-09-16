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
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.BackgroundTaskExceptionHandler;
import de.ii.ldproxy.ogcapi.domain.ContentExtension;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableRequestContext;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.StartupTask;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.dropwizard.domain.Dropwizard;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
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
public class VectorTileSeeding implements StartupTask {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(VectorTileSeeding.class);
    private Thread t = null;
    private Map<Thread, String> threadMap = new HashMap<>();

    private final I18n i18n;
    private final CrsTransformerFactory crsTransformerFactory;
    private final MetricRegistry metricRegistry;
    private final ExtensionRegistry extensionRegistry;
    private final TileMatrixSetLimitsGenerator limitsGenerator;
    private final TilesCache tilesCache;
    private final XtraPlatform xtraPlatform;
    private final FeaturesQuery queryParser;
    private final FeaturesCoreProviders providers;
    private final TilesQueriesHandler queryHandler;
    private final BackgroundTaskExceptionHandler backgroundTaskExceptionHandler;

    public VectorTileSeeding(@Requires I18n i18n,
                             @Requires CrsTransformerFactory crsTransformerFactory,
                             @Requires Dropwizard dropwizard,
                             @Requires ExtensionRegistry extensionRegistry,
                             @Requires TileMatrixSetLimitsGenerator limitsGenerator,
                             @Requires TilesCache tilesCache,
                             @Requires XtraPlatform xtraPlatform,
                             @Requires FeaturesQuery queryParser,
                             @Requires FeaturesCoreProviders providers,
                             @Requires TilesQueriesHandler queryHandler,
                             @Requires BackgroundTaskExceptionHandler backgroundTaskExceptionHandler) {
        this.i18n = i18n;
        this.crsTransformerFactory = crsTransformerFactory;

        this.metricRegistry = dropwizard.getEnvironment()
                .metrics();
        this.extensionRegistry = extensionRegistry;
        this.limitsGenerator = limitsGenerator;
        this.tilesCache = tilesCache;
        this.xtraPlatform = xtraPlatform;
        this.queryParser = queryParser;
        this.providers = providers;
        this.queryHandler = queryHandler;
        this.backgroundTaskExceptionHandler = backgroundTaskExceptionHandler;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        // currently no vector tiles support for WFS backends
        if (providers.getFeatureProvider(apiData).getData().getFeatureProviderType().equals("WFS"))
            return false;

        Optional<TilesConfiguration> extension = apiData.getExtension(TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::isEnabled)
                .filter(config -> !config.getSeeding().isEmpty())
                .isPresent();
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        // currently no vector tiles support for WFS backends
        if (providers.getFeatureProvider(apiData).getData().getFeatureProviderType().equals("WFS"))
            return false;

        FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
        Optional<TilesConfiguration> extension = featureType!=null ?
                featureType.getExtension(TilesConfiguration.class) :
                apiData.getExtension(TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::isEnabled)
                .filter(config -> !config.getSeeding().isEmpty())
                .isPresent();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    private boolean isEnabledForApiMultiCollection(OgcApiDataV2 apiData) {
        // currently no vector tiles support for WFS backends
        if (providers.getFeatureProvider(apiData).getData().getFeatureProviderType().equals("WFS"))
            return false;

        Optional<TilesConfiguration> extension = apiData.getExtension(TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::isEnabled)
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
    public Runnable getTask(OgcApi api) {

        OgcApiDataV2 apiData = api.getData();
        if (!isEnabledForApi(apiData)) {
            return () -> {
            };
        }

        List<TileFormatExtension> outputFormats = extensionRegistry.getExtensionsForType(TileFormatExtension.class);
        if (outputFormats.isEmpty()) {
            return () -> {
            };
        }

        Runnable startSeeding = () -> {

            LOGGER.debug("Start seeding vector tiles for API {}.", api.getId(), Thread.currentThread().getName());

            try {
                // first seed the multi-layer tiles, which also generates the necessary single-layer tiles
                seedMultiLayerTiles(api, outputFormats);

                // add any additional single-layer tiles
                seedSingleLayerTiles(api, outputFormats);
            } catch (IOException e) {
                throw new RuntimeException("Error accessing the tile cache during seeding.", e);
            }

            LOGGER.debug("Finished seeding vector tiles for API {}.", api.getId(), Thread.currentThread().getName());

        };
        t = new Thread(startSeeding);
        t.setUncaughtExceptionHandler(backgroundTaskExceptionHandler);
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
    private Map<String, Map<String, MinMax>> getMinMaxMap(OgcApiDataV2 apiData) {

        Map<String, Map<String, MinMax>> minMaxMap = new HashMap<>();
        for (FeatureTypeConfigurationOgcApi featureType : apiData.getCollections().values()) {
            final Optional<TilesConfiguration> tilesConfiguration = featureType!=null ?
                    featureType.getExtension(TilesConfiguration.class) :
                    apiData.getExtension(TilesConfiguration.class);
            if (tilesConfiguration.isPresent()) {
                Map<String, MinMax> seedingConfig = tilesConfiguration.get().getSeeding();
                if (seedingConfig != null && !seedingConfig.isEmpty())
                    minMaxMap.put(featureType.getId(), seedingConfig);
            }
        }
        return minMaxMap;
    }

    private void seedSingleLayerTiles(OgcApi api, List<TileFormatExtension> outputFormats) throws IOException {
        OgcApiDataV2 apiData = api.getData();
        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData);
        Map<String, Map<String, MinMax>> seedingMap = getMinMaxMap(apiData);
        for (Map.Entry<String, Map<String, MinMax>> collectionEntry : seedingMap.entrySet()) {
            String collectionId = collectionEntry.getKey();
            FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
            Optional<TilesConfiguration> tilesConfiguration = featureType!=null ?
                    featureType.getExtension(TilesConfiguration.class) :
                    apiData.getExtension(TilesConfiguration.class);
            if (!tilesConfiguration.filter(TilesConfiguration::isEnabled).isPresent())
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
                                Path tileFile = tilesCache.getFile(tile);
                                if (Files.exists(tileFile))
                                    // already there, nothing to create
                                    continue;

                                URI uri;
                                String uriString = String.format("%s/%s/collections/%s/tiles/%s/%s/%s/%s", xtraPlatform.getServicesUri(), apiData.getId(), collectionId, tileMatrixSet.getId(), level, row, col);
                                try {
                                    uri = new URI(uriString);
                                } catch (URISyntaxException e) {
                                    LOGGER.error("Stopping seeding. Invalid request URI during seeding: " + uriString);
                                    return;
                                }

                                URICustomizer uriCustomizer = new URICustomizer(uri);
                                ApiRequestContext requestContext = new ImmutableRequestContext.Builder()
                                        .api(api)
                                        .requestUri(uri)
                                        .mediaType(outputFormat.getMediaType())
                                        .build();

                                // generate a query template for an arbitrary collection
                                FeatureQuery query = outputFormat.getQuery(tile, ImmutableList.of(), ImmutableMap.of(), tilesConfiguration.get(), uriCustomizer);

                                FeaturesCoreConfiguration coreConfiguration = apiData.getExtension(FeaturesCoreConfiguration.class).get();

                                TilesQueriesHandler.QueryInputTileSingleLayer queryInput = new ImmutableQueryInputTileSingleLayer.Builder()
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

    private void seedMultiLayerTiles(OgcApi api, List<TileFormatExtension> outputFormats) throws IOException {
        OgcApiDataV2 apiData = api.getData();
        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData);
        Map<String, MinMax> multiLayerTilesSeeding = ImmutableMap.of();
        Optional<TilesConfiguration> tilesConfiguration = apiData.getExtension(TilesConfiguration.class);
        if (tilesConfiguration.isPresent()) {
            Map<String, MinMax> seedingConfig = tilesConfiguration.get().getSeeding();
            if (seedingConfig != null && !seedingConfig.isEmpty())
                multiLayerTilesSeeding = seedingConfig;
        }

        List<String> collectionIds = apiData.getCollections()
                .values()
                .stream()
                .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
                .filter(collection -> collection.getExtension(TilesConfiguration.class).filter(ExtensionConfiguration::isEnabled).isPresent())
                .map(FeatureTypeConfiguration::getId)
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
                            Path tileFile = tilesCache.getFile(multiLayerTile);
                            if (Files.exists(tileFile))
                                // already there, nothing to create
                                continue;

                            URI uri;
                            String uriString = String.format("%s/%s/tiles/%s/%s/%s/%s", xtraPlatform.getServicesUri(), apiData.getId(), tileMatrixSet.getId(), level, row, col);
                            try {
                                uri = new URI(uriString);
                            } catch (URISyntaxException e) {
                                LOGGER.error("Stopping seeding. Invalid request URI during seeding: " + uriString);
                                return;
                            }

                            URICustomizer uriCustomizer = new URICustomizer(uri);
                            ApiRequestContext requestContext = new ImmutableRequestContext.Builder()
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
                                    .collect(ImmutableMap.toImmutableMap(collectionId -> collectionId, collectionId -> {
                                        String featureTypeId = apiData.getCollections()
                                                                      .get(collectionId)
                                                                      .getExtension(FeaturesCoreConfiguration.class)
                                                                      .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                                                      .orElse(collectionId);
                                        return ImmutableFeatureQuery.builder()
                                            .from(query)
                                            .type(featureTypeId)
                                            .build();
                                    }));

                            FeaturesCoreConfiguration coreConfiguration = apiData.getExtension(FeaturesCoreConfiguration.class).get();

                            TilesQueriesHandler.QueryInputTileMultiLayer queryInput = new ImmutableQueryInputTileMultiLayer.Builder()
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
        for (ContentExtension contentExtension : extensionRegistry.getExtensionsForType(ContentExtension.class)) {
            if (contentExtension instanceof TileMatrixSet && ((TileMatrixSet) contentExtension).getId().equals(tileMatrixSetId)) {
                tileMatrixSet = (TileMatrixSet) contentExtension;
                break;
            }
        }

        return tileMatrixSet;
    }
}
