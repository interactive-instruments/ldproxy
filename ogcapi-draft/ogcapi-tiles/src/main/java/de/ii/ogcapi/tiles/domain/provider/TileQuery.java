/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import java.util.Optional;
import javax.ws.rs.core.MediaType;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, attributeBuilderDetection = true)
public interface TileQuery extends TileCoordinates {
  String getLayer();

  MediaType getMediaType();

  Optional<TileGenerationParameters> getGenerationParameters();

  // TODO: is there really a practical use case for these or should we drop them altogether?
  Optional<TileGenerationParametersTransient> getGenerationParametersTransient();

  @Value.Derived
  default boolean isTransient() {
    return getGenerationParametersTransient().isPresent()
        && !getGenerationParametersTransient().get().isEmpty();
  }
}
