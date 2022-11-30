/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimits;
import de.ii.ogcapi.tiles.domain.ImmutableVectorLayer;
import de.ii.ogcapi.tiles.domain.VectorLayer;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationSchema;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.ogcapi.tiles.domain.provider.TileStore;
import de.ii.ogcapi.tiles.domain.provider.TileStoreReadOnly;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.store.domain.BlobStore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileStoreMbTiles implements TileStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileStoreMbTiles.class);

  static TileStoreReadOnly readOnly(Map<String, Path> tileSetSources) {
    Map<String, MbtilesTileset> tileSets =
        tileSetSources.entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(), new MbtilesTileset(entry.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return new TileStoreMbTiles("", null, tileSets, Map.of());
  }

  static TileStore readWrite(
      BlobStore rootStore,
      String providerId,
      Map<String, Map<String, TileGenerationSchema>> tileSchemas) {
    return new TileStoreMbTiles(providerId, rootStore, new ConcurrentHashMap<>(), tileSchemas);
  }

  private final String providerId;
  private final BlobStore rootStore;
  private final Map<String, Map<String, TileGenerationSchema>> tileSchemas;
  private final Map<String, MbtilesTileset> tileSets;

  private TileStoreMbTiles(
      String providerId,
      BlobStore rootStore,
      Map<String, MbtilesTileset> tileSets,
      Map<String, Map<String, TileGenerationSchema>> tileSchemas) {
    this.providerId = providerId;
    this.rootStore = rootStore;
    this.tileSchemas = tileSchemas;
    this.tileSets = tileSets;
  }

  @Override
  public boolean has(TileQuery tile) {
    try {
      return tileSets.containsKey(key(tile)) && tileSets.get(key(tile)).tileExists(tile);
    } catch (SQLException | IOException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Failed to check existence of tile {}/{}/{}/{} for layer '{}'. Reason: {}",
            tile.getTileMatrixSet().getId(),
            tile.getLevel(),
            tile.getRow(),
            tile.getCol(),
            tile.getLayer(),
            e.getMessage());
        if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
          LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
        }
      }
    }
    return false;
  }

  @Override
  public TileResult get(TileQuery tile) throws IOException {
    if (!tileSets.containsKey(key(tile))) {
      return TileResult.notFound();
    }

    try {
      Optional<InputStream> content = tileSets.get(key(tile)).getTile(tile);

      if (content.isEmpty()) {
        return TileResult.notFound();
      }

      return TileResult.found(content.get().readAllBytes());
    } catch (SQLException e) {
      return TileResult.error(e.getMessage());
    }
  }

  @Override
  public Optional<Boolean> isEmpty(TileQuery tile) throws IOException {
    try {
      if (tileSets.containsKey(key(tile))) {
        return tileSets.get(key(tile)).tileIsEmpty(tile);
      }
    } catch (SQLException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Failed to retrieve tile {}/{}/{}/{} for layer '{}'. Reason: {}",
            tile.getTileMatrixSet().getId(),
            tile.getLevel(),
            tile.getRow(),
            tile.getCol(),
            tile.getLayer(),
            e.getMessage());
        if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
          LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public void put(TileQuery tile, InputStream content) throws IOException {
    try {
      synchronized (tileSets) {
        if (!tileSets.containsKey(key(tile))) {
          tileSets.put(
              key(tile),
              createTileSet(
                  providerId,
                  tile.getLayer(),
                  tile.getTileMatrixSet(),
                  getVectorLayers(tile.getLayer())));
        }
      }
      tileSets.get(key(tile)).writeTile(tile, content.readAllBytes());

    } catch (SQLException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Failed to write tile {}/{}/{}/{} for layer '{}'. Reason: {}",
            tile.getTileMatrixSet().getId(),
            tile.getLevel(),
            tile.getRow(),
            tile.getCol(),
            tile.getLayer(),
            e.getMessage());
        if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
          LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
        }
      }
    }
  }

  @Override
  public void delete(TileQuery tile) throws IOException {
    try {
      if (tileSets.containsKey(key(tile))) {
        tileSets.get(key(tile)).deleteTile(tile);
      }
    } catch (SQLException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Failed to delete tile {}/{}/{}/{} for layer '{}'. Reason: {}",
            tile.getTileMatrixSet().getId(),
            tile.getLevel(),
            tile.getRow(),
            tile.getCol(),
            tile.getLayer(),
            e.getMessage());
        if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
          LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
        }
      }
    }
  }

  @Override
  public void delete(String layer, TileMatrixSet tileMatrixSet, TileMatrixSetLimits limits)
      throws IOException {
    try {
      if (tileSets.containsKey(key(layer, tileMatrixSet))) {
        tileSets.get(key(layer, tileMatrixSet)).deleteTiles(tileMatrixSet, limits);
      }
    } catch (SQLException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Failed to delete tiles {}/{}/{}-{}/{}-{} for layer '{}'. Reason: {}",
            tileMatrixSet,
            limits.getTileMatrix(),
            limits.getMinTileRow(),
            limits.getMaxTileRow(),
            limits.getMinTileCol(),
            limits.getMaxTileCol(),
            layer,
            e.getMessage());
        if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
          LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
        }
      }
    }
  }

  private List<VectorLayer> getVectorLayers(String layer) {
    return tileSchemas.get(layer).entrySet().stream()
        .map(entry -> getVectorLayer(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  // TODO: fields, minzoom, maxzoom
  private VectorLayer getVectorLayer(String subLayer, TileGenerationSchema generationSchema) {

    ImmutableVectorLayer.Builder builder =
        ImmutableVectorLayer.builder().id(subLayer).fields(generationSchema.getProperties());

    switch (generationSchema.getGeometryType().orElse(SimpleFeatureGeometry.ANY)) {
      case POINT:
      case MULTI_POINT:
        builder.geometryType("points");
        break;
      case LINE_STRING:
      case MULTI_LINE_STRING:
        builder.geometryType("lines");
        break;
      case POLYGON:
      case MULTI_POLYGON:
        builder.geometryType("polygons");
        break;
      case GEOMETRY_COLLECTION:
      case ANY:
      case NONE:
      default:
        builder.geometryType("unknown");
        break;
    }

    return builder.build();
  }

  // TODO: minzoom, maxzoom, bounds, center
  private MbtilesTileset createTileSet(
      String name, String layer, TileMatrixSet tileMatrixSet, List<VectorLayer> vectorLayers)
      throws IOException {
    Path relPath = Path.of(layer).resolve(tileMatrixSet.getId() + ".mbtiles");
    Optional<Path> filePath = rootStore.path(relPath, true);

    if (filePath.isEmpty()) {
      throw new IllegalStateException(
          "Could not create MBTiles file. Make sure you have a writable localizable source defined in cfg.yml.");
    }

    if (rootStore.has(relPath)) {
      return new MbtilesTileset(filePath.get());
    }

    MbtilesMetadata md =
        ImmutableMbtilesMetadata.builder()
            .name(name)
            .format(MbtilesMetadata.MbtilesFormat.pbf)
            .vectorLayers(vectorLayers)
            .build();
    try {
      return new MbtilesTileset(filePath.get(), md);
    } catch (FileAlreadyExistsException e) {
      throw new IllegalStateException(
          "A MBTiles file already exists. It must have been created by a parallel thread, which should not occur. MBTiles file creation must be synchronized.");
    }
  }

  private static String key(TileQuery tile) {
    return key(tile.getLayer(), tile.getTileMatrixSet());
  }

  private static String key(String layer, TileMatrixSet tileMatrixSet) {
    return String.join("/", layer, tileMatrixSet.getId());
  }
}
