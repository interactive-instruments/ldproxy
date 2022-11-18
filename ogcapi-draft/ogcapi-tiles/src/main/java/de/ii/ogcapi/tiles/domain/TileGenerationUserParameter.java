/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileGenerationUserParameters;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationSchema;
import java.util.Optional;

@AutoMultiBind
public interface TileGenerationUserParameter {

  void applyTo(
      ImmutableTileGenerationUserParameters.Builder userParametersBuilder,
      QueryParameterSet parameters,
      Optional<TileGenerationSchema> generationSchema);
}
