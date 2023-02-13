/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.app;

import static de.ii.ogcapi.foundation.domain.FoundationConfiguration.CACHE_DIR;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles3d.domain.TileResourceCache;
import de.ii.ogcapi.tiles3d.domain.TileResourceDescriptor;
import de.ii.ogcapi.tiles3d.domain.Tiles3dConfiguration;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Access to the cache for tile files. */
@Singleton
@AutoBind
public class TileResourceCacheImpl implements TileResourceCache {

  private static final String TILES_DIR_NAME = "tiles3d";
  private final Path cacheStore;

  /** set data directory */
  @Inject
  public TileResourceCacheImpl(AppContext appContext) {
    // the ldproxy data directory, in development environment this would be ./build/data
    this.cacheStore = appContext.getDataDir().resolve(CACHE_DIR).resolve(TILES_DIR_NAME);
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

    for (String collectionId : apiData.getCollections().keySet()) {
      Tiles3dConfiguration config =
          apiData.getExtension(Tiles3dConfiguration.class, collectionId).orElseThrow();
      if (config.isEnabled()) {
        process(builder, api, collectionId);
      }
    }

    return builder.build();
  }

  private void process(ImmutableValidationResult.Builder builder, OgcApi api, String collectionId) {
    try {
      Files.createDirectories(cacheStore.resolve(api.getId()).resolve(collectionId));
    } catch (IOException e) {
      builder.addErrors(
          MessageFormat.format(
              "The folders for the tile cache for collection ''{0}'' could not be initialized.",
              collectionId));
    }
  }

  @Override
  public boolean tileResourceExists(TileResourceDescriptor r) throws IOException {
    return Files.exists(getPath(r));
  }

  @Override
  public Optional<InputStream> getTileResource(TileResourceDescriptor r) throws IOException {
    Path path = getPath(r);
    if (Files.notExists(path)) {
      return Optional.empty();
    }
    return Optional.of(new BufferedInputStream(Files.newInputStream(path)));
  }

  @Override
  public void deleteTileResource(TileResourceDescriptor r) throws IOException {
    Path path = getPath(r);
    Files.delete(path);
  }

  @Override
  public File getFile(TileResourceDescriptor r) throws IOException {
    return getPath(r).toFile();
  }

  @Override
  public void deleteTileResources(OgcApi api) throws IOException {
    try (Stream<Path> stream = Files.walk(cacheStore.resolve(api.getId()))) {
      stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
  }

  @Override
  public void storeTileResource(TileResourceDescriptor r, byte[] content) throws IOException {
    Path path = getPath(r);
    if (Files.notExists(path) || Files.isWritable(path)) {
      Files.write(path, content);
    }
  }

  /**
   * return and if necessary create the directory for the tiles cache
   *
   * @return the file object of the directory
   */
  private Path getStore() {
    return cacheStore;
  }

  /**
   * FILES cache: determine the file path of a tile resource
   *
   * @param r the tile
   * @return the file path
   */
  private Path getPath(TileResourceDescriptor r) throws IOException {
    Path subDir = getStore().resolve(r.getApiData().getId()).resolve(r.getCollectionId());

    Path path = subDir.resolve(r.getRelativePath());
    Files.createDirectories(path.getParent());
    return path;
  }
}
