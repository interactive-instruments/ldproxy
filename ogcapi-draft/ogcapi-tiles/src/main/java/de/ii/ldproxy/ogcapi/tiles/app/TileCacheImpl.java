/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import static de.ii.ldproxy.ogcapi.domain.FoundationConfiguration.CACHE_DIR;
import static de.ii.xtraplatform.runtime.domain.Constants.DATA_DIR_KEY;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.common.domain.metadata.CollectionDynamicMetadataRegistry;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ldproxy.ogcapi.tiles.app.mbtiles.ImmutableMbtilesMetadata;
import de.ii.ldproxy.ogcapi.tiles.app.mbtiles.MbtilesMetadata;
import de.ii.ldproxy.ogcapi.tiles.app.mbtiles.MbtilesTileset;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.Tile;
import de.ii.ldproxy.ogcapi.tiles.domain.TileCache;
import de.ii.ldproxy.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSet;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Access to the cache for tile files.
 */
@Component
@Provides
@Instantiate
public class TileCacheImpl implements TileCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(TileCacheImpl.class);
    private static final String TILES_DIR_NAME = "tiles";
    private static final String TMP_DIR_NAME = "__tmp__";
    private static final long TEN_MINUTES = 10 * 60 * 1000;
    private final Path cacheStore;
    private long lastCleanup = System.currentTimeMillis();
    private final Map<String, MbtilesTileset> mbtiles;
    private final TileMatrixSetLimitsGenerator limitsGenerator;
    private final FeaturesCoreProviders providers;
    private final SchemaInfo schemaInfo;
    private final ExtensionRegistry extensionRegistry;
    private final EntityRegistry entityRegistry;
    private final TileMatrixSetRepository tileMatrixSetRepository;
    private final CollectionDynamicMetadataRegistry metadataRegistry;

    /**
     * set data directory
     */
    public TileCacheImpl(@Context BundleContext bundleContext,
                         @Requires TileMatrixSetLimitsGenerator limitsGenerator,
                         @Requires FeaturesCoreProviders providers,
                         @Requires SchemaInfo schemaInfo,
                         @Requires ExtensionRegistry extensionRegistry,
                         @Requires EntityRegistry entityRegistry,
                         @Requires TileMatrixSetRepository tileMatrixSetRepository,
                         @Requires CollectionDynamicMetadataRegistry metadataRegistry) throws IOException {
        // the ldproxy data directory, in development environment this would be ./build/data
        this.cacheStore = Paths.get(bundleContext.getProperty(DATA_DIR_KEY), CACHE_DIR)
                               .resolve(TILES_DIR_NAME);
        this.limitsGenerator = limitsGenerator;
        this.providers = providers;
        this.schemaInfo = schemaInfo;
        this.extensionRegistry = extensionRegistry;
        this.entityRegistry = entityRegistry;
        this.tileMatrixSetRepository = tileMatrixSetRepository;
        this.metadataRegistry = metadataRegistry;
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

        Map<String, TileMatrixSet> tileMatrixSets = tileMatrixSetRepository.getAll();
        Optional<TilesConfiguration> config = apiData.getExtension(TilesConfiguration.class);
        if (config.isPresent()
                && config.get().isEnabled()
                && config.get().isMultiCollectionEnabled()
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
    public void deleteTiles(OgcApiDataV2 apiData, Optional<String> collectionId,
                            Optional<String> tileMatrixSetId, Optional<BoundingBox> boundingBox) throws IOException, SQLException {
        LOGGER.debug("Purging tile cache for collection '{}', tiling scheme '{}', bounding box '{}'",
                     collectionId.orElse("*"), tileMatrixSetId.orElse("*"),
                     boundingBox.isEmpty() ? "*" : String.format(Locale.US, "%f,%f,%f,%f",
                                                                 boundingBox.get().getXmin(), boundingBox.get().getYmin(),
                                                                 boundingBox.get().getXmax(), boundingBox.get().getYmax()));

        Optional<TilesConfiguration> config = collectionId.isEmpty()
                ? apiData.getExtension(TilesConfiguration.class)
                : apiData.getExtension(TilesConfiguration.class, collectionId.get());
        if (config.isEmpty())
            return;

        Map<String, MinMax> zoomLevels = config.get().getZoomLevelsDerived();
        for (Map.Entry<String, MinMax> tileset : zoomLevels.entrySet()) {
            if (tileMatrixSetId.isPresent() && !tileMatrixSetId.get().equals(tileset.getKey()))
                continue;

            TileMatrixSet tileMatrixSet = getTileMatrixSetById(tileset.getKey());
            MinMax levels = tileset.getValue();

            BoundingBox bbox = boundingBox.orElseGet(() -> collectionId.isEmpty()
                    ? metadataRegistry.getSpatialExtent(apiData.getId())
                             .orElse(tileMatrixSet.getBoundingBox())
                    : metadataRegistry.getSpatialExtent(apiData.getId(), collectionId.get())
                             .orElse(tileMatrixSet.getBoundingBox()));

            // first the dataset tiles
            deleteTiles(apiData, Optional.empty(), tileMatrixSet, levels, bbox);

            if (collectionId.isPresent()) {
                // also the single collection tiles for the collection
                deleteTiles(apiData, collectionId, tileMatrixSet, levels, bbox);
            } else {
                // all single collection tiles
                for (String colId : apiData.getCollections()
                                           .keySet()) {
                    deleteTiles(apiData, Optional.of(colId), tileMatrixSet, levels, bbox);
                }
            }
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
        return getType(tile.getApiData(), tile.isDatasetTile() ? Optional.empty() : Optional.of(tile.getCollectionId()));
    }

    /**
     * identifies the type of the cache for this tile (set)
     * @param apiData the API
     * @param collectionId the collection, empty=dataset
     * @return {@code MBTILES} or {@code FILES} (the default)
     */
    private TilesConfiguration.TileCacheType getType(OgcApiDataV2 apiData, Optional<String> collectionId) {
        return (collectionId.isEmpty()
                ? apiData.getExtension(TilesConfiguration.class)
                : apiData.getExtension(TilesConfiguration.class, collectionId.get())).map(cfg -> cfg.getCache())
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
        OgcApiDataV2 apiData = tile.getApiData();
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
                                                                   limitsGenerator,
                                                                   providers,
                                                                   entityRegistry,
                                                                   metadataRegistry);

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
            subDir = getTilesStore().resolve(tile.getApiData().getId())
                                    .resolve(tile.isDatasetTile() ? "__all__" : tile.getCollectionId())
                                    .resolve(tile.getTileMatrixSet().getId());
        }

        Path path = subDir.resolve(tile.getRelativePath());
        Files.createDirectories(path.getParent());
        return path;
    }

    private TileMatrixSet getTileMatrixSetById(String tileMatrixSetId) {
        return tileMatrixSetRepository.get(tileMatrixSetId)
                                      .orElseThrow(() -> new IllegalArgumentException("TileMatrixSet not found: "+tileMatrixSetId));
    }

    private List<TileMatrixSetLimits> getLimits(OgcApiDataV2 apiData, TileMatrixSet tileMatrixSet, MinMax minmax, Optional<String> collectionId, BoundingBox bbox) {
        return limitsGenerator.getTileMatrixSetLimits(bbox, tileMatrixSet, minmax);
    }

    private List<TileFormatExtension> getTileFormats(OgcApiDataV2 apiData, Optional<String> collectionId) {
        Optional<TilesConfiguration> config = collectionId.isEmpty()
                ? apiData.getExtension(TilesConfiguration.class)
                : apiData.getExtension(TilesConfiguration.class, collectionId.get());
        if (config.isEmpty())
            return ImmutableList.of();
        return extensionRegistry.getExtensionsForType(TileFormatExtension.class)
                                .stream()
                                .filter(format -> collectionId.map(s -> format.isEnabledForApi(apiData, s)).orElseGet(() -> format.isEnabledForApi(apiData)))
                                .filter(format -> config.get().getTileEncodingsDerived() == null || config.get().getTileEncodingsDerived().isEmpty() || config.get().getTileEncodingsDerived().contains(format.getMediaType().label()))
                                .collect(Collectors.toList());

    }

    private void deleteTiles(OgcApiDataV2 apiData, Optional<String> collectionId,
                             TileMatrixSet tileMatrixSet, MinMax levels, BoundingBox bbox) throws SQLException, IOException {
        switch (getType(apiData, collectionId)) {
            case MBTILES:
                deleteTilesMbtiles(apiData, collectionId, tileMatrixSet, levels, bbox);
                break;
            case FILES:
                deleteTilesFiles(apiData, collectionId, getTileFormats(apiData, collectionId), tileMatrixSet, levels, bbox);
                break;
        }
    }

    private void deleteTilesMbtiles(OgcApiDataV2 apiData, Optional<String> collectionId,
                                    TileMatrixSet tileMatrixSet, MinMax levels, BoundingBox bbox) throws SQLException, IOException {
        MbtilesTileset tileset = getOrInitTileset(apiData, collectionId, tileMatrixSet);
        List<TileMatrixSetLimits> limitsList = getLimits(apiData, tileMatrixSet, levels, collectionId, bbox);
        for (TileMatrixSetLimits limits : limitsList) {
            LOGGER.trace("Deleting tiles from Mbtiles cache: API {}, collection {}, tiles {}/{}/{}-{}/{}-{}, TMS rows {}-{}",
                         apiData.getId(), collectionId.orElse("all"), tileMatrixSet.getId(),
                         limits.getTileMatrix(), limits.getMinTileRow(), limits.getMaxTileRow(),
                         limits.getMinTileCol(), limits.getMaxTileCol(),
                         tileMatrixSet.getTmsRow(Integer.parseInt(limits.getTileMatrix()), limits.getMaxTileRow()),
                         tileMatrixSet.getTmsRow(Integer.parseInt(limits.getTileMatrix()), limits.getMinTileRow()));
            tileset.deleteTiles(tileMatrixSet, limits);
        }
    }

    private void deleteTilesFiles(OgcApiDataV2 apiData, Optional<String> collectionId, List<TileFormatExtension> outputFormats,
                                  TileMatrixSet tileMatrixSet, MinMax levels, BoundingBox bbox) throws SQLException, IOException {
        List<TileMatrixSetLimits> limitsList = getLimits(apiData, tileMatrixSet, levels, collectionId, bbox);
        for (TileMatrixSetLimits limits : limitsList) {
            LOGGER.trace("Deleting tiles from file cache: API {}, collection {}, tiles {}/{}/{}-{}/{}-{}, extensions {}",
                         apiData.getId(), collectionId.orElse("all"), tileMatrixSet.getId(),
                         limits.getTileMatrix(), limits.getMinTileRow(), limits.getMaxTileRow(),
                         limits.getMinTileCol(), limits.getMaxTileCol(),
                         outputFormats.stream().map(TileFormatExtension::getExtension).collect(Collectors.joining("/")));
            for (int row=limits.getMinTileRow(); row<=limits.getMaxTileRow(); row++) {
                for (int col=limits.getMinTileCol(); col<=limits.getMaxTileCol(); col++) {
                    for (TileFormatExtension outputFormat: outputFormats) {
                        Path path = getTilesStore().resolve(apiData.getId())
                                                   .resolve(collectionId.orElse("__all__"))
                                                   .resolve(tileMatrixSet.getId())
                                                   .resolve(limits.getTileMatrix())
                                                   .resolve(String.valueOf(row))
                                                   .resolve(String.join(".", String.valueOf(col), outputFormat.getExtension()));
                        Files.deleteIfExists(path);
                    }
                }
            }
        }
    }
}
