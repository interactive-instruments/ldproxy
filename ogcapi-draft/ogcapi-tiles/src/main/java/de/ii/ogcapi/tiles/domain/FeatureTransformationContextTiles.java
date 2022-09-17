/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import de.ii.ogcapi.features.core.domain.FeatureTransformationContext;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
public interface FeatureTransformationContextTiles extends FeatureTransformationContext {

  Map<String, Object> processingParameters();

  Tile tile();

  TileCache getTileCache();

  List<String> getGeometryProperty();

  @Value.Lazy
  default TilesConfiguration tilesConfiguration() {
    return getConfiguration(TilesConfiguration.class);
  }
}
