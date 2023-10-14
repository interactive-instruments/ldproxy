/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.tiles3d.domain.TileResourceCache;
import de.ii.ogcapi.tiles3d.domain.TileResourceDescriptor;
import de.ii.xtraplatform.blobs.domain.BlobStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Access to the cache for tile files. */
@Singleton
@AutoBind
public class TileResourceCacheImpl implements TileResourceCache {

  private final BlobStore cacheStore;

  @Inject
  public TileResourceCacheImpl(BlobStore blobStore) {
    this.cacheStore = blobStore.with(Tiles3dBuildingBlock.STORE_RESOURCE_TYPE);
  }

  @Override
  public boolean tileResourceExists(TileResourceDescriptor r) throws IOException {
    return cacheStore.has(getPath(r));
  }

  @Override
  public Optional<InputStream> getTileResource(TileResourceDescriptor r) throws IOException {
    return cacheStore.content(getPath(r));
  }

  @Override
  public void deleteTileResource(TileResourceDescriptor r) throws IOException {
    cacheStore.delete(getPath(r));
  }

  @Override
  public void deleteTileResources(OgcApi api) throws IOException {
    Path parent = Path.of(api.getId());

    try (Stream<Path> paths = cacheStore.walk(parent, 16, (p, a) -> true)) {
      paths
          .sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                Path path1 = parent.resolve(path);
                try {
                  cacheStore.delete(path1);
                } catch (IOException e) {
                  // ignore
                }
              });
    }
  }

  @Override
  public void storeTileResource(TileResourceDescriptor r, byte[] content) throws IOException {
    cacheStore.put(getPath(r), new ByteArrayInputStream(content));
  }

  /**
   * FILES cache: determine the file path of a tile resource
   *
   * @param r the tile
   * @return the file path
   */
  private Path getPath(TileResourceDescriptor r) {
    return Path.of(r.getApiData().getId())
        .resolve(r.getCollectionId())
        .resolve(r.getRelativePath());
  }
}
