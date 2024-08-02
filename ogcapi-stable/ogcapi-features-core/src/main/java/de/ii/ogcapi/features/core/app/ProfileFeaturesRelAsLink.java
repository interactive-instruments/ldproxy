/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

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
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class ProfileFeaturesRelAsLink extends ProfileFeaturesRel {

  public static final String NAME = "rel-as-link";

  @Inject
  protected ProfileFeaturesRelAsLink(
      ExtensionRegistry extensionRegistry, FeaturesCoreProviders providers) {
    super(extensionRegistry, providers);
  }

  @Override
  public void addRefTransformations(FeatureSchema property, String mediaType, Builder builder) {
    if (mediaType.equals(MediaType.TEXT_HTML)) {
      reduceToLink(property, builder);
    } else {
      mapToLink(property, builder);
    }
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @Override
  public Optional<ProfileFeatures> negotiateProfile(@NotNull String mediaType) {
    if (mediaType.startsWith(MediaType.TEXT_HTML)
        || mediaType.startsWith("application/geo+json")
        || mediaType.startsWith("application/fg+json")
        || mediaType.startsWith("application/vnd.ogc.fg+json")
        || mediaType.startsWith("application/gml+xml")) {
      return Optional.of(this);
    }

    return extensionRegistry.getExtensionsForType(ProfileFeatures.class).stream()
        .filter(p -> ProfileFeaturesRelAsKey.NAME.equals(p.getName()))
        .findFirst();
  }
}
