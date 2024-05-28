/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import static de.ii.ogcapi.tiles.app.TilesBuildingBlock.DATASET_TILES;

import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.base.domain.resiliency.OptionalVolatileCapability;
import de.ii.xtraplatform.tiles.domain.TileAccess;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TilesetMetadata;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface TilesProviders {

  static String toTilesId(String apiId) {
    return String.format("%s-tiles", apiId);
  }

  boolean hasTileProvider(OgcApiDataV2 apiData);

  Optional<TileProvider> getTileProvider(OgcApiDataV2 apiData);

  default <T> Optional<T> getTileProvider(
      OgcApiDataV2 apiData, Function<TileProvider, OptionalVolatileCapability<T>> capability) {
    return getTileProvider(apiData)
        .map(capability)
        .filter(OptionalVolatileCapability::isAvailable)
        .map(OptionalVolatileCapability::get);
  }

  default TileProvider getTileProviderOrThrow(OgcApiDataV2 apiData) {
    return getTileProvider(apiData)
        .orElseThrow(() -> new IllegalStateException("No tile provider found."));
  }

  default <T> T getTileProviderOrThrow(
      OgcApiDataV2 apiData, Function<TileProvider, OptionalVolatileCapability<T>> capability) {
    return getTileProvider(apiData, capability)
        .orElseThrow(() -> new IllegalStateException("No tile provider found."));
  }

  default Optional<TilesetMetadata> getTilesetMetadata(OgcApiDataV2 apiData) {
    Optional<TileAccess> optionalProvider =
        getTileProvider(apiData)
            .filter(provider -> provider.access().isAvailable())
            .map(provider -> provider.access().get());
    return getTilesetId(apiData)
        .flatMap(
            tilesetId -> optionalProvider.flatMap(provider -> provider.getMetadata(tilesetId)));
  }

  default Optional<String> getTilesetId(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(TilesConfiguration.class)
        .map(cfg -> Optional.ofNullable(cfg.getTileProviderTileset()).orElse(DATASET_TILES));
  }

  default Map<String, TilesetMetadata> getRasterTilesetMetadata(OgcApiDataV2 apiData) {
    Optional<TileAccess> optionalProvider =
        getTileProvider(apiData)
            .filter(provider -> provider.access().isAvailable())
            .map(provider -> provider.access().get());
    if (optionalProvider.isEmpty()) {
      return ImmutableMap.of();
    }

    Optional<String> optionalTilesetId = getTilesetId(apiData);
    if (optionalTilesetId.isEmpty()) {
      return ImmutableMap.of();
    }

    return optionalProvider.get().getMapStyles(optionalTilesetId.get()).stream()
        .collect(
            Collectors.toMap(
                styleId -> styleId,
                styleId ->
                    optionalProvider
                        .flatMap(provider -> provider.getMetadata(optionalTilesetId.get(), styleId))
                        .get()));
  }

  default TilesetMetadata getTilesetMetadataOrThrow(OgcApiDataV2 apiData) {
    return getTilesetMetadata(apiData)
        .orElseThrow(() -> new IllegalStateException("No tileset metadata found."));
  }

  boolean hasTileProvider(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData);

  Optional<TileProvider> getTileProvider(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData);

  default <T> Optional<T> getTileProvider(
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      Function<TileProvider, OptionalVolatileCapability<T>> capability) {
    return getTileProvider(apiData, collectionData)
        .map(capability)
        .filter(OptionalVolatileCapability::isAvailable)
        .map(OptionalVolatileCapability::get);
  }

  TileProvider getTileProviderOrThrow(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData);

  default <T> T getTileProviderOrThrow(
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      Function<TileProvider, OptionalVolatileCapability<T>> capability) {
    return getTileProvider(apiData, collectionData, capability)
        .orElseThrow(() -> new IllegalStateException("No tile provider found."));
  }

  default Optional<TilesetMetadata> getTilesetMetadata(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {
    Optional<TileAccess> optionalProvider =
        getTileProvider(apiData, collectionData)
            .filter(provider -> provider.access().isAvailable())
            .map(provider -> provider.access().get());
    return getTilesetId(collectionData)
        .flatMap(
            tilesetId -> optionalProvider.flatMap(provider -> provider.getMetadata(tilesetId)));
  }

  default Optional<String> getTilesetId(FeatureTypeConfigurationOgcApi collectionData) {
    return collectionData
        .getExtension(TilesConfiguration.class)
        .map(
            cfg ->
                Optional.ofNullable(cfg.getTileProviderTileset()).orElse(collectionData.getId()));
  }

  default Map<String, TilesetMetadata> getRasterTilesetMetadata(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {
    Optional<TileAccess> optionalProvider =
        getTileProvider(apiData, collectionData)
            .filter(provider -> provider.access().isAvailable())
            .map(provider -> provider.access().get());
    if (optionalProvider.isEmpty()) {
      return ImmutableMap.of();
    }

    Optional<String> optionalTilesetId = getTilesetId(collectionData);
    if (optionalTilesetId.isEmpty()) {
      return ImmutableMap.of();
    }

    return optionalProvider.get().getMapStyles(optionalTilesetId.get()).stream()
        .collect(
            Collectors.toMap(
                styleId -> styleId,
                styleId ->
                    optionalProvider
                        .flatMap(provider -> provider.getMetadata(optionalTilesetId.get(), styleId))
                        .get()));
  }

  default TilesetMetadata getTilesetMetadataOrThrow(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {
    return getTilesetMetadata(apiData, collectionData)
        .orElseThrow(() -> new IllegalStateException("No tileset metadata found."));
  }

  default boolean hasTileProvider(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? hasTileProvider(apiData, collectionData.get())
        : hasTileProvider(apiData);
  }

  default Optional<TileProvider> getTileProvider(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? getTileProvider(apiData, collectionData.get())
        : getTileProvider(apiData);
  }

  default TileProvider getTileProviderOrThrow(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? getTileProviderOrThrow(apiData, collectionData.get())
        : getTileProviderOrThrow(apiData);
  }

  default <T> T getTileProviderOrThrow(
      OgcApiDataV2 apiData,
      Optional<FeatureTypeConfigurationOgcApi> collectionData,
      Function<TileProvider, OptionalVolatileCapability<T>> capability) {
    return collectionData.isPresent()
        ? getTileProviderOrThrow(apiData, collectionData.get(), capability)
        : getTileProviderOrThrow(apiData, capability);
  }

  default Optional<TilesetMetadata> getTilesetMetadata(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? getTilesetMetadata(apiData, collectionData.get())
        : getTilesetMetadata(apiData);
  }

  default Map<String, TilesetMetadata> getRasterTilesetMetadata(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? getRasterTilesetMetadata(apiData, collectionData.get())
        : getRasterTilesetMetadata(apiData);
  }

  default TilesetMetadata getTilesetMetadataOrThrow(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? getTilesetMetadataOrThrow(apiData, collectionData.get())
        : getTilesetMetadataOrThrow(apiData);
  }
}
