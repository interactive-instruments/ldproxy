/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.consumerMayThrow;

import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimits;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.ogcapi.tiles.domain.provider.TileStore;
import de.ii.xtraplatform.store.domain.BlobStore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.ws.rs.core.MediaType;

class TileStorePlain implements TileStore {

  private static Map<MediaType, String> EXTENSIONS =
      ImmutableMap.of(FeatureEncoderMVT.FORMAT, "pbf");

  private final BlobStore blobStore;

  TileStorePlain(BlobStore blobStore) {
    this.blobStore = blobStore;
  }

  @Override
  public boolean has(TileQuery tile) throws IOException {
    return blobStore.has(path(tile));
  }

  @Override
  public TileResult get(TileQuery tile) throws IOException {
    Optional<InputStream> content = blobStore.get(path(tile));

    if (content.isEmpty()) {
      return TileResult.notFound();
    }

    return TileResult.found(content.get().readAllBytes());
  }

  @Override
  public Optional<Boolean> isEmpty(TileQuery tile) throws IOException {
    long size = blobStore.size(path(tile));

    return size < 0 ? Optional.empty() : Optional.of(size == 0);
  }

  @Override
  public void put(TileQuery tile, InputStream content) throws IOException {
    blobStore.put(path(tile), content);
  }

  @Override
  public void delete(TileQuery tile) throws IOException {
    blobStore.delete(path(tile));
  }

  @Override
  public void delete(
      String layer, TileMatrixSet tileMatrixSet, TileMatrixSetLimits limits, boolean inverse)
      throws IOException {
    try (Stream<Path> matchingFiles =
        blobStore.walk(
            Path.of(""),
            5,
            (path, fileAttributes) ->
                fileAttributes.isValue()
                    && TileStore.isInsideBounds(
                        path, layer, tileMatrixSet.getId(), limits, inverse))) {

      try {
        matchingFiles.forEach(consumerMayThrow(blobStore::delete));
      } catch (RuntimeException e) {
        if (e.getCause() instanceof IOException) {
          throw (IOException) e.getCause();
        }
        throw e;
      }
    }
  }

  private static Path path(TileQuery tile) {
    return Path.of(
        tile.getLayer(),
        tile.getTileMatrixSet().getId(),
        String.valueOf(tile.getLevel()),
        String.valueOf(tile.getRow()),
        String.format("%d.%s", tile.getCol(), EXTENSIONS.get(tile.getMediaType())));
  }
}
