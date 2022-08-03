/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.gltf.domain.Immutable_3dTilesConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title TODO
 * @langEn TODO
 * @langDe TODO
 * @propertyTable {@link de.ii.ogcapi.features.gltf.domain._3dTilesConfiguration}
 */
@AutoBind
@Singleton
public class _3dTilesBuildingBlock implements ApiBuildingBlock {

  @Inject
  public _3dTilesBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Immutable_3dTilesConfiguration.Builder().enabled(false).build();
  }
}
