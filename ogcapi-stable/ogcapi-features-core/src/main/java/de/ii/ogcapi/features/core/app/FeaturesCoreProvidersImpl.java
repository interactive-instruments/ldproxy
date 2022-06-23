/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ExtendableConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class FeaturesCoreProvidersImpl implements FeaturesCoreProviders {

  private final EntityRegistry entityRegistry;

  @Inject
  public FeaturesCoreProvidersImpl(EntityRegistry entityRegistry) {
    this.entityRegistry = entityRegistry;
  }

  @Override
  public boolean hasFeatureProvider(OgcApiDataV2 apiData) {
    return getFeatureProvider(apiData).isPresent();
  }

  @Override
  public Optional<FeatureProvider2> getFeatureProvider(OgcApiDataV2 apiData) {
    Optional<FeatureProvider2> optionalFeatureProvider = getOptionalFeatureProvider(apiData);

    if (!optionalFeatureProvider.isPresent()) {
      optionalFeatureProvider = entityRegistry.getEntity(FeatureProvider2.class, apiData.getId());
    }
    return optionalFeatureProvider;
  }

  @Override
  public FeatureProvider2 getFeatureProviderOrThrow(OgcApiDataV2 apiData) {
    return getFeatureProvider(apiData)
        .orElseThrow(() -> new IllegalStateException("No feature provider found."));
  }

  @Override
  public boolean hasFeatureProvider(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureType) {
    return getFeatureProvider(apiData, featureType).isPresent();
  }

  @Override
  public Optional<FeatureProvider2> getFeatureProvider(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureType) {
    return getOptionalFeatureProvider(featureType).or(() -> getFeatureProvider(apiData));
  }

  @Override
  public FeatureProvider2 getFeatureProviderOrThrow(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureType) {
    return getOptionalFeatureProvider(featureType).orElse(getFeatureProviderOrThrow(apiData));
  }

  private Optional<FeatureProvider2> getOptionalFeatureProvider(
      ExtendableConfiguration extendableConfiguration) {
    return extendableConfiguration
        .getExtension(FeaturesCoreConfiguration.class)
        .filter(ExtensionConfiguration::isEnabled)
        .flatMap(FeaturesCoreConfiguration::getFeatureProvider)
        .flatMap(id -> entityRegistry.getEntity(FeatureProvider2.class, id));
  }
}
