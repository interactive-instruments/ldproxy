/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import static de.ii.xtraplatform.tiles.domain.TilesetFeatures.COMBINE_ALL;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.app.TilesBuildingBlock;
import de.ii.ogcapi.tiles.domain.TilesConfiguration.TileCacheType;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityMigration;
import de.ii.xtraplatform.tiles.domain.Cache;
import de.ii.xtraplatform.tiles.domain.Cache.Storage;
import de.ii.xtraplatform.tiles.domain.Cache.Type;
import de.ii.xtraplatform.tiles.domain.ImmutableCache;
import de.ii.xtraplatform.tiles.domain.ImmutableTileProviderFeaturesData;
import de.ii.xtraplatform.tiles.domain.ImmutableTileProviderHttpData;
import de.ii.xtraplatform.tiles.domain.ImmutableTileProviderMbtilesData;
import de.ii.xtraplatform.tiles.domain.ImmutableTilesetFeatures;
import de.ii.xtraplatform.tiles.domain.ImmutableTilesetFeaturesDefaults;
import de.ii.xtraplatform.tiles.domain.ImmutableTilesetHttp;
import de.ii.xtraplatform.tiles.domain.ImmutableTilesetHttpDefaults;
import de.ii.xtraplatform.tiles.domain.ImmutableTilesetMbTiles;
import de.ii.xtraplatform.tiles.domain.ImmutableTilesetMbTilesDefaults;
import de.ii.xtraplatform.tiles.domain.MinMax;
import de.ii.xtraplatform.tiles.domain.TileProviderData;
import de.ii.xtraplatform.tiles.domain.TileProviderFeaturesData;
import de.ii.xtraplatform.tiles.domain.TileProviderHttpData;
import de.ii.xtraplatform.tiles.domain.TileProviderMbtilesData;
import de.ii.xtraplatform.tiles.domain.TilesetFeatures;
import de.ii.xtraplatform.tiles.domain.WithCenter.LonLat;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class TilesMigrationV4 extends EntityMigration<OgcApiDataV2, OgcApiDataV2> {

  public TilesMigrationV4(EntityMigrationContext context) {
    super(context);
  }

  @Override
  public String getSubject() {
    return "building block TILES";
  }

  @Override
  public String getDescription() {
    return "is deprecated and will be migrated to a separate tile provider entity";
  }

  @Override
  public boolean isApplicable(EntityData entityData) {
    if (!(entityData instanceof OgcApiDataV2)) {
      return false;
    }

    OgcApiDataV2 apiData = (OgcApiDataV2) entityData;

    Optional<TilesConfiguration> tilesConfiguration =
        apiData.getExtension(TilesConfiguration.class);

    if (tilesConfiguration.isEmpty()
        || Objects.nonNull(tilesConfiguration.get().getTileProviderId())) {
      return false;
    }

    // TODO: might be in any group
    return !getContext()
        .exists(
            identifier ->
                Objects.equals(identifier.id(), TilesProviders.toTilesId(apiData.getId()))
                    && identifier.path().get(identifier.path().size() - 1).equals("providers"));
  }

  @Override
  public OgcApiDataV2 migrate(OgcApiDataV2 entityData) {
    Optional<TilesConfiguration> tilesConfigurationOld =
        entityData.getExtension(TilesConfiguration.class);

    if (tilesConfigurationOld.isEmpty()) {
      return entityData;
    }

    TilesConfiguration tilesConfiguration =
        new ImmutableTilesConfiguration.Builder()
            .from(tilesConfigurationOld.get())
            .tileProvider(null)
            .tileProviderId(TilesProviders.toTilesId(entityData.getId()))
            .cache(null)
            .tileEncodings(List.of())
            .center(List.of())
            .zoomLevels(Map.of())
            .zoomLevelsCache(Map.of())
            .seeding(Map.of())
            .seedingOptions(Optional.empty())
            .limit(null)
            .ignoreInvalidGeometries(null)
            .filters(Map.of())
            .rules(Map.of())
            .minimumSizeInPixel(null)
            .build();

    return OgcApiDataV2.replaceOrAddExtensions(entityData, tilesConfiguration);
  }

  @Override
  public Map<Identifier, ? extends EntityData> getAdditionalEntities(EntityData entityData) {
    Optional<Tuple<Class<? extends TileProviderData>, ? extends TileProviderData>>
        tileProviderData = getTileProviderData((OgcApiDataV2) entityData, true);

    if (tileProviderData.isPresent()) {
      return Map.of(
          Identifier.from(tileProviderData.get().second().getId(), "providers"),
          tileProviderData.get().second());
    }

    return Map.of();
  }

  public Optional<Tuple<Class<? extends TileProviderData>, ? extends TileProviderData>>
      getTileProviderData(OgcApiDataV2 apiData) {
    return getTileProviderData(apiData, false);
  }

  public Optional<Tuple<Class<? extends TileProviderData>, ? extends TileProviderData>>
      getTileProviderData(OgcApiDataV2 apiData, boolean ignoreEnabled) {
    Optional<TilesConfiguration> tiles =
        apiData
            .getExtension(TilesConfiguration.class)
            .filter(tilesConfiguration -> ignoreEnabled || tilesConfiguration.isEnabled());

    if (tiles.isEmpty()) {
      return Optional.empty();
    }

    Optional<FeaturesCoreConfiguration> featuresCore =
        apiData.getExtension(FeaturesCoreConfiguration.class);

    Map<String, FeatureTypeConfigurationOgcApi> collections =
        apiData.getCollections().entrySet().stream()
            .filter(
                entry ->
                    entry
                        .getValue()
                        .getExtension(TilesConfiguration.class)
                        .filter(TilesConfiguration::isEnabled)
                        .filter(TilesConfiguration::hasCollectionTiles)
                        .isPresent())
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    if (Objects.nonNull(tiles.get().getTileProvider())
        && tiles.get().getTileProvider() instanceof TileProviderMbtiles) {
      return Optional.of(
          Tuple.of(
              TileProviderMbtilesData.class,
              getMbTilesData(
                  apiData.getId(),
                  tiles.get(),
                  (TileProviderMbtiles) tiles.get().getTileProvider(),
                  collections)));
    }

    if (Objects.nonNull(tiles.get().getTileProvider())
        && tiles.get().getTileProvider() instanceof TileProviderTileServer) {
      return Optional.of(
          Tuple.of(
              TileProviderHttpData.class,
              getTileServerData(
                  apiData.getId(),
                  tiles.get(),
                  (TileProviderTileServer) tiles.get().getTileProvider(),
                  collections)));
    }

    return Optional.of(
        Tuple.of(
            TileProviderFeaturesData.class,
            getFeaturesData(apiData.getId(), tiles.get(), featuresCore, collections)));
  }

  private static TileProviderMbtilesData getMbTilesData(
      String apiId,
      TilesConfiguration tilesConfiguration,
      TileProviderMbtiles tileProviderMbtiles,
      Map<String, FeatureTypeConfigurationOgcApi> collections) {
    return new ImmutableTileProviderMbtilesData.Builder()
        .id(TilesProviders.toTilesId(apiId))
        .providerType(TileProviderMbtilesData.PROVIDER_TYPE)
        .providerSubType(TileProviderMbtilesData.PROVIDER_SUBTYPE)
        .tilesetDefaults(
            new ImmutableTilesetMbTilesDefaults.Builder()
                .putAllLevels(tilesConfiguration.getZoomLevelsDerived())
                .build())
        .putAllTilesets(
            tilesConfiguration.hasDatasetTiles()
                ? Map.of(
                    TilesBuildingBlock.DATASET_TILES,
                    new ImmutableTilesetMbTiles.Builder()
                        .id(TilesBuildingBlock.DATASET_TILES)
                        .source(Path.of(apiId, tileProviderMbtiles.getFilename()).toString())
                        .putAllLevels(tilesConfiguration.getZoomLevelsDerived())
                        .build())
                : Map.of())
        .putAllTilesets(
            collections.entrySet().stream()
                .map(
                    entry ->
                        new SimpleImmutableEntry<>(
                            entry.getKey(),
                            entry.getValue().getExtension(TilesConfiguration.class).get()))
                .filter(
                    entry ->
                        Objects.nonNull(entry.getValue().getTileProvider())
                            && entry.getValue().getTileProvider() instanceof TileProviderMbtiles
                            && Objects.nonNull(
                                ((TileProviderMbtiles) entry.getValue().getTileProvider())
                                    .getFilename()))
                .map(
                    entry ->
                        new SimpleImmutableEntry<>(
                            entry.getKey(),
                            new ImmutableTilesetMbTiles.Builder()
                                .id(entry.getKey())
                                .source(
                                    ((TileProviderMbtiles) entry.getValue().getTileProvider())
                                        .getFilename())
                                .putAllLevels(entry.getValue().getZoomLevelsDerived())
                                .build()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
        .build();
  }

  private static TileProviderHttpData getTileServerData(
      String apiId,
      TilesConfiguration tilesConfiguration,
      TileProviderTileServer tileProviderTileServer,
      Map<String, FeatureTypeConfigurationOgcApi> collections) {
    return new ImmutableTileProviderHttpData.Builder()
        .id(TilesProviders.toTilesId(apiId))
        .providerType(TileProviderHttpData.PROVIDER_TYPE)
        .providerSubType(TileProviderHttpData.PROVIDER_SUBTYPE)
        .tilesetDefaults(
            new ImmutableTilesetHttpDefaults.Builder()
                .putAllLevels(tilesConfiguration.getZoomLevelsDerived())
                .center(
                    tilesConfiguration.getCenterDerived().size() == 2
                        ? Optional.of(
                            LonLat.of(
                                tilesConfiguration.getCenterDerived().get(0),
                                tilesConfiguration.getCenterDerived().get(1)))
                        : Optional.empty())
                .encodings(
                    tilesConfiguration.getTileEncodingsDerived().stream()
                        .map(enc -> new SimpleImmutableEntry<>(enc, enc.toLowerCase()))
                        .collect(
                            ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
                .build())
        .putAllTilesets(
            tilesConfiguration.hasDatasetTiles()
                ? Map.of(
                    TilesBuildingBlock.DATASET_TILES,
                    new ImmutableTilesetHttp.Builder()
                        .id(TilesBuildingBlock.DATASET_TILES)
                        .urlTemplate(
                            tileProviderTileServer
                                .getUrlTemplate()
                                .replaceAll("\\{", "{{")
                                .replaceAll("}", "}}"))
                        .putAllLevels(tilesConfiguration.getZoomLevelsDerived())
                        .build())
                : Map.of())
        .putAllTilesets(
            collections.entrySet().stream()
                .map(
                    entry ->
                        new SimpleImmutableEntry<>(
                            entry.getKey(),
                            entry.getValue().getExtension(TilesConfiguration.class).get()))
                .filter(
                    entry ->
                        Objects.nonNull(tileProviderTileServer.getUrlTemplateSingleCollection()))
                .map(
                    entry ->
                        new SimpleImmutableEntry<>(
                            entry.getKey(),
                            new ImmutableTilesetHttp.Builder()
                                .id(entry.getKey())
                                .urlTemplate(
                                    tileProviderTileServer
                                        .getUrlTemplateSingleCollection()
                                        .replaceAll("\\{", "{{")
                                        .replaceAll("}", "}}"))
                                .putAllLevels(entry.getValue().getZoomLevelsDerived())
                                .center(
                                    entry.getValue().getCenterDerived().size() == 2
                                        ? Optional.of(
                                            LonLat.of(
                                                entry.getValue().getCenterDerived().get(0),
                                                entry.getValue().getCenterDerived().get(1)))
                                        : Optional.empty())
                                .build()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
        .build();
  }

  private static TileProviderFeaturesData getFeaturesData(
      String apiId,
      TilesConfiguration tilesConfiguration,
      Optional<FeaturesCoreConfiguration> featuresCore,
      Map<String, FeatureTypeConfigurationOgcApi> collections) {
    Map<String, TilesConfiguration> collectionConfigs =
        collections.entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(),
                        entry.getValue().getExtension(TilesConfiguration.class).get()))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    return new ImmutableTileProviderFeaturesData.Builder()
        .id(TilesProviders.toTilesId(apiId))
        .providerType(TileProviderFeaturesData.PROVIDER_TYPE)
        .providerSubType(TileProviderFeaturesData.PROVIDER_SUBTYPE)
        .addAllCaches(getCaches(tilesConfiguration, collectionConfigs))
        .seeding(tilesConfiguration.getSeedingOptionsDerived())
        .tilesetDefaults(
            new ImmutableTilesetFeaturesDefaults.Builder()
                .featureProvider(
                    featuresCore.flatMap(FeaturesCoreConfiguration::getFeatureProvider))
                .putAllLevels(tilesConfiguration.getZoomLevelsDerived())
                .putAllTransformations(tilesConfiguration.getRulesDerived())
                .featureLimit(tilesConfiguration.getLimitDerived())
                .minimumSizeInPixel(tilesConfiguration.getMinimumSizeInPixelDerived())
                .ignoreInvalidGeometries(tilesConfiguration.isIgnoreInvalidGeometriesDerived())
                .center(
                    tilesConfiguration.getCenterDerived().size() == 2
                        ? Optional.of(
                            LonLat.of(
                                tilesConfiguration.getCenterDerived().get(0),
                                tilesConfiguration.getCenterDerived().get(1)))
                        : Optional.empty())
                .build())
        .putAllTilesets(
            tilesConfiguration.hasDatasetTiles()
                ? Map.of(
                    TilesBuildingBlock.DATASET_TILES,
                    new ImmutableTilesetFeatures.Builder()
                        .id(TilesBuildingBlock.DATASET_TILES)
                        .addCombine(COMBINE_ALL)
                        .putAllLevels(tilesConfiguration.getZoomLevelsDerived())
                        .build())
                : Map.of())
        .putAllTilesets(
            collectionConfigs.entrySet().stream()
                .map(
                    entry ->
                        new SimpleImmutableEntry<>(
                            entry.getKey(),
                            getFeatureLayer(entry.getKey(), entry.getValue(), collections)))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
        .build();
  }

  private static List<Cache> getCaches(
      TilesConfiguration tilesConfiguration, Map<String, TilesConfiguration> collectionConfigs) {

    if (Objects.equals(tilesConfiguration.getCache(), TileCacheType.NONE)) {
      return List.of();
    }

    Storage storage =
        Objects.equals(tilesConfiguration.getCache(), TileCacheType.MBTILES)
            ? Storage.MBTILES
            : Storage.PLAIN;

    return List.of(
        new ImmutableCache.Builder()
            .type(Type.DYNAMIC)
            .storage(storage)
            .putAllLevels(tilesConfiguration.getSeedingDerived())
            .putAllTilesetLevels(
                collectionConfigs.entrySet().stream()
                    .map(
                        entry ->
                            new SimpleImmutableEntry<>(
                                entry.getKey(), entry.getValue().getSeedingDerived()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .build(),
        new ImmutableCache.Builder()
            .type(Type.DYNAMIC)
            .storage(storage)
            .seeded(false)
            .putAllLevels(getNonSeededRanges(tilesConfiguration))
            .putAllTilesetLevels(
                collectionConfigs.entrySet().stream()
                    .map(
                        entry ->
                            new SimpleImmutableEntry<>(
                                entry.getKey(), getNonSeededRanges(entry.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .build());
  }

  private static Map<String, MinMax> getNonSeededRanges(TilesConfiguration tilesConfiguration) {
    Map<String, MinMax> seeding = tilesConfiguration.getSeedingDerived();
    Map<String, MinMax> cache = tilesConfiguration.getZoomLevelsCacheDerived();

    return tilesConfiguration.getZoomLevelsDerived().entrySet().stream()
        .map(
            entry -> {
              if (seeding.containsKey(entry.getKey())) {
                Range<Integer> range;
                if (cache.containsKey(entry.getKey())) {
                  range =
                      Range.closed(
                          Math.max(
                              seeding.get(entry.getKey()).getMax() + 1,
                              cache.get(entry.getKey()).getMin()),
                          cache.get(entry.getKey()).getMax());
                } else {
                  range =
                      Range.closed(
                          Math.max(
                              Math.min(
                                  seeding.get(entry.getKey()).getMax() + 1,
                                  entry.getValue().getMax()),
                              entry.getValue().getMin()),
                          entry.getValue().getMax());
                }

                return new SimpleImmutableEntry<>(entry.getKey(), MinMax.of(range));
              } else if (cache.containsKey(entry.getKey())) {
                return new SimpleImmutableEntry<>(entry.getKey(), cache.get(entry.getKey()));
              }
              return entry;
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static TilesetFeatures getFeatureLayer(
      String id, TilesConfiguration cfg, Map<String, FeatureTypeConfigurationOgcApi> collections) {
    return new ImmutableTilesetFeatures.Builder()
        .id(id)
        .featureProvider(
            collections
                .get(id)
                .getExtension(FeaturesCoreConfiguration.class)
                .flatMap(FeaturesCoreConfiguration::getFeatureProvider))
        .featureType(
            collections
                .get(id)
                .getExtension(FeaturesCoreConfiguration.class)
                .flatMap(FeaturesCoreConfiguration::getFeatureType))
        .putAllLevels(cfg.getZoomLevelsDerived())
        .putAllTransformations(cfg.getRulesDerived())
        .putAllFilters(cfg.getFiltersDerived())
        .featureLimit(cfg.getLimitDerived())
        .minimumSizeInPixel(cfg.getMinimumSizeInPixelDerived())
        .ignoreInvalidGeometries(cfg.isIgnoreInvalidGeometriesDerived())
        .center(
            cfg.getCenterDerived().size() == 2
                ? Optional.of(
                    LonLat.of(cfg.getCenterDerived().get(0), cfg.getCenterDerived().get(1)))
                : Optional.empty())
        .build();
  }
}
