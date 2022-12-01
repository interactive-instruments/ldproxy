/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimits;
import de.ii.ogcapi.tiles.domain.provider.Cache;
import de.ii.ogcapi.tiles.domain.provider.Cache.Storage;
import de.ii.ogcapi.tiles.domain.provider.Cache.Type;
import de.ii.ogcapi.tiles.domain.provider.ChainedTileProvider;
import de.ii.ogcapi.tiles.domain.provider.LayerOptionsFeatures;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationParameters;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationSchema;
import de.ii.ogcapi.tiles.domain.provider.TileGenerator;
import de.ii.ogcapi.tiles.domain.provider.TileProvider;
import de.ii.ogcapi.tiles.domain.provider.TileProviderFeaturesData;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.ogcapi.tiles.domain.provider.TileSeeding;
import de.ii.ogcapi.tiles.domain.provider.TileStore;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.services.domain.TaskContext;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.store.domain.entities.AbstractPersistentEntity;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileProviderFeatures extends AbstractPersistentEntity<TileProviderFeaturesData>
    implements TileProvider, TileSeeding {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileProviderFeatures.class);
  private static final String TILES_DIR_NAME = "tiles";

  private final TileGeneratorFeatures tileGenerator;
  private final TileEncoders tileEncoders;
  private final ChainedTileProvider generatorProviderChain;
  private final ChainedTileProvider combinerProviderChain;
  private final List<TileStore> tileStores;
  private final List<ChainedTileProvider> tileCaches;

  @AssistedInject
  public TileProviderFeatures(
      CrsInfo crsInfo,
      EntityRegistry entityRegistry,
      AppContext appContext,
      Cql cql,
      BlobStore blobStore,
      TileWalker tileWalker,
      @Assisted TileProviderFeaturesData data) {
    super(data);

    this.tileGenerator = new TileGeneratorFeatures(data, crsInfo, entityRegistry, cql);
    this.tileStores = new ArrayList<>();
    this.tileCaches = new ArrayList<>();

    BlobStore tilesStore = blobStore.with(TILES_DIR_NAME, clean(data.getId()));
    ChainedTileProvider current = tileGenerator;

    for (int i = 0; i < data.getCaches().size(); i++) {
      Cache cache = data.getCaches().get(i);
      // TODO: stay backwards compatible? or move to new dir? or cleanup old in routine?
      BlobStore cacheStore =
          data.getCaches().size() == 1 && cache.getType() == Type.DYNAMIC
              ? tilesStore
              : tilesStore.with(String.format("cache_%s", cache.getType().getSuffix()));

      if (cache.getType() == Type.DYNAMIC) {
        TileStore tileStore =
            cache.getStorage() == Storage.MBTILES
                ? TileStoreMbTiles.readWrite(
                    cacheStore, data.getId(), getTileSchemas(tileGenerator, data.getLayers()))
                : new TileStorePlain(cacheStore);

        tileStores.add(tileStore);
        // TODO: cacheLevels
        current = new TileCacheDynamic(tileStore, current, data.getTmsRanges());
        tileCaches.add(current);
      } else if (cache.getType() == Type.IMMUTABLE) {
        TileStore tileStore =
            new TileStoreMulti(
                cacheStore,
                cache.getStorage(),
                data.getId(),
                getTileSchemas(tileGenerator, data.getLayers()));
        tileStores.add(tileStore);
        // TODO: cacheLevels
        current =
            new TileCacheImmutable(
                tileWalker,
                tileStore,
                current,
                Map.of("governmentalservice", cache.getTmsRanges()));
        tileCaches.add(current);
      }
    }

    this.generatorProviderChain = current;

    this.tileEncoders = new TileEncoders(data, generatorProviderChain);
    current = tileEncoders;

    for (int i = 0; i < data.getCaches().size(); i++) {
      Cache cache = data.getCaches().get(i);

      if (cache.getType() == Type.DYNAMIC) {
        // TODO: cacheLevels
        current = new TileCacheDynamic(tileStores.get(i), current, data.getTmsRanges());
      }
    }

    this.combinerProviderChain = current;
  }

  static String clean(String id) {
    return id.replace("-tiles", "").replace("tiles-", "");
  }

  private static Map<String, Map<String, TileGenerationSchema>> getTileSchemas(
      TileGeneratorFeatures tileGenerator, Map<String, LayerOptionsFeatures> layers) {
    return layers.values().stream()
        .map(
            layer -> {
              Map<String, TileGenerationSchema> schemas =
                  layer.isCombined()
                      ? layer.getCombine().stream()
                          .flatMap(
                              subLayer -> {
                                if (Objects.equals(subLayer, LayerOptionsFeatures.COMBINE_ALL)) {
                                  return layers.entrySet().stream()
                                      .filter(entry -> !entry.getValue().isCombined())
                                      .map(Entry::getKey);
                                }
                                return Stream.of(subLayer);
                              })
                          .map(
                              subLayer ->
                                  new SimpleImmutableEntry<>(
                                      subLayer,
                                      tileGenerator.getGenerationSchema(subLayer, Map.of())))
                          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                      : Map.of(
                          layer.getId(),
                          tileGenerator.getGenerationSchema(layer.getId(), Map.of()));

              return new SimpleImmutableEntry<>(layer.getId(), schemas);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    LayerOptionsFeatures layer = getData().getLayers().get(tile.getLayer());
    TileResult result =
        layer.isCombined() ? combinerProviderChain.get(tile) : generatorProviderChain.get(tile);

    if (result.isNotFound() && tileEncoders.canEncode(tile.getMediaType())) {
      return TileResult.notFound(tileEncoders.empty(tile.getMediaType(), tile.getTileMatrixSet()));
    }

    return result;
  }

  // TODO: add to TileCacheDynamic, use canProvide + clip limits
  @Override
  public void deleteFromCache(
      String layer, TileMatrixSet tileMatrixSet, TileMatrixSetLimits limits) {
    for (TileStore cache : tileStores) {
      try {
        cache.delete(layer, tileMatrixSet, limits);
      } catch (IOException e) {

      }
    }
  }

  @Override
  public boolean supportsGeneration() {
    return true;
  }

  @Override
  public TileGenerator generator() {
    return tileGenerator;
  }

  @Override
  public String getType() {
    return TileProviderFeaturesData.PROVIDER_TYPE;
  }

  // TODO
  @Override
  public void seed(
      Map<String, TileGenerationParameters> layers,
      List<MediaType> mediaTypes,
      TaskContext taskContext)
      throws IOException {
    for (ChainedTileProvider cache : tileCaches) {
      if (cache instanceof TileCacheImmutable) {
        ((TileCacheImmutable) cache).seed(layers, mediaTypes, taskContext);
      }
    }
  }
}
