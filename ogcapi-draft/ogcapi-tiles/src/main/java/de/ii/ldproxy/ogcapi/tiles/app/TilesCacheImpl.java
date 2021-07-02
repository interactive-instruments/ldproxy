/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ldproxy.ogcapi.features.geojson.domain.SchemaGeneratorGeoJson;
import de.ii.ldproxy.ogcapi.tiles.app.mbtiles.ImmutableMbtilesMetadata;
import de.ii.ldproxy.ogcapi.tiles.app.mbtiles.MbtilesMetadata;
import de.ii.ldproxy.ogcapi.tiles.app.mbtiles.MbtilesTileset;
import de.ii.ldproxy.ogcapi.tiles.domain.Tile;
import de.ii.ldproxy.ogcapi.tiles.domain.TileCache;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSet;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.CACHE_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

/**
 * Access to the cache for tile files.
 */
@Component
@Provides
@Instantiate
public class TilesCacheImpl implements TileCache {

    private static final String TILES_DIR_NAME = "tiles";
    private static final String TMP_DIR_NAME = "__tmp__";
    private static final long TEN_MINUTES = 10 * 60 * 1000;
    private final Path cacheStore;
    private long lastCleanup = System.currentTimeMillis();
    private Map<String, MbtilesTileset> mbtiles;
    private final CrsTransformerFactory crsTransformerFactory;
    private final TileMatrixSetLimitsGenerator limitsGenerator;
    private final SchemaGeneratorGeoJson schemaGeneratorFeature;
    private final FeaturesCoreProviders providers;
    private final SchemaInfo schemaInfo;
    private final ExtensionRegistry extensionRegistry;

