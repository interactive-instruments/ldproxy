/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.tiles3d.domain.ImmutableTiles3dConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title TODO
 * @langEn TODO
 * @langDe TODO
 * @propertyTable {@link de.ii.ogcapi.tiles3d.domain.Tiles3dConfiguration}
 */
@AutoBind
@Singleton
public class Tiles3dBuildingBlock implements ApiBuildingBlock {

  @Inject
  public Tiles3dBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutableTiles3dConfiguration.Builder()
        .enabled(false)
        .availableLevels(9)
        .subtreeLevels(3)
        .build();
  }
}
