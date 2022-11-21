/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

public interface TileGenerationParameters {

  @Value.Default
  default int getFeatureLimit() {
    return 100000;
  }

  @Value.Default
  default double getMinimumSizeInPixel() {
    return 0.5;
  }

  @Value.Default
  default boolean getIgnoreInvalidGeometries() {
    return false;
  }

  Map<String, List<LevelTransformation>> getTransformations();
}
