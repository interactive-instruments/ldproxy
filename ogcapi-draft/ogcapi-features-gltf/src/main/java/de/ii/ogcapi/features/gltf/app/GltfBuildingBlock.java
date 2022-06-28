/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.gltf.domain.ImmutableGltfConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Features glTF
 * @langEn The module *Features glTF* adds support for glTF 2.0 as a feature encoding. TODO
 * @langDe Das Modul *Features glTF* unterstützt glTF 2.0 als Kodierung für Features. TODO
 * @propertyTable {@link de.ii.ogcapi.features.gltf.domain.GltfConfiguration}
 */
@AutoBind
@Singleton
public class GltfBuildingBlock implements ApiBuildingBlock {

  @Inject
  public GltfBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableGltfConfiguration.Builder().enabled(false).build();
  }
}
