/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.codelists.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.ImmutableProfileTransformations.Builder;
import de.ii.ogcapi.features.core.domain.ProfileFeatures;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;

@Singleton
@AutoBind
public class ProfileFeaturesValAsTitle extends ProfileFeaturesVal {

  public static final String NAME = "val-as-title";

  @Inject
  protected ProfileFeaturesValAsTitle(
      ExtensionRegistry extensionRegistry, FeaturesCoreProviders providers) {
    super(extensionRegistry, providers);
  }

  @Override
  public void addValTransformations(FeatureSchema property, String mediaType, Builder builder) {
    mapToTitle(property, builder);
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Optional<ProfileFeatures> negotiateProfile(@NotNull String mediaType) {
    if (mediaType.startsWith("application/gml+xml")) {
      return extensionRegistry.getExtensionsForType(ProfileFeatures.class).stream()
          .filter(p -> ProfileFeaturesValAsCode.NAME.equals(p.getName()))
          .findFirst();
    }

    return Optional.of(this);
  }
}