    /**
     * set data directory
     */
    public TilesCacheImpl(@Context BundleContext bundleContext,
                          @Requires CrsTransformerFactory crsTransformerFactory,
                          @Requires TileMatrixSetLimitsGenerator limitsGenerator,
                          @Requires SchemaGeneratorGeoJson schemaGeneratorFeature,
                          @Requires FeaturesCoreProviders providers,
                          @Requires SchemaInfo schemaInfo,
                          @Requires ExtensionRegistry extensionRegistry) throws IOException {
        // the ldproxy data directory, in development environment this would be ./build/data
        this.cacheStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), CACHE_DIR)
                               .resolve(TILES_DIR_NAME);
        this.crsTransformerFactory = crsTransformerFactory;
        this.limitsGenerator = limitsGenerator;
        this.schemaGeneratorFeature = schemaGeneratorFeature;
        this.providers = providers;
        this.schemaInfo = schemaInfo;
        this.extensionRegistry = extensionRegistry;
        Files.createDirectories(cacheStore);

        mbtiles = new HashMap<>();

        // TODO move to background task
        cleanup();
    }

    /**
     * generate empty cache files and directories
     */
    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, ValidationResult.MODE apiValidation) {
        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                                                                             .mode(apiValidation);

        Map<String, TileMatrixSet> tileMatrixSets = extensionRegistry.getExtensionsForType(TileMatrixSet.class)
                                                                     .stream()
                                                                     .collect(Collectors.toMap(TileMatrixSet::getId, tms -> tms));
        Optional<TilesConfiguration> config = apiData.getExtension(TilesConfiguration.class);
        if (config.isPresent()
                && config.get().isEnabled()
                && config.get().getMultiCollectionEnabledDerived()
                && config.get().getTileProvider() instanceof TileProviderFeatures) {
            TilesConfiguration.TileCacheType cacheType = config.get().getCache();
            Set<String> tileMatrixSetIds = config.get().getZoomLevelsDerived().keySet();
            for (String tileMatrixSetId : tileMatrixSetIds) {
                TileMatrixSet tileMatrixSet = tileMatrixSets.get(tileMatrixSetId);
                builder = process(builder, apiData, Optional.empty(), tileMatrixSet, cacheType);
            }
        }

        for (String collectionId : apiData.getCollections().keySet()) {
            config = apiData.getExtension(TilesConfiguration.class, collectionId);
            if (config.isPresent()
                    && config.get().isEnabled()
                    && config.get().getTileProvider() instanceof TileProviderFeatures) {
                TilesConfiguration.TileCacheType cacheType = config.get().getCache();
                Set<String> tileMatrixSetIds = config.get().getZoomLevelsDerived().keySet();
                for (String tileMatrixSetId : tileMatrixSetIds) {
                    TileMatrixSet tileMatrixSet = tileMatrixSets.get(tileMatrixSetId);
                    builder = process(builder, apiData, Optional.of(collectionId), tileMatrixSet, cacheType);
                }
           }
        }

        return builder.build();
    }

    private ImmutableValidationResult.Builder process(ImmutableValidationResult.Builder builder,
               OgcApiDataV2 apiData,
               Optional<String> collectionId,
               TileMatrixSet tileMatrixSet,
               TilesConfiguration.TileCacheType cacheType) {
        switch (cacheType) {
            case MBTILES:
                try {
                    getOrInitTileset(apiData, collectionId, tileMatrixSet);
                } catch (IOException e) {
                    builder.addErrors(MessageFormat.format("The Mbtiles container for the tile cache for collection ''{0}'' could not be initialized.", collectionId.orElse("__all__")));
                }
                break;

            default:
            case FILES:
                try {
                    Files.createDirectories(cacheStore.resolve(apiData.getId()).resolve(collectionId.orElse("__all__")).resolve(tileMatrixSet.getId()));
                } catch (IOException e) {
                    builder.addErrors(MessageFormat.format("The folders for the tile cache for collection ''{0}'' could not be initialized.", collectionId.orElse("__all__")));
                }
                break;
        }
        return builder;
    }

    @Override
    public void cleanup() {
        Runnable cleanup = () -> {
            try {
                Path tmpDirectory = getTmpDirectory();
                long cutoff = FileTime.from(Instant.now()).toMillis() - TEN_MINUTES;
                Files.list(tmpDirectory)
                     .filter(path -> {
                         try {
                             return Files.getLastModifiedTime(path).toMillis() <= cutoff;
                         } catch (IOException e) {
                             throw new RuntimeException("Error while cleaning the tile cache.", e);
                         }
                     })
                     .forEach(path -> {
                         try {
                             Files.delete(path);
                         } catch (IOException e) {
                             throw new RuntimeException("Error while cleaning the tile cache.", e);
                         }
                     });
                lastCleanup = System.currentTimeMillis();
            } catch (IOException e) {
                throw new RuntimeException("Error while cleaning the tile cache.", e);
            }
        };

        new Thread(cleanup).start();
    }

    @Override
    public boolean tileExists(Tile tile) throws IOException, SQLException {
        switch (getType(tile)) {
            case MBTILES:
                if (!tile.getTemporary())
                    return getTileset(tile).tileExists(tile);

            case FILES:
            default:
                return Files.exists(getPath(tile));
        }
    }

    @Override
    public Optional<InputStream> getTile(Tile tile) throws IOException, SQLException {
        switch (getType(tile)) {
            case MBTILES:
                if (!tile.getTemporary())
                    return getTileset(tile).getTile(tile);

            case FILES:
            default:
                Path path = getPath(tile);
                if (Files.notExists(path))
                    return Optional.empty();
                return Optional.of(new BufferedInputStream(new FileInputStream(path.toFile())));
        }
    }

    @Override
    public Optional<Boolean> tileIsEmpty(Tile tile) throws IOException, SQLException {
        switch (getType(tile)) {
            case MBTILES:
                if (!tile.getTemporary())
                    return getTileset(tile).tileIsEmpty(tile);

            case FILES:
            default:
                Path path = getPath(tile);
                if (Files.notExists(path))
                    return Optional.empty();
                return Optional.of(Files.size(path)==0);
        }
    }

    @Override
    public void deleteTile(Tile tile) throws IOException, SQLException {
        switch (getType(tile)) {
            case MBTILES:
                if (!tile.getTemporary()) {
                    getTileset(tile).deleteTile(tile);
                    break;
                }

            case FILES:
            default:
                Path path = getPath(tile);
                    Files.delete(path);
                break;
        }
    }

    @Override
    public void storeTile(Tile tile, byte[] content) throws IOException, SQLException {
        switch (getType(tile)) {
            case MBTILES:
                if (!tile.getTemporary()) {
                    getTileset(tile).writeTile(tile, content);
                    break;
                }

            case FILES:
            default:
                Path path = getPath(tile);
                if (Files.notExists(path) || Files.isWritable(path)) {
                    Files.write(path, content);
                }
                break;
        }
    }

    /**
     * return and if necessary create the directory for the tiles cache
     * @return the file object of the directory
     */
    private Path getTilesStore() {
        return cacheStore;
    }

    /**
     * return and if necessary create the directory for the tile files that cannot be cached
     * @return the file object of the directory
     */
    private Path getTmpDirectory() throws IOException {
        Path tmpDirectory = cacheStore.resolve(TMP_DIR_NAME);
        Files.createDirectories(tmpDirectory);
        return tmpDirectory;
    }

    /**
     * identifies the type of the cache for this tile (set)
     * @param tile a tile in a tile set
     * @return {@code MBTILES} or {@code FILES} (the default)
     */
    private TilesConfiguration.TileCacheType getType(Tile tile) {
        OgcApiDataV2 apiData = tile.getApi().getData();
        return (tile.isDatasetTile()
                ? apiData.getExtension(TilesConfiguration.class)
                : apiData.getExtension(TilesConfiguration.class, tile.getCollectionId())).map(cfg -> cfg.getCache())
                                                                                         .orElse(TilesConfiguration.TileCacheType.FILES);
    }

    /**
     * MBTILES: open an existing Mbtiles cache file
     * @param tile the tile
     * @return the Tileset
     */
    private MbtilesTileset getTileset(Tile tile) throws IOException {
        if (getType(tile)!= TilesConfiguration.TileCacheType.MBTILES)
            throw new IllegalStateException(String.format("Cannot get an Mbtiles cache. Found cache type: %s", getType(tile).toString()));
        OgcApiDataV2 apiData = tile.getApi().getData();
        Optional<String> collectionId = tile.isDatasetTile() ? Optional.empty() : Optional.of(tile.getCollectionId());
        TileMatrixSet tileMatrixSet = tile.getTileMatrixSet();
        return getOrInitTileset(apiData, collectionId, tileMatrixSet);
    }

    /**
     * MBTILES: create a new, empty Mbtiles cache file
     * @param apiData the API
     * @param collectionId the collection; an empty value represents the dataset
     * @param tileMatrixSet the tile matrix set
     * @return the Tileset
     */
    private MbtilesTileset getOrInitTileset(OgcApiDataV2 apiData, Optional<String> collectionId, TileMatrixSet tileMatrixSet) throws IOException {
        String apiId = apiData.getId();
        String tileMatrixSetId = tileMatrixSet.getId();
        String key = String.join("/", apiId, collectionId.orElse("__all__"), tileMatrixSetId);
        if (!mbtiles.containsKey(key)) {
            Files.createDirectories(cacheStore.resolve(apiId).resolve(collectionId.orElse("__all__")));
            Path path = cacheStore.resolve(apiId).resolve(collectionId.orElse("__all__")).resolve(tileMatrixSetId+".mbtiles");
            if (Files.exists(path)) {
                mbtiles.put(key, new MbtilesTileset(path));
            } else {
                TilesConfiguration config = collectionId.isEmpty()
                        ? apiData.getExtension(TilesConfiguration.class).get()
                        : apiData.getExtension(TilesConfiguration.class, collectionId.get()).get();

                // get the tile set metadata
                TileSet tileSetMetadata = TilesHelper.buildTileSet(apiData,
                                                                   tileMatrixSet,
                                                                   config.getZoomLevelsDerived().get(tileMatrixSetId),
                                                                   config.getCenterDerived(),
                                                                   collectionId,
                                                                   ImmutableList.of(),
                                                                   Optional.empty(),
                                                                   crsTransformerFactory,
                                                                   limitsGenerator,
                                                                   schemaGeneratorFeature);

                // convert to Mbtiles metadata
                // TODO support attribution, type, version
                MbtilesMetadata md = ImmutableMbtilesMetadata.builder()
                                                             .name(apiData.getLabel())
                                                             .format(MbtilesMetadata.MbtilesFormat.pbf)
                                                             .description(apiData.getDescription())
                                                             .minzoom(TilesHelper.getMinzoom(tileSetMetadata))
                                                             .maxzoom(TilesHelper.getMaxzoom(tileSetMetadata))
                                                             .bounds(TilesHelper.getBounds(tileSetMetadata))
                                                             .center(TilesHelper.getCenter(tileSetMetadata))
                                                             .vectorLayers(TilesHelper.getVectorLayers(apiData,
                                                                                                       collectionId,
                                                                                                       tileMatrixSet.getId(),
                                                                                                       providers,
                                                                                                       schemaInfo))
                                                             .build();
                mbtiles.put(key, new MbtilesTileset(path, md));
            }
        }
        return mbtiles.get(key);
    }

    /**
     * FILES cache or temporary tiles: determine the file path of a tile
     * @param tile the tile
     * @return the file path
     */
    private Path getPath(Tile tile) throws IOException {
        Path subDir;
        if (tile.getTemporary()) {
            subDir = getTmpDirectory();
            if (FileTime.from(Instant.now()).toMillis() - lastCleanup > TEN_MINUTES)
                cleanup();
        } else {
            subDir = getTilesStore().resolve(tile.getApi().getId())
                                    .resolve(tile.isDatasetTile() ? "__all__" : tile.getCollectionId())
                                    .resolve(tile.getTileMatrixSet().getId());
        }

        Path path = subDir.resolve(tile.getRelativePath());
        Files.createDirectories(path.getParent());
        return path;
    }
}
