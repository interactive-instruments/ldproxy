/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import com.google.common.collect.Range;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimits;
import de.ii.ogcapi.tiles.app.provider.TileCacheDynamic.TileStoreReadOnly;
import de.ii.ogcapi.tiles.domain.provider.ChainedTileProvider;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationParameters;
import de.ii.ogcapi.tiles.domain.provider.TileProvider;
import de.ii.ogcapi.tiles.domain.provider.TileProviderFeaturesData;
import de.ii.ogcapi.tiles.domain.provider.TileProviderMbtilesData;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.store.domain.entities.AbstractPersistentEntity;
import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileProviderMbTiles extends AbstractPersistentEntity<TileProviderMbtilesData>
    implements TileProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileProviderMbTiles.class);
  private final ChainedTileProvider providerChain;

  @AssistedInject
  public TileProviderMbTiles(AppContext appContext, @Assisted TileProviderMbtilesData data) {
    super(data);

    Map<String, Path> layerSources =
        data.getLayers().entrySet().stream()
            .map(
                entry -> {
                  Path source = Path.of(entry.getValue().getSource());

                  // TODO: different TMSs?
                  return new SimpleImmutableEntry<>(
                      String.join("/", entry.getKey(), "WebMercatorQuad"),
                      source.isAbsolute() ? source : appContext.getDataDir().resolve(source));
                })
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    TileStoreReadOnly tileStore = TileStoreMbTiles.readOnly(layerSources);

    this.providerChain =
        new ChainedTileProvider() {
          @Override
          public Map<String, Map<String, Range<Integer>>> getTmsRanges() {
            return data.getTmsRanges();
          }

          @Override
          public TileResult getTile(TileQuery tile) throws IOException {
            return tileStore.get(tile);
          }
        };
  }

  @Override
  protected boolean onStartup() throws InterruptedException {
    return super.onStartup();
  }

  @Override
  public TileResult getTile(TileQuery tile) {
    Optional<TileResult> error = validate(tile);

    if (error.isPresent()) {
      return error.get();
    }

    return providerChain.get(tile);
  }

  @Override
  public boolean supportsGeneration() {
    return false;
  }

  private Optional<TileResult> validate(TileQuery tile) {
    if (!getData().getLayers().containsKey(tile.getLayer())) {
      return Optional.of(
          TileResult.error(String.format("Layer '%s' is not supported.", tile.getLayer())));
    }

    Map<String, Range<Integer>> tmsRanges =
        getData().getLayers().get(tile.getLayer()).getTmsRanges();

    if (!tmsRanges.containsKey(tile.getTileMatrixSet().getId())) {
      return Optional.of(
          TileResult.error(
              String.format(
                  "Tile matrix set '%s' is not supported.", tile.getTileMatrixSet().getId())));
    }

    if (!tmsRanges.get(tile.getTileMatrixSet().getId()).contains(tile.getLevel())) {
      return Optional.of(
          TileResult.error("The requested tile is outside the zoom levels for this tile set."));
    }

    BoundingBox boundingBox =
        tile.getGenerationParameters()
            .flatMap(TileGenerationParameters::getClipBoundingBox)
            .orElse(tile.getTileMatrixSet().getBoundingBox());
    TileMatrixSetLimits limits = tile.getTileMatrixSet().getLimits(tile.getLevel(), boundingBox);

    if (!limits.contains(tile.getRow(), tile.getCol())) {
      return Optional.of(
          TileResult.error(
              "The requested tile is outside of the limits for this zoom level and tile set."));
    }

    return Optional.empty();
  }

  @Override
  public String getType() {
    return TileProviderFeaturesData.PROVIDER_TYPE;
  }
}
