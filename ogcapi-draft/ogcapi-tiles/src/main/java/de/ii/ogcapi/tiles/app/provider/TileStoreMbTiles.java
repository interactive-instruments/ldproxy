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
import de.ii.ogcapi.tiles.app.provider.TileCacheDynamic.TileStore;
import de.ii.ogcapi.tiles.app.provider.TileCacheDynamic.TileStoreReadOnly;
import de.ii.ogcapi.tiles.domain.ImmutableFields;
import de.ii.ogcapi.tiles.domain.ImmutableVectorLayer;
import de.ii.ogcapi.tiles.domain.VectorLayer;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationSchema;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TileStoreMbTiles implements TileStore {

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
      Path rootDir, String providerId, Map<String, Map<String, TileGenerationSchema>> tileSchemas) {
    return new TileStoreMbTiles(providerId, rootDir, new ConcurrentHashMap<>(), tileSchemas);
  }

  private final String providerId;
  private final Path rootDir;
  private final Map<String, Map<String, TileGenerationSchema>> tileSchemas;
  private final Map<String, MbtilesTileset> tileSets;

  private TileStoreMbTiles(
      String providerId,
      Path rootDir,
      Map<String, MbtilesTileset> tileSets,
      Map<String, Map<String, TileGenerationSchema>> tileSchemas) {
    this.providerId = providerId;
    this.rootDir = rootDir;
    this.tileSchemas = tileSchemas;
    this.tileSets = tileSets;
  }

  @Override
  public boolean has(TileQuery tile) {
    try {
      return tileSets.containsKey(key(tile)) && tileSets.get(key(tile)).tileExists(tile);
    } catch (SQLException | IOException e) {
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
    }
    return Optional.empty();
  }

  @Override
  public void put(TileQuery tile, InputStream content) throws IOException {
    try {
      if (!tileSets.containsKey(key(tile))) {
        synchronized (tileSets) {
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
    }
  }

  @Override
  public void delete(TileQuery tile) throws IOException {
    try {
      if (tileSets.containsKey(key(tile))) {
        tileSets.get(key(tile)).deleteTile(tile);
      }
    } catch (SQLException e) {
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
        ImmutableVectorLayer.builder()
            .id(subLayer)
            .fields(
                new ImmutableFields.Builder()
                    .additionalProperties(generationSchema.getProperties())
                    .build());

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
    Path path = rootDir.resolve(layer).resolve(tileMatrixSet.getId() + ".mbtiles");

    if (Files.exists(path)) {
      return new MbtilesTileset(path);
    }

    MbtilesMetadata md =
        ImmutableMbtilesMetadata.builder()
            .name(name)
            .format(MbtilesMetadata.MbtilesFormat.pbf)
            .vectorLayers(vectorLayers)
            .build();
    try {
      return new MbtilesTileset(path, md);
    } catch (FileAlreadyExistsException e) {
      // The file could have been created by a parallel thread
      // LOGGER.debug("MBTiles file '{}' already exists.", path);
      // reuse the existing file
      return new MbtilesTileset(path);
    }
  }

  private static String key(TileQuery tile) {
    return key(tile.getLayer(), tile.getTileMatrixSet());
  }

  private static String key(String layer, TileMatrixSet tileMatrixSet) {
    return String.join("/", layer, tileMatrixSet.getId());
  }
}
