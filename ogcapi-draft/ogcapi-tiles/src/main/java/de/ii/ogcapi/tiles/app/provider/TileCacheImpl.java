/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import static de.ii.ogcapi.foundation.domain.FoundationConfiguration.CACHE_DIR;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tilematrixsets.domain.MinMax;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimits;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetRepository;
import de.ii.ogcapi.tiles.app.TilesHelper;
import de.ii.ogcapi.tiles.app.mbtiles.ImmutableMbtilesMetadata;
import de.ii.ogcapi.tiles.app.mbtiles.MbtilesMetadata;
import de.ii.ogcapi.tiles.app.mbtiles.MbtilesTileset;
import de.ii.ogcapi.tiles.domain.Tile;
import de.ii.ogcapi.tiles.domain.TileCache;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileFormatWithQuerySupportExtension;
import de.ii.ogcapi.tiles.domain.TileSet;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Access to the cache for tile files. */
@Singleton
@AutoBind
public class TileCacheImpl implements TileCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileCacheImpl.class);
  static final String TILES_DIR_NAME = "tiles";
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
  private final CrsTransformerFactory crsTransformerFactory;

  /** set data directory */
  @Inject
  public TileCacheImpl(
      AppContext appContext,
      TileMatrixSetLimitsGenerator limitsGenerator,
      FeaturesCoreProviders providers,
      SchemaInfo schemaInfo,
      ExtensionRegistry extensionRegistry,
      EntityRegistry entityRegistry,
      TileMatrixSetRepository tileMatrixSetRepository,
      CrsTransformerFactory crsTransformerFactory) {
    // the ldproxy data directory, in development environment this would be ./build/data
    this.cacheStore = appContext.getDataDir().resolve(CACHE_DIR).resolve(TILES_DIR_NAME);
    this.limitsGenerator = limitsGenerator;
    this.providers = providers;
    this.schemaInfo = schemaInfo;
    this.extensionRegistry = extensionRegistry;
    this.entityRegistry = entityRegistry;
    this.tileMatrixSetRepository = tileMatrixSetRepository;
    this.crsTransformerFactory = crsTransformerFactory;
    this.mbtiles = new HashMap<>();
  }

  /** generate empty cache files and directories */
  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    OgcApiDataV2 apiData = api.getData();
    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    try {
      Files.createDirectories(cacheStore);
    } catch (IOException e) {
      builder.addErrors(e.getMessage());
    }

    // TODO move to background task
    cleanup();

    Map<String, TileMatrixSet> tileMatrixSets = tileMatrixSetRepository.getAll();
    Optional<TilesConfiguration> config = apiData.getExtension(TilesConfiguration.class);
    if (config.isPresent()
        && config.get().isEnabled()
        && config.get().isMultiCollectionEnabled()
        && config.get().getTileProvider()
            instanceof de.ii.ogcapi.tiles.domain.TileProviderFeatures) {
      TilesConfiguration.TileCacheType cacheType = config.get().getCache();
      Set<String> tileMatrixSetIds = config.get().getZoomLevelsDerived().keySet();
      for (String tileMatrixSetId : tileMatrixSetIds) {
        TileMatrixSet tileMatrixSet = tileMatrixSets.get(tileMatrixSetId);
        builder = process(builder, api, Optional.empty(), tileMatrixSet, cacheType);
      }
    }

    for (String collectionId : apiData.getCollections().keySet()) {
      config = apiData.getExtension(TilesConfiguration.class, collectionId);
      if (config.isPresent()
          && config.get().isEnabled()
          && config.get().getTileProvider()
              instanceof de.ii.ogcapi.tiles.domain.TileProviderFeatures) {
        TilesConfiguration.TileCacheType cacheType = config.get().getCache();
        Set<String> tileMatrixSetIds = config.get().getZoomLevelsDerived().keySet();
        for (String tileMatrixSetId : tileMatrixSetIds) {
          TileMatrixSet tileMatrixSet = tileMatrixSets.get(tileMatrixSetId);
          builder = process(builder, api, Optional.of(collectionId), tileMatrixSet, cacheType);
        }
      }
    }

    return builder.build();
  }

  private ImmutableValidationResult.Builder process(
      ImmutableValidationResult.Builder builder,
      OgcApi api,
      Optional<String> collectionId,
      TileMatrixSet tileMatrixSet,
      TilesConfiguration.TileCacheType cacheType) {
    switch (cacheType) {
      case MBTILES:
        try {
          getOrInitTileset(api, collectionId, tileMatrixSet);
        } catch (IOException e) {
          builder.addErrors(
              MessageFormat.format(
                  "The Mbtiles container for the tile cache for collection ''{0}'' could not be initialized.",
                  collectionId.orElse("__all__")));
        }
        break;

      default:
      case FILES:
        try {
          Files.createDirectories(
              cacheStore
                  .resolve(api.getId())
                  .resolve(collectionId.orElse("__all__"))
                  .resolve(tileMatrixSet.getId()));
        } catch (IOException e) {
          builder.addErrors(
              MessageFormat.format(
                  "The folders for the tile cache for collection ''{0}'' could not be initialized.",
                  collectionId.orElse("__all__")));
        }
        break;
    }
    return builder;
  }

  @Override
  public void cleanup() {
    Runnable cleanup =
        () -> {
          try {
            Path tmpDirectory = getTmpDirectory();
            long cutoff = FileTime.from(Instant.now()).toMillis() - TEN_MINUTES;
            Files.list(tmpDirectory)
                .filter(
                    path -> {
                      try {
                        return Files.getLastModifiedTime(path).toMillis() <= cutoff;
                      } catch (IOException e) {
                        throw new RuntimeException("Error while cleaning the tile cache.", e);
                      }
                    })
                .forEach(
                    path -> {
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
        if (!tile.getTemporary()) return getTileset(tile).tileExists(tile);

      case FILES:
      default:
        return Files.exists(getPath(tile));
    }
  }

  @Override
  public Optional<InputStream> getTile(Tile tile) throws IOException, SQLException {
    switch (getType(tile)) {
      case MBTILES:
        if (!tile.getTemporary()) return getTileset(tile).getTile(tile);

      case FILES:
      default:
        Path path = getPath(tile);
        if (Files.notExists(path)) return Optional.empty();
        return Optional.of(new BufferedInputStream(new FileInputStream(path.toFile())));
    }
  }

  @Override
  public Optional<Boolean> tileIsEmpty(Tile tile) throws IOException, SQLException {
    switch (getType(tile)) {
      case MBTILES:
        if (!tile.getTemporary()) return getTileset(tile).tileIsEmpty(tile);

      case FILES:
      default:
        Path path = getPath(tile);
        if (Files.notExists(path)) return Optional.empty();
        return Optional.of(Files.size(path) == 0);
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
  public void deleteTiles(
      OgcApi api,
      Optional<String> collectionId,
      Optional<String> tileMatrixSetId,
      Optional<BoundingBox> boundingBox)
      throws IOException, SQLException {
    LOGGER.info(
        "Purging tile cache for collection '{}', tiling scheme '{}', bounding box '{}'",
        collectionId.orElse("*"),
        tileMatrixSetId.orElse("*"),
        boundingBox.isEmpty()
            ? "*"
            : String.format(
                Locale.US,
                "%f,%f,%f,%f",
                boundingBox.get().getXmin(),
                boundingBox.get().getYmin(),
                boundingBox.get().getXmax(),
                boundingBox.get().getYmax()));

    OgcApiDataV2 apiData = api.getData();
    Optional<TilesConfiguration> config =
        collectionId.isEmpty()
            ? apiData.getExtension(TilesConfiguration.class)
            : apiData.getExtension(TilesConfiguration.class, collectionId.get());
    if (config.isEmpty()) return;

    Map<String, MinMax> zoomLevels = config.get().getZoomLevelsDerived();

    Map<String, MinMax> relevantZoomLevels =
        tileMatrixSetId
            .map(
                tmsId ->
                    zoomLevels.entrySet().stream()
                        .filter(entry -> Objects.equals(tmsId, entry.getKey()))
                        .collect(
                            Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(zoomLevels);

    Map<String, BoundingBox> relevantBoundingBoxes =
        relevantZoomLevels.keySet().stream()
            .map(
                tmsId -> {
                  TileMatrixSet tileMatrixSet = getTileMatrixSetById(tmsId);
                  BoundingBox bbox =
                      boundingBox.orElseGet(
                          () ->
                              api.getSpatialExtent(collectionId)
                                  .orElse(tileMatrixSet.getBoundingBox()));
                  return new SimpleImmutableEntry<>(tmsId, bbox);
                })
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    switch (getType(apiData, collectionId)) {
      case MBTILES:
        deleteTilesMbtiles(api, collectionId, relevantZoomLevels, relevantBoundingBoxes);
        break;
      case FILES:
        deleteTilesFiles(apiData, collectionId, relevantZoomLevels, relevantBoundingBoxes);
        break;
    }

    LOGGER.info("Purging tile cache has finished");
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
   *
   * @return the file object of the directory
   */
  private Path getTilesStore() {
    return cacheStore;
  }

  /**
   * return and if necessary create the directory for the tile files that cannot be cached
   *
   * @return the file object of the directory
   */
  private Path getTmpDirectory() throws IOException {
    Path tmpDirectory = cacheStore.resolve(TMP_DIR_NAME);
    Files.createDirectories(tmpDirectory);
    return tmpDirectory;
  }

  /**
   * identifies the type of the cache for this tile (set)
   *
   * @param tile a tile in a tile set
   * @return {@code MBTILES} or {@code FILES} (the default)
   */
  private TilesConfiguration.TileCacheType getType(Tile tile) {
    return getType(
        tile.getApiData(),
        tile.isDatasetTile() ? Optional.empty() : Optional.of(tile.getCollectionId()));
  }

  /**
   * identifies the type of the cache for this tile (set)
   *
   * @param apiData the API
   * @param collectionId the collection, empty=dataset
   * @return {@code MBTILES} or {@code FILES} (the default)
   */
  private TilesConfiguration.TileCacheType getType(
      OgcApiDataV2 apiData, Optional<String> collectionId) {
    return (collectionId.isEmpty()
            ? apiData.getExtension(TilesConfiguration.class)
            : apiData.getExtension(TilesConfiguration.class, collectionId.get()))
        .map(cfg -> cfg.getCache())
        .orElse(TilesConfiguration.TileCacheType.FILES);
  }

  /**
   * MBTILES: open an existing Mbtiles cache file
   *
   * @param tile the tile
   * @return the Tileset
   */
  private MbtilesTileset getTileset(Tile tile) throws IOException {
    if (getType(tile) != TilesConfiguration.TileCacheType.MBTILES)
      throw new IllegalStateException(
          String.format(
              "Cannot get an Mbtiles cache. Found cache type: %s", getType(tile).toString()));
    OgcApi api = tile.getApi();
    Optional<String> collectionId =
        tile.isDatasetTile() ? Optional.empty() : Optional.of(tile.getCollectionId());
    TileMatrixSet tileMatrixSet = tile.getTileMatrixSet();
    return getOrInitTileset(api, collectionId, tileMatrixSet);
  }

  /**
   * MBTILES: create a new, empty Mbtiles cache file
   *
   * @param api the API
   * @param collectionId the collection; an empty value represents the dataset
   * @param tileMatrixSet the tile matrix set
   * @return the Tileset
   */
  private MbtilesTileset getOrInitTileset(
      OgcApi api, Optional<String> collectionId, TileMatrixSet tileMatrixSet) throws IOException {
    OgcApiDataV2 apiData = api.getData();
    String apiId = apiData.getId();
    String tileMatrixSetId = tileMatrixSet.getId();
    String key = String.join("/", apiId, collectionId.orElse("__all__"), tileMatrixSetId);
    if (!mbtiles.containsKey(key)) {
      Files.createDirectories(cacheStore.resolve(apiId).resolve(collectionId.orElse("__all__")));
      Path path =
          cacheStore
              .resolve(apiId)
              .resolve(collectionId.orElse("__all__"))
              .resolve(tileMatrixSetId + ".mbtiles");
      if (Files.exists(path)) {
        mbtiles.put(key, new MbtilesTileset(path));
      } else {
        TilesConfiguration config =
            collectionId.isEmpty()
                ? apiData.getExtension(TilesConfiguration.class).get()
                : apiData.getExtension(TilesConfiguration.class, collectionId.get()).get();

        // test, if tiles will be created for this tileset, otherwise log this information
        MinMax range = config.getZoomLevelsDerived().get(tileMatrixSetId);
        if (Objects.isNull(range)) {
          LOGGER.debug(
              "The configuration does not include tiles for tile matrix set '{}'{}, but other parts of the configuration require that the MBTiles file cache '{}' is created. Review the configuration for TILES on the API level and for each collection.",
              tileMatrixSetId,
              collectionId.map(s -> " for collection '" + s + "'").orElse(""),
              key);
        }

        // get the tile set metadata
        TileSet tileSetMetadata =
            TilesHelper.buildTileSet(
                api,
                tileMatrixSet,
                range,
                config.getCenterDerived(),
                collectionId,
                TileSet.DataType.vector,
                ImmutableList.of(),
                Optional.empty(),
                crsTransformerFactory,
                limitsGenerator,
                providers,
                entityRegistry);

        // convert to Mbtiles metadata
        // TODO support type, version
        MbtilesMetadata md =
            ImmutableMbtilesMetadata.builder()
                .name(apiData.getLabel())
                .format(MbtilesMetadata.MbtilesFormat.pbf)
                .description(apiData.getDescription())
                .attribution(apiData.getMetadata().flatMap(ApiMetadata::getAttribution))
                .minzoom(TilesHelper.getMinzoom(tileSetMetadata))
                .maxzoom(TilesHelper.getMaxzoom(tileSetMetadata))
                .bounds(TilesHelper.getBounds(tileSetMetadata))
                .center(TilesHelper.getCenter(tileSetMetadata))
                .vectorLayers(
                    TilesHelper.getVectorLayers(
                        apiData, collectionId, tileMatrixSet.getId(), providers, schemaInfo))
                .build();
        try {
          mbtiles.put(key, new MbtilesTileset(path, md));
        } catch (FileAlreadyExistsException e) {
          // The file could have been created by a parallel thread
          LOGGER.debug("MBTiles file '{}' already exists.", path);
          if (!mbtiles.containsKey(key)) {
            // reuse the existing file
            mbtiles.put(key, new MbtilesTileset(path));
          }
        }
      }
    }
    return mbtiles.get(key);
  }

  /**
   * FILES cache or temporary tiles: determine the file path of a tile
   *
   * @param tile the tile
   * @return the file path
   */
  private Path getPath(Tile tile) throws IOException {
    Path subDir;
    if (tile.getTemporary()) {
      subDir = getTmpDirectory();
      if (FileTime.from(Instant.now()).toMillis() - lastCleanup > TEN_MINUTES) cleanup();
    } else {
      subDir =
          getTilesStore()
              .resolve(tile.getApiData().getId())
              .resolve(tile.isDatasetTile() ? "__all__" : tile.getCollectionId())
              .resolve(tile.getTileMatrixSet().getId());
    }

    Path path = subDir.resolve(tile.getRelativePath());
    Files.createDirectories(path.getParent());
    return path;
  }

  private TileMatrixSet getTileMatrixSetById(String tileMatrixSetId) {
    return tileMatrixSetRepository
        .get(tileMatrixSetId)
        .orElseThrow(
            () -> new IllegalArgumentException("TileMatrixSet not found: " + tileMatrixSetId));
  }

  private List<TileMatrixSetLimits> getLimits(
      OgcApiDataV2 apiData,
      TileMatrixSet tileMatrixSet,
      MinMax minmax,
      Optional<String> collectionId,
      BoundingBox bbox) {
    return limitsGenerator.getTileMatrixSetLimits(bbox, tileMatrixSet, minmax);
  }

  private List<TileFormatWithQuerySupportExtension> getTileFormats(
      OgcApiDataV2 apiData, Optional<String> collectionId) {
    Optional<TilesConfiguration> config =
        collectionId.isEmpty()
            ? apiData.getExtension(TilesConfiguration.class)
            : apiData.getExtension(TilesConfiguration.class, collectionId.get());
    if (config.isEmpty()) return ImmutableList.of();
    return extensionRegistry
        .getExtensionsForType(TileFormatWithQuerySupportExtension.class)
        .stream()
        .filter(
            format ->
                collectionId
                    .map(s -> format.isEnabledForApi(apiData, s))
                    .orElseGet(() -> format.isEnabledForApi(apiData)))
        .filter(
            format ->
                config.get().getTileEncodingsDerived() == null
                    || config.get().getTileEncodingsDerived().isEmpty()
                    || config
                        .get()
                        .getTileEncodingsDerived()
                        .contains(format.getMediaType().label()))
        .collect(Collectors.toList());
  }

  private void deleteTilesMbtiles(
      OgcApi api,
      Optional<String> collectionId,
      Map<String, MinMax> zoomLevels,
      Map<String, BoundingBox> boundingBoxes)
      throws SQLException, IOException {
    for (Map.Entry<String, MinMax> tileSet : zoomLevels.entrySet()) {
      TileMatrixSet tileMatrixSet = getTileMatrixSetById(tileSet.getKey());
      MinMax levels = tileSet.getValue();
      BoundingBox bbox = boundingBoxes.get(tileSet.getKey());

      // first the dataset tiles
      deleteTilesMbtiles(api, Optional.empty(), tileMatrixSet, levels, bbox);

      if (collectionId.isPresent()) {
        // also the single collection tiles for the collection
        deleteTilesMbtiles(api, collectionId, tileMatrixSet, levels, bbox);
      } else {
        // all single collection tiles
        for (String colId : api.getData().getCollections().keySet()) {
          deleteTilesMbtiles(api, Optional.of(colId), tileMatrixSet, levels, bbox);
        }
      }
    }
  }

  private void deleteTilesMbtiles(
      OgcApi api,
      Optional<String> collectionId,
      TileMatrixSet tileMatrixSet,
      MinMax levels,
      BoundingBox bbox)
      throws SQLException, IOException {
    OgcApiDataV2 apiData = api.getData();
    MbtilesTileset tileset = getOrInitTileset(api, collectionId, tileMatrixSet);
    List<TileMatrixSetLimits> limitsList =
        getLimits(apiData, tileMatrixSet, levels, collectionId, bbox);
    for (TileMatrixSetLimits limits : limitsList) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "Deleting tiles from Mbtiles cache: API {}, collection {}, tiles {}/{}/{}-{}/{}-{}, TMS rows {}-{}",
            apiData.getId(),
            collectionId.orElse("all"),
            tileMatrixSet.getId(),
            limits.getTileMatrix(),
            limits.getMinTileRow(),
            limits.getMaxTileRow(),
            limits.getMinTileCol(),
            limits.getMaxTileCol(),
            tileMatrixSet.getTmsRow(
                Integer.parseInt(limits.getTileMatrix()), limits.getMaxTileRow()),
            tileMatrixSet.getTmsRow(
                Integer.parseInt(limits.getTileMatrix()), limits.getMinTileRow()));
      }
      tileset.deleteTiles(tileMatrixSet, limits);
    }
  }

  private void deleteTilesFiles(
      OgcApiDataV2 apiData,
      Optional<String> collectionId,
      Map<String, MinMax> zoomLevels,
      Map<String, BoundingBox> boundingBoxes)
      throws IOException {
    List<String> extensions =
        getTileFormats(apiData, collectionId).stream()
            .map(TileFormatExtension::getExtension)
            .collect(ImmutableList.toImmutableList());

    Map<String, Map<String, TileMatrixSetLimits>> limits =
        zoomLevels.keySet().stream()
            .map(
                tmsId -> {
                  Map<String, TileMatrixSetLimits> limitsMap =
                      getLimits(
                              apiData,
                              getTileMatrixSetById(tmsId),
                              zoomLevels.get(tmsId),
                              collectionId,
                              boundingBoxes.get(tmsId))
                          .stream()
                          .map(l -> new SimpleImmutableEntry<>(l.getTileMatrix(), l))
                          .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));

                  return new SimpleImmutableEntry<>(tmsId, limitsMap);
                })
            .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));

    Path basePath = getTilesStore().resolve(apiData.getId());

    try (Stream<Path> walk =
        Files.find(
            basePath,
            5,
            (path, basicFileAttributes) ->
                basicFileAttributes.isRegularFile()
                    && shouldDeleteTileFile(
                        basePath.relativize(path), collectionId, limits, extensions))) {
      walk.map(Path::toFile).forEach(File::delete);
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  private static boolean shouldDeleteTileFile(
      Path tilePath,
      Optional<String> collectionId,
      Map<String, Map<String, TileMatrixSetLimits>> tmsLimits,
      List<String> extensions) {
    if (tilePath.getNameCount() < 5) {
      return false;
    }

    String collection = tilePath.getName(0).toString();

    if (!Objects.equals(collection, "__all__")
        && collectionId.isPresent()
        && !Objects.equals(collectionId.get(), collection)) {
      return false;
    }

    String tmsId = tilePath.getName(1).toString();

    if (!tmsLimits.containsKey(tmsId)) {
      return false;
    }

    Map<String, TileMatrixSetLimits> levelLimits = tmsLimits.get(tmsId);
    String level = tilePath.getName(2).toString();

    if (!levelLimits.containsKey(level)) {
      return false;
    }

    TileMatrixSetLimits limits = levelLimits.get(level);
    int row = Integer.parseInt(tilePath.getName(3).toString());

    if (row < limits.getMinTileRow() || row > limits.getMaxTileRow()) {
      return false;
    }

    String file = tilePath.getName(4).toString();
    int col = Integer.parseInt(com.google.common.io.Files.getNameWithoutExtension(file));
    String extension = com.google.common.io.Files.getFileExtension(file);

    if (!extensions.contains(extension)) {
      return false;
    }

    if (col < limits.getMinTileCol() || col > limits.getMaxTileCol()) {
      return false;
    }

    return true;
  }
}
