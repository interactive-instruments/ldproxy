/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.flatgeobuf.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.flatgeobuf.domain.ImmutableFlatgeobufConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Features FlatGeobuf
 * @langEn Encode features as [FlatGeobuf](https://flatgeobuf.org).
 * @langDe Enkodierung von Features als [FlatGeobuf](https://flatgeobuf.org).
 * @ref:cfgProperties {@link
 *     de.ii.ogcapi.features.flatgeobuf.domain.ImmutableFlatgeobufConfiguration}
 */
@Singleton
@AutoBind
public class FeaturesFlatgeobufBuildingBlock implements ApiBuildingBlock {

  public static int DEFAULT_MULTIPLICITY = 3;

  @Inject
  public FeaturesFlatgeobufBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableFlatgeobufConfiguration.Builder()
        .enabled(false)
        .maxMultiplicity(DEFAULT_MULTIPLICITY)
        .build();
  }
}
