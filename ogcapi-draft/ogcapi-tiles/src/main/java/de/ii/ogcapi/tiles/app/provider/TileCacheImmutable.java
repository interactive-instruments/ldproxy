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
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileCache;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationParameters;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.ogcapi.tiles.domain.provider.TileStore;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.services.domain.TaskContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileCacheImmutable implements ChainedTileProvider, TileCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileCacheImmutable.class);

  private final TileWalker tileWalker;
  private final TileStore tileStore;
  private final ChainedTileProvider delegate;
  private final Map<String, Map<String, Range<Integer>>> tmsRanges;

  public TileCacheImmutable(
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
  public void seed(
      Map<String, TileGenerationParameters> layers,
      List<MediaType> mediaTypes,
      boolean reseed,
      TaskContext taskContext)
      throws IOException {
    if (tileStore.staging().inProgress()) {
      // TODO: queue? what about partials?
    }

    tileStore.staging().init();

    Map<String, Optional<BoundingBox>> boundingBoxes =
        layers.entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(), entry.getValue().getClipBoundingBox()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // TODO: compute from limits instead of walking
    long numberOfTiles =
        tileWalker.getNumberOfTiles(
            layers.keySet(), mediaTypes, getTmsRanges(), boundingBoxes, taskContext);
    final double[] currentTile = {0.0};
    final boolean[] isEmpty = {true};

    LOGGER.debug("NUMTILES {} {}", numberOfTiles, reseed);

    tileWalker.walkLayersAndTiles(
        layers.keySet(),
        mediaTypes,
        getTmsRanges(),
        boundingBoxes,
        taskContext,
        (layer, mediaType, tileMatrixSet, level, row, col) -> {
          TileQuery tile =
              ImmutableTileQuery.builder()
                  .layer(layer)
                  .mediaType(mediaType)
                  .tileMatrixSet(tileMatrixSet)
                  .level(level)
                  .row(row)
                  .col(col)
                  .generationParameters(layers.get(layer))
                  .build();

          taskContext.setStatusMessage(
              String.format(
                  "currently processing -> %s, %s/%s/%s/%s, %s",
                  layer, tileMatrixSet.getId(), level, row, col, mediaType));

          if (reseed || !tileStore.has(tile)) {
            TileResult result = delegate.get(tile);

            if (shouldCache(tile) && result.isAvailable()) {
              tileStore.put(tile, new ByteArrayInputStream(result.getContent().get()));
              if (isEmpty[0]) {
                isEmpty[0] = false;
              }
            }

            if (result.isError()) {
              LOGGER.warn(
                  "{}: processing failed -> {}, {}/{}/{}/{}, {} | {}",
                  taskContext.getTaskLabel(),
                  layer,
                  tileMatrixSet.getId(),
                  level,
                  row,
                  col,
                  mediaType,
                  result.getError().get());
            }
          }

          currentTile[0] += 1;
          taskContext.setCompleteness(currentTile[0] / numberOfTiles);

          return !taskContext.isStopped();
        });

    if (!isEmpty[0]) {
      tileStore.staging().promote();
    } else {
      tileStore.staging().abort();
    }

    if (taskContext.isFirstPartial()) {
      tileStore.staging().cleanup();
    }
  }

  private boolean shouldCache(TileQuery tileQuery) {
    return !tileQuery.isTransient();
  }
}
