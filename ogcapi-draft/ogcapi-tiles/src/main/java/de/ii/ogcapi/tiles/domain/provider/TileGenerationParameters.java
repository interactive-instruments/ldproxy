/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface TileGenerationParameters {

  Optional<BoundingBox> getClipBoundingBox();

  Optional<PropertyTransformations> getPropertyTransformations();

  @Value.Derived
  default boolean isEmpty() {
    return getClipBoundingBox().isEmpty() && getPropertyTransformations().isEmpty();
  }
}
