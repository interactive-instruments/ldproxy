/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import de.ii.ogcapi.tiles.domain.provider.ChainedTileProvider;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.MediaType;

public class TileCacheDynamic implements ChainedTileProvider {

  /** abstraction over fs */
  interface FileStore {

    boolean has(Path path);

    Optional<InputStream> get(Path path) throws IOException;

    Optional<Boolean> isEmpty(Path path) throws IOException;

    void put(Path path, InputStream content) throws IOException;

    void delete(Path path) throws IOException;
  }

  static class FileStoreFs implements FileStore {
    private final Path rootDir;

    FileStoreFs(Path rootDir) {
      this.rootDir = rootDir;
    }

    @Override
    public boolean has(Path path) {
      return Files.exists(full(path));
    }

    @Override
    public Optional<InputStream> get(Path path) throws IOException {
      Path filePath = full(path);

      if (Files.notExists(filePath)) {
        return Optional.empty();
      }

      return Optional.of(Files.newInputStream(filePath));
    }

    @Override
    public Optional<Boolean> isEmpty(Path path) throws IOException {
      Path filePath = full(path);

      if (Files.notExists(filePath)) {
        return Optional.empty();
      }

      return Optional.of(Files.size(full(path)) == 0);
    }

    @Override
    public void put(Path path, InputStream content) throws IOException {
      Path filePath = full(path);

      if (Files.notExists(filePath) || Files.isWritable(filePath)) {
        Files.createDirectories(filePath.getParent());

        try (OutputStream file = Files.newOutputStream(filePath)) {
          content.transferTo(file);
        }
      }
    }

    @Override
    public void delete(Path path) throws IOException {
      Files.delete(full(path));
    }

    private Path full(Path path) {
      return rootDir.resolve(path);
    }
  }

  interface TileStoreReadOnly {

    boolean has(TileQuery tile);

    TileResult get(TileQuery tile) throws IOException;

    Optional<Boolean> isEmpty(TileQuery tile) throws IOException;
  }

  interface TileStore extends TileStoreReadOnly {

    void put(TileQuery tile, InputStream content) throws IOException;

    void delete(TileQuery tile) throws IOException;
  }

  static class TileStoreFiles implements TileStore {

    private static Map<MediaType, String> EXTENSIONS =
        ImmutableMap.of(FeatureEncoderMVT.FORMAT, "pbf");

    private final FileStore fileStore;

    TileStoreFiles(FileStore fileStore) {
      this.fileStore = fileStore;
    }

    @Override
    public boolean has(TileQuery tile) {
      return fileStore.has(path(tile));
    }

    @Override
    public TileResult get(TileQuery tile) throws IOException {
      Optional<InputStream> content = fileStore.get(path(tile));

      if (content.isEmpty()) {
        return TileResult.notFound();
      }

      return TileResult.found(content.get().readAllBytes());
    }

    @Override
    public Optional<Boolean> isEmpty(TileQuery tile) throws IOException {
      return fileStore.isEmpty(path(tile));
    }

    @Override
    public void put(TileQuery tile, InputStream content) throws IOException {
      fileStore.put(path(tile), content);
    }

    @Override
    public void delete(TileQuery tile) throws IOException {
      fileStore.delete(path(tile));
    }

    private Path path(TileQuery tile) {
      return Path.of(
          tile.getLayer(),
          tile.getTileMatrixSet().getId(),
          String.valueOf(tile.getTileLevel()),
          String.valueOf(tile.getTileRow()),
          String.format("%d.%s", tile.getTileCol(), EXTENSIONS.get(tile.getMediaType())));
    }
  }

  private final TileStore tileStore;
  private final ChainedTileProvider delegate;
  private final Map<String, Range<Integer>> tmsRanges;

  public TileCacheDynamic(
      TileStore tileStore, ChainedTileProvider delegate, Map<String, Range<Integer>> tmsRanges) {
    this.tileStore = tileStore;
    this.delegate = delegate;
    this.tmsRanges = tmsRanges;
  }

  @Override
  public Map<String, Range<Integer>> getTmsRanges() {
    return tmsRanges;
  }

  @Override
  public Optional<ChainedTileProvider> getDelegate() {
    return Optional.of(delegate);
  }

  @Override
  public TileResult getTile(TileQuery tile) throws IOException {
    if (shouldCache(tile)) {
      return tileStore.get(tile);
    }
    return TileResult.notFound();
  }

  @Override
  public TileResult processDelegateResult(TileQuery tile, TileResult tileResult)
      throws IOException {
    if (shouldCache(tile) && tileResult.isAvailable()) {
      tileStore.put(tile, new ByteArrayInputStream(tileResult.getContent().get()));

      return tileStore.get(tile);
    }

    return tileResult;
  }

  private boolean shouldCache(TileQuery tileQuery) {
    return tileQuery.getUserParametersForGeneration().isEmpty();
  }
}
