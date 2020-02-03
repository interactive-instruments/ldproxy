/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiStartupTask;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.ldproxy.target.geojson.OgcApiFeaturesOutputFormatGeoJson;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.CrsTransformerFactory;
import de.ii.xtraplatform.feature.provider.api.FeatureProvider2;
import de.ii.xtraplatform.server.CoreServerConfig;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

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
    private final VectorTilesCache cache;
    private Thread t = null;
    private Map<Thread, String> threadMap = new HashMap<>();

    @Requires
    I18n i18n;

    @Requires
    private CrsTransformerFactory crsTransformerFactory;

    @Requires
    private CoreServerConfig coreServerConfig;

    @Requires
    private OgcApiExtensionRegistry extensionRegistry;

    private final VectorTileMapGenerator vectorTileMapGenerator = new VectorTileMapGenerator();

    public VectorTileSeeding(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) {
        String dataDirectory = bundleContext.getProperty(DATA_DIR_KEY);
        cache = new VectorTilesCache(dataDirectory);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    /**
     * The runnable Task which starts the seeding.
     *
     * @param api               the API
     * @param featureProvider   the featureProvider
     * @return the runnable process
     */
    @Override
    public Runnable getTask(OgcApiApi api, FeatureProvider2 featureProvider) {

        Optional<OgcApiFeatureFormatExtension> wfs3OutputFormatGeoJson = getOutputFormatForType(OgcApiFeaturesOutputFormatGeoJson.MEDIA_TYPE);
        OgcApiApiDataV2 apiData = api.getData();

        if (!wfs3OutputFormatGeoJson.isPresent()) {
            return () -> {
            };
        }

        Runnable startSeeding = () -> {

            Set<String> collectionIdsDataset = Wfs3EndpointTiles.getCollectionIdsDataset(vectorTileMapGenerator.getAllCollectionIdsWithTileExtension(apiData), vectorTileMapGenerator.getEnabledMap(apiData),
                    vectorTileMapGenerator.getFormatsMap(apiData), vectorTileMapGenerator.getMinMaxMap(apiData, true), false, false, true);
            try {
                boolean tilesDatasetEnabled = false;
                boolean seedingDatasetEnabled = false;

                if (!collectionIdsDataset.isEmpty())
                    tilesDatasetEnabled = true;


                if (tilesDatasetEnabled) {
                    for (String collectionId : collectionIdsDataset) {
                        if (isExtensionEnabled(apiData, apiData.getCollections()
                                                                       .get(collectionId), TilesConfiguration.class)) {

                            final TilesConfiguration tilesConfiguration = getExtensionConfiguration(apiData, apiData.getCollections()
                                                                                                                            .get(collectionId), TilesConfiguration.class).get();

                            Map<String, MinMax> seedingCollection = tilesConfiguration.getSeeding();

                            if (seedingCollection != null) {
                                seedingDatasetEnabled = true;
                                break;
                            }
                        }

                    }
                }

                if (tilesDatasetEnabled && seedingDatasetEnabled) {
                    seedingDataset(collectionIdsDataset, api, crsTransformerFactory, cache, featureProvider, coreServerConfig, wfs3OutputFormatGeoJson.get(), Optional.of(Locale.ENGLISH));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

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
     * Computes the minimum and maximum row and col values.
     * <p>
     * Generates all JSON Tiles and MVT (if the format is enabled) for each collection (MVT support has to be enabled)
     * for every tile matrix set in the specified seeding range
     *
     * @param collectionIdsDataset all ids of feature Types which have the tiles support and seeding enabled
     * @param service              the Wfs3 service
     * @param crsTransformerFactory    the coordinate reference system transformation object to transform coordinates
     * @param cache                the vector tile cache
     * @param featureProvider      the feature Provider
     * @param coreServerConfig     the core server config with the external url
     * @throws FileNotFoundException
     */
    private void seedingDataset(Set<String> collectionIdsDataset, OgcApiApi service,
                                CrsTransformerFactory crsTransformerFactory, VectorTilesCache cache,
                                FeatureProvider2 featureProvider, CoreServerConfig coreServerConfig,
                                OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson, Optional<Locale> language)
            throws FileNotFoundException {

        Set<String> tileMatrixSetIdsCollection = null;
        OgcApiApiDataV2 datasetData = service.getData();
        Map<String, Map<String, MinMax>> seedingMap = vectorTileMapGenerator.getMinMaxMap(datasetData, true);

        Map<String,Map<String,File>> fileMap = new HashMap<>();
        // first generate GeoJSON/MVT tiles for all the collections and tiling schemes
        for (String collectionId : collectionIdsDataset) {
            if (!Objects.isNull(seedingMap) && seedingMap.containsKey(collectionId)) {
                Map<String, MinMax> seeding = seedingMap.get(collectionId);
                tileMatrixSetIdsCollection = seeding.keySet();
                for (String tileMatrixSetId : tileMatrixSetIdsCollection) {
                    TileMatrixSet tileMatrixSet = TileMatrixSetCache.getTileMatrixSet(tileMatrixSetId);
                    if (seeding.size() != 0) {
                        int maxZoom = seeding.get(tileMatrixSetId)
                                .getMax();
                        int minZoom = seeding.get(tileMatrixSetId)
                                .getMin();
                        for (int level = minZoom; level <= maxZoom; level++) {
                            BoundingBox bbox = null;
                            try {
                                bbox = datasetData.getSpatialExtent(collectionId, crsTransformerFactory, tileMatrixSet.getCrs());
                                TileMatrixSetLimits limits = tileMatrixSet.getLimits(level, bbox);
                                int rowMin = limits.getMinTileRow();
                                int rowMax = limits.getMaxTileRow();
                                int colMin = limits.getMinTileCol();
                                int colMax = limits.getMaxTileCol();

                                for (int row = rowMin; row <= rowMax; row++) {
                                    for (int col = colMin; col <= colMax; col++) {

                                        String tileKey = String.format("%s;%s;%s;%s", tileMatrixSetId, Integer.toString(level), Integer.toString(row), Integer.toString(col));
                                        if (!fileMap.containsKey(tileKey)) {
                                            fileMap.put(tileKey, new HashMap<String, File>());
                                        }

                                        // generates both GeoJSON and MVT files
                                        generateMVT(service, collectionId, tileMatrixSetId, level, row, col, cache, crsTransformerFactory, featureProvider, coreServerConfig, wfs3OutputFormatGeoJson, language);

                                        VectorTile tile = new VectorTile(collectionId, tileMatrixSetId, Integer.toString(level), Integer.toString(row), Integer.toString(col), service, false, cache, featureProvider, wfs3OutputFormatGeoJson);
                                        File tileFileJson = tile.getFile(cache, "json");
                                        if (tileFileJson!=null) {
                                            Map<String,File> layers = fileMap.get(tileKey);
                                            layers.put(collectionId, tileFileJson);
                                        }
                                    }
                                }
                            } catch (CrsTransformationException e) {
                                // skip tiles and report them in the log
                                LOGGER.error(String.format("Could not seed tiles due to a CRS transformation error: scheme=%s level=%s", tileMatrixSetId, Integer.toString(level)));
                            }
                        }
                    }
                }
            }
        }

        // now generate the multi-collection tiles
        for (Map.Entry<String, Map<String, File>> entry : fileMap.entrySet()) {
            String tileKey = entry.getKey();
            String[] keys = tileKey.split(";");
            String tileMatrixSetId = keys[0];
            int level = Integer.parseInt(keys[1]);
            int row = Integer.parseInt(keys[2]);
            int col = Integer.parseInt(keys[3]);

            Map<String, File> layers = entry.getValue();
            VectorTile tile = new VectorTile(null, tileMatrixSetId, Integer.toString(level), Integer.toString(row), Integer.toString(col), service, false, cache, featureProvider, wfs3OutputFormatGeoJson);
            File tileFileMvt = tile.getFile(cache, "pbf");
            boolean success = TileGeneratorMvt.generateTileMvt(tileFileMvt, layers, null, crsTransformerFactory, tile, true);
            if (!success) {
                // skip tiles and report them in the log
                String msg = "Internal server error: could not generate protocol buffer for a tile.";
                LOGGER.error(msg);
            }
        }
    }

    /**
     * generates the GeoJSON and MVT files for the specified parameters
     *
     * @param service           the service data of the Wfs3 Service
     * @param collectionId      the id of the collection of the tile
     * @param tileMatrixSetId   the id of the tile matrix set of the tile
     * @param level             the zoom level of the tile
     * @param row               the row of the tile
     * @param col               the col of the tile
     * @param cache             the vector tile cache
     * @param crsTransformerFactory the coordinate reference system transformation object to transform coordinates
     * @param featureProvider   the feature Provider
     * @param coreServerConfig  the core server config with the external url
     * @return the Json File. If the mvt already exists, return null
     */
    private File generateMVT(OgcApiApi service, String collectionId, String tileMatrixSetId, int level, int row,
                             int col, VectorTilesCache cache, CrsTransformerFactory crsTransformerFactory,
                             FeatureProvider2 featureProvider, CoreServerConfig coreServerConfig,
                             OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson, Optional<Locale> language) {

        VectorTile tile = new VectorTile(collectionId, tileMatrixSetId, Integer.toString(level), Integer.toString(row), Integer.toString(col), service, false, cache, featureProvider, wfs3OutputFormatGeoJson);
        File tileFileMvt = tile.getFile(cache, "pbf");
        if (!tileFileMvt.exists()) {
            LOGGER.debug("seeding - " + collectionId + " | " + tileMatrixSetId + " | level: " + level + " | row: " + row + " | col: " + col + " | format: MVT");
            File tileFileJson = generateJSON(service, collectionId, tileMatrixSetId, level, row, col, cache, crsTransformerFactory, featureProvider, coreServerConfig, wfs3OutputFormatGeoJson, language);
            Map<String, File> layers = new HashMap<>();
            layers.put(collectionId, tileFileJson);
            boolean success = TileGeneratorMvt.generateTileMvt(tileFileMvt, layers, null, crsTransformerFactory, tile, true);
            if (!success) {
                // skip the tile and report error in the log
                String msg = "Internal server error: could not generate protocol buffers for a tile.";
                LOGGER.error(msg);
            }
            return tileFileJson;
        }

        return null;
    }

    /**
     * generates the JSON Tile for the specified parameters
     *
     * @param service           the service data of the Wfs3 Service
     * @param collectionId      the id of the collection of the tile
     * @param tileMatrixSetId   the id of the tile matrix set of the tile
     * @param level             the zoom level of the tile
     * @param row               the row of the tile
     * @param col               the col of the tile
     * @param cache             the vector tile cache
     * @param crsTransformerFactory the coordinate reference system transformation object to transform coordinates
     * @param featureProvider   the feature Provider
     * @param coreServerConfig  the core server config with the external url
     * @return the json File, if it already exists return null
     */
    private File generateJSON(OgcApiApi service, String collectionId, String tileMatrixSetId, int level, int row,
                              int col, VectorTilesCache cache, CrsTransformerFactory crsTransformerFactory,
                              FeatureProvider2 featureProvider, CoreServerConfig coreServerConfig,
                              OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson, Optional<Locale> language) {

        VectorTile tile = new VectorTile(collectionId, tileMatrixSetId, Integer.toString(level), Integer.toString(row), Integer.toString(col), service, false, cache, featureProvider, wfs3OutputFormatGeoJson);
        File tileFileJson = tile.getFile(cache, "json");

        if (!tileFileJson.exists()) {
            LOGGER.debug("seeding - " + collectionId + " | " + tileMatrixSetId + " | level: " + level + " | row: " + row + " | col: " + col + " | format: JSON");
            String prefix = coreServerConfig.getExternalUrl();
            String uriString = prefix + "/" + service.getData().getId() + "/" + "collections" + "/"
                    + collectionId + "/tiles/" + tileMatrixSetId + "/" + level + "/" + row + "/" + col;

            URI uri = null;
            try {
                uri = new URI(uriString);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            URICustomizer uriCustomizer = new URICustomizer(uri);

            OgcApiMediaType mediaType;
            mediaType = new ImmutableOgcApiMediaType.Builder()
                    .type(new MediaType("application", "json"))
                    .label("JSON")
                    .build();
            TileGeneratorJson.generateTileJson(tileFileJson, crsTransformerFactory, null, null, null, uriCustomizer, mediaType, true, tile, i18n, language);
        }
        return tileFileJson;
    }

    private Optional<OgcApiFeatureFormatExtension> getOutputFormatForType(OgcApiMediaType mediaType) {
        return extensionRegistry.getExtensionsForType(OgcApiFeatureFormatExtension.class)
                                .stream()
                                .filter(wfs3OutputFormatExtension -> wfs3OutputFormatExtension.getMediaType()
                                                                                              .equals(mediaType))
                                .findFirst();
    }
}
