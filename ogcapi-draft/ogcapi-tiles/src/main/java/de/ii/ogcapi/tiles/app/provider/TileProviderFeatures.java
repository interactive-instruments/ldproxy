/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import static de.ii.ogcapi.foundation.domain.FoundationConfiguration.CACHE_DIR;
import static de.ii.ogcapi.tiles.app.provider.TileCacheImpl.TILES_DIR_NAME;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.Range;
import de.ii.ogcapi.tiles.app.provider.TileCacheDynamic.FileStoreFs;
import de.ii.ogcapi.tiles.app.provider.TileCacheDynamic.TileStoreFiles;
import de.ii.ogcapi.tiles.domain.TileCache;
import de.ii.ogcapi.tiles.domain.provider.ChainedTileProvider;
import de.ii.ogcapi.tiles.domain.provider.ImmutableLayerOptions;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileProviderFeaturesData;
import de.ii.ogcapi.tiles.domain.provider.TileGenerator;
import de.ii.ogcapi.tiles.domain.provider.TileProvider;
import de.ii.ogcapi.tiles.domain.provider.TileProviderFeaturesData;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import java.io.IOException;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: who creates this? entity factory?
@Singleton
@AutoBind
public class TileProviderFeatures implements TileProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileProviderFeatures.class);

  private final TileGeneratorFeatures tileGenerator;
  private final ChainedTileProvider providerChain;
  // TODO
  private final TileProviderFeaturesData data =
      ImmutableTileProviderFeaturesData.builder()
          .layerDefaults(new ImmutableLayerOptions.Builder().featureProvider("bergbau").build())
          .build();

  @Inject
  public TileProviderFeatures(
      CrsInfo crsInfo, TileCache tileCache, EntityRegistry entityRegistry, AppContext appContext) {
    this.tileGenerator = new TileGeneratorFeatures(data, crsInfo, entityRegistry);

    Path cacheDir =
        appContext.getDataDir().resolve(CACHE_DIR).resolve(TILES_DIR_NAME).resolve("bergbau");
    FileStoreFs fileStore = new FileStoreFs(cacheDir);
    TileStoreFiles tileStore = new TileStoreFiles(fileStore);
    TileCacheDynamic tileCacheDynamic =
        new TileCacheDynamic(tileStore, tileGenerator, Range.closed(2, 8));

    this.providerChain = tileCacheDynamic;
  }

  @Override
  public TileResult getTile(TileQuery tile) {
    // TODO: check out of bounds
    try {
      return providerChain.get(tile);
    } catch (IOException e) {
      LOGGER.warn(
          "Failed to retrieve tile {}/{}/{}/{} for layer '{}'. Reason: {}",
          tile.getTileMatrixSet().getId(),
          tile.getTileLevel(),
          tile.getTileRow(),
          tile.getTileCol(),
          tile.getLayer(),
          e.getMessage());
    }

    // TODO: .error
    return TileResult.notFound();
  }

  @Override
  public boolean supportsGeneration() {
    return true;
  }

  @Override
  public TileGenerator generator() {
    return tileGenerator;
  }
}
