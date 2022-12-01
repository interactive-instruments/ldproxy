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
import de.ii.ogcapi.tiles.domain.provider.Cache;
import de.ii.ogcapi.tiles.domain.provider.Cache.Storage;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationSchema;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.ogcapi.tiles.domain.provider.TileStore;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.store.domain.BlobStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileStoreMulti implements TileStore, TileStore.Staging {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileStoreMulti.class);

  private final BlobStore cacheStore;
  // TODO: factory?
  private final Cache.Storage storage;
  private final String tileSetName;
  private final Map<String, Map<String, TileGenerationSchema>> tileSchemas;
  private final List<Tuple<TileStore, BlobStore>> active;
  private Tuple<TileStore, BlobStore> staging;

  // TODO: how to sync active on non-seeding node in multi-node setup
  public TileStoreMulti(
      BlobStore cacheStore,
      Storage storage,
      String tileSetName,
      Map<String, Map<String, TileGenerationSchema>> tileSchemas) {
    this.cacheStore = cacheStore;
    this.storage = storage;
    this.tileSetName = tileSetName;
    this.tileSchemas = tileSchemas;
    this.staging = null;
    this.active = getActive();

    // TODO: minAge option for full seeding
    // TODO: without purge, create new layer with missing tiles
    // TODO: with purge, create complete new layer
    // TODO: partial updates from feature changes always purge
    // TODO: purge on dataset changes can be configured

  }

  private List<Tuple<TileStore, BlobStore>> getActive() {
    try (Stream<Path> walk = cacheStore.walk(Path.of(""), 1, (p, a) -> !a.isValue())) {
      return walk.skip(1)
          .flatMap(
              p -> {
                try {
                  BlobStore cacheLevel = cacheStore.with(p.getFileName().toString());
                  boolean isStaging = cacheLevel.has(Path.of(".staging"));

                  LOGGER.debug("{} {}", p.getFileName(), isStaging ? "staging" : "active");

                  if (!isStaging) {
                    return Stream.of(Tuple.of(getTileStore(cacheLevel), cacheLevel));
                  }
                } catch (IOException e) {
                  // ignore
                }
                return Stream.empty();
              })
          .collect(Collectors.toList());
    } catch (IOException e) {
      // ignore
    }

    return List.of();
  }

  @Override
  public boolean has(TileQuery tile) throws IOException {
    for (Tuple<TileStore, BlobStore> store : active) {
      if (store.first().has(tile)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public TileResult get(TileQuery tile) throws IOException {
    for (Tuple<TileStore, BlobStore> store : active) {
      TileResult result = store.first().get(tile);
      if (!result.isNotFound()) {
        return result;
      }
    }

    return TileResult.notFound();
  }

  @Override
  public Optional<Boolean> isEmpty(TileQuery tile) throws IOException {
    for (Tuple<TileStore, BlobStore> store : active) {
      Optional<Boolean> result = store.first().isEmpty(tile);
      if (result.isPresent()) {
        return result;
      }
    }

    return Optional.empty();
  }

  @Override
  public void put(TileQuery tile, InputStream content) throws IOException {
    if (!inProgress()) {
      throw new IllegalStateException("Writing is only allowed during staging.");
    }
    staging.first().put(tile, content);
  }

  @Override
  public void delete(TileQuery tile) throws IOException {
    // TODO: deletes not allowed, only by internal cleanup?
  }

  @Override
  public void delete(String layer, TileMatrixSet tileMatrixSet, TileMatrixSetLimits limits)
      throws IOException {
    // TODO: deletes not allowed, only by internal cleanup?
  }

  @Override
  public synchronized boolean inProgress() {
    return Objects.nonNull(staging);
  }

  @Override
  public synchronized boolean init() throws IOException {
    if (inProgress()) {
      return false;
    }
    BlobStore stagingStore = cacheStore.with(String.format("%d", Instant.now().toEpochMilli()));

    stagingStore.put(Path.of(".staging"), new ByteArrayInputStream(new byte[0]));

    TileStore tileStore = getTileStore(stagingStore);

    this.staging = Tuple.of(tileStore, stagingStore);

    return true;
  }

  private TileStore getTileStore(BlobStore blobStore) {
    return storage == Storage.MBTILES
        ? TileStoreMbTiles.readWrite(blobStore, tileSetName, tileSchemas)
        : new TileStorePlain(blobStore);
  }

  @Override
  public synchronized void promote() throws IOException {
    if (inProgress()) {
      // TODO: rename folder, BlobStore.move? or just remove special marker file .staging?
      staging.second().delete(Path.of(".staging"));

      this.active.add(0, staging);
      this.staging = null;
    }
  }

  @Override
  public synchronized void cleanup() throws IOException {
    if (inProgress()) {
      throw new IllegalStateException("Cleanup is not allowed during staging.");
    }

    // TODO: remove all directories with .staging
    // remove all tiles that are also in newer active
    // remove all tiles outside of bounds
  }
}
