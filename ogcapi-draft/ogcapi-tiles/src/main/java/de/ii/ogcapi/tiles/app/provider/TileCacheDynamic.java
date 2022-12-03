/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import com.google.common.collect.Range;
import de.ii.ogcapi.tiles.domain.provider.ChainedTileProvider;
import de.ii.ogcapi.tiles.domain.provider.TileCache;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationParameters;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.ogcapi.tiles.domain.provider.TileStore;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.services.domain.TaskContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileCacheDynamic implements ChainedTileProvider, TileCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileCacheDynamic.class);

  private final TileWalker tileWalker;
  private final TileStore tileStore;
  private final ChainedTileProvider delegate;
  private final Map<String, Map<String, Range<Integer>>> tmsRanges;

  public TileCacheDynamic(
      TileWalker tileWalker,
      TileStore tileStore,
      ChainedTileProvider delegate,
      Map<String, Map<String, Range<Integer>>> tmsRanges) {
    this.tileWalker = tileWalker;
    this.tileStore = tileStore;
    this.delegate = delegate;
    this.tmsRanges = tmsRanges;
  }

  @Override
  public Map<String, Map<String, Range<Integer>>> getTmsRanges() {
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

  @Override
  public void seed(
      Map<String, TileGenerationParameters> layers,
      List<MediaType> mediaTypes,
      boolean reseed,
      String tileSourceLabel,
      TaskContext taskContext)
      throws IOException {
    doSeed(
        layers,
        mediaTypes,
        reseed,
        tileSourceLabel,
        taskContext,
        tileStore,
        delegate,
        tileWalker,
        getTmsRanges());
  }

  @Override
  public void purge(
      Map<String, TileGenerationParameters> layers,
      List<MediaType> mediaTypes,
      boolean reseed,
      String tileSourceLabel,
      TaskContext taskContext)
      throws IOException {

    Map<String, Optional<BoundingBox>> boundingBoxes = getBoundingBoxes(layers);

    // TODO: other partials may start writing before first partial is done with purging
    // add preRun and postRun to tasks which are run before/after partials
    if (reseed) {
      if (taskContext.isFirstPartial()) {
        LOGGER.debug("{}: purging cache for {}", taskContext.getTaskLabel(), tileSourceLabel);

        tileWalker.walkLayersAndLimits(
            layers.keySet(),
            getTmsRanges(),
            boundingBoxes,
            (layer, tileMatrixSet, limits) -> {
              try {
                tileStore.delete(layer, tileMatrixSet, limits, false);
              } catch (IOException e) {
                // ignore
              }
            });

        LOGGER.debug(
            "{}: purged cache successfully for {}", taskContext.getTaskLabel(), tileSourceLabel);
      } else {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          // ignore
        }
      }
    }
  }
}
