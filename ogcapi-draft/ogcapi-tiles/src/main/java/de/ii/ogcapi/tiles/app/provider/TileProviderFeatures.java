/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import static de.ii.ogcapi.foundation.domain.FoundationConfiguration.CACHE_DIR;

import com.google.common.collect.Range;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimits;
import de.ii.ogcapi.tiles.app.provider.TileCacheDynamic.FileStoreFs;
import de.ii.ogcapi.tiles.app.provider.TileCacheDynamic.TileStore;
import de.ii.ogcapi.tiles.app.provider.TileCacheDynamic.TileStoreFiles;
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
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.store.domain.entities.AbstractPersistentEntity;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileProviderFeatures extends AbstractPersistentEntity<TileProviderFeaturesData>
    implements TileProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileProviderFeatures.class);
  private static final String TILES_DIR_NAME = "tiles";

  private final TileGeneratorFeatures tileGenerator;
  private final TileEncoders tileEncoders;
  private final ChainedTileProvider generatorProviderChain;
  private final ChainedTileProvider combinerProviderChain;
  private final List<TileCacheDynamic.TileStore> tileCaches;

  @AssistedInject
  public TileProviderFeatures(
      CrsInfo crsInfo,
      EntityRegistry entityRegistry,
      AppContext appContext,
      Cql cql,
      @Assisted TileProviderFeaturesData data) {
    super(data);

    this.tileGenerator = new TileGeneratorFeatures(data, crsInfo, entityRegistry, cql);
    this.tileCaches = new ArrayList<>();

    ChainedTileProvider current = tileGenerator;
    Path cacheRootDir =
        appContext
            .getDataDir()
            .resolve(CACHE_DIR)
            .resolve(TILES_DIR_NAME)
            .resolve(data.getId().replace("-tiles", ""));

    for (int i = 0; i < data.getCaches().size(); i++) {
      Cache cache = data.getCaches().get(i);
      // TODO: stay backwards compatible? or move to new dir?
      Path cacheDir =
          data.getCaches().size() == 1
              ? cacheRootDir
              : cacheRootDir.resolve(String.format("cache_%d", i));

      if (cache.getType() == Type.DYNAMIC) {
        if (cache.getStorage() == Storage.FILES) {
          FileStoreFs fileStore = new FileStoreFs(cacheDir);
          TileStore tileStore = new TileStoreFiles(fileStore);
          tileCaches.add(tileStore);
          // TODO: cacheLevels
          current = new TileCacheDynamic(tileStore, current, data.getTmsRanges());
        } else if (cache.getStorage() == Storage.MBTILES) {
          TileStore tileStore =
              TileStoreMbTiles.readWrite(
                  cacheDir, data.getId(), getTileSchemas(tileGenerator, data.getLayers()));
          tileCaches.add(tileStore);
          // TODO: cacheLevels
          current = new TileCacheDynamic(tileStore, current, data.getTmsRanges());
        }
      }
    }

    this.generatorProviderChain = current;

    this.tileEncoders = new TileEncoders(data, generatorProviderChain);
    current = tileEncoders;

    for (int i = 0; i < data.getCaches().size(); i++) {
      Cache cache = data.getCaches().get(i);

      if (cache.getType() == Type.DYNAMIC) {
        // TODO: cacheLevels
        current = new TileCacheDynamic(tileCaches.get(i), current, data.getTmsRanges());
      }
    }

    this.combinerProviderChain = current;
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
    for (TileStore cache : tileCaches) {
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
