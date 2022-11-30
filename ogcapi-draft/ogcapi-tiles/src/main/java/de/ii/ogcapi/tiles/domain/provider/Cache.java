/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.tilematrixsets.domain.MinMax;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCache.Builder.class)
public interface Cache extends WithTmsLevels {
  enum Type {
    DYNAMIC
  }

  enum Storage {
    PLAIN,
    MBTILES
  }

  Type getType();

  Storage getStorage();

  @Value.Default
  default boolean doNotSeed() {
    return false;
  }

  @Override
  Map<String, MinMax> getLevels();
}
