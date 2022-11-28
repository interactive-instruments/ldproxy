/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import de.ii.ogcapi.features.core.domain.EncodingContextSfFlat;
import org.immutables.value.Value;

@Value.Immutable
public interface TileGenerationContext extends EncodingContextSfFlat {

  TileCoordinates getCoordinates();

  TileGenerationOptions getParameters();

  // String getCollectionId(); // TODO: layer
}
