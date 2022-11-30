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
import de.ii.ogcapi.tiles.domain.provider.ChainedTileProvider;
import de.ii.ogcapi.tiles.domain.provider.TileProvider;
import de.ii.ogcapi.tiles.domain.provider.TileProviderMbtilesData;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.ogcapi.tiles.domain.provider.TileStoreReadOnly;
import de.ii.xtraplatform.base.domain.AppContext;
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

  @Override
  public String getType() {
    return TileProviderMbtilesData.PROVIDER_TYPE;
  }
}
