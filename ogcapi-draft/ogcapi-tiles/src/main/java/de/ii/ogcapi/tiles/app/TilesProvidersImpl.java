/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ExtendableConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.ogcapi.tiles.domain.provider.TileProvider;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class TilesProvidersImpl implements TilesProviders {

  private final EntityRegistry entityRegistry;

  @Inject
  public TilesProvidersImpl(EntityRegistry entityRegistry) {
    this.entityRegistry = entityRegistry;
  }

  @Override
  public boolean hasTileProvider(OgcApiDataV2 apiData) {
    return getTileProvider(apiData).isPresent();
  }

  @Override
  public Optional<TileProvider> getTileProvider(OgcApiDataV2 apiData) {
    Optional<TileProvider> optionalTileProvider = getOptionalTileProvider(apiData);

    if (!optionalTileProvider.isPresent()) {
      optionalTileProvider =
          entityRegistry.getEntity(TileProvider.class, String.format("%s-tiles", apiData.getId()));
    }
    return optionalTileProvider;
  }

  @Override
  public TileProvider getTileProviderOrThrow(OgcApiDataV2 apiData) {
    return getTileProvider(apiData)
        .orElseThrow(() -> new IllegalStateException("No tile provider found."));
  }

  @Override
  public boolean hasTileProvider(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {
    return getTileProvider(apiData, collectionData).isPresent();
  }

  @Override
  public Optional<TileProvider> getTileProvider(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {
    return getOptionalTileProvider(collectionData).or(() -> getTileProvider(apiData));
  }

  @Override
  public TileProvider getTileProviderOrThrow(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {
    return getOptionalTileProvider(collectionData).orElse(getTileProviderOrThrow(apiData));
  }

  private Optional<TileProvider> getOptionalTileProvider(
      ExtendableConfiguration extendableConfiguration) {
    return extendableConfiguration
        .getExtension(TilesConfiguration.class)
        .filter(ExtensionConfiguration::isEnabled)
        // TODO
        .map(c -> "USE_FALLBACK")
        // .flatMap(TilesConfiguration::getTileProviderRef)
        .flatMap(id -> entityRegistry.getEntity(TileProvider.class, id));
  }
}
