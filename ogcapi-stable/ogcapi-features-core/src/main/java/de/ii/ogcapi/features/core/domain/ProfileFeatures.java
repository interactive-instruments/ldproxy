/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import de.ii.ogcapi.features.core.domain.ImmutableProfileTransformations.Builder;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.ProfileExtension;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public abstract class ProfileFeatures implements ProfileExtension {

  protected final ExtensionRegistry extensionRegistry;
  protected final FeaturesCoreProviders providers;

  protected ProfileFeatures(ExtensionRegistry extensionRegistry, FeaturesCoreProviders providers) {
    this.extensionRegistry = extensionRegistry;
    this.providers = providers;
  }

  public abstract Optional<ProfileFeatures> negotiateProfile(@NotNull String mediaType);

  public abstract void addPropertyTransformations(
      @NotNull FeatureSchema schema, @NotNull String mediaType, @NotNull Builder builder);
}