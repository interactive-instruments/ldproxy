/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableProperty.Builder.class)
public interface Property {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Property> FUNNEL =
      (from, into) -> {
        into.putInt(from.getValues());
        from.getArrayOffsets().ifPresent(into::putInt);
        from.getStringOffsets().ifPresent(into::putInt);
        from.getArrayOffsetType().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getStringOffsetType().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        // TODO other properties
      };

  int getValues();

  Optional<Integer> getArrayOffsets();

  Optional<Integer> getStringOffsets();

  Optional<String> getArrayOffsetType();

  Optional<String> getStringOffsetType();

  // TODO these can be a number an array or an array of an array
  Optional<Number> getOffset();

  Optional<Number> getScale();

  Optional<Number> getMin();

  Optional<Number> getMax();
}
