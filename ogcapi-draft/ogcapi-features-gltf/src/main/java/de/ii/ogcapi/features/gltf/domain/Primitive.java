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
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutablePrimitive.Builder.class)
public interface Primitive {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Primitive> FUNNEL =
      (from, into) -> {
        into.putInt(from.getMode());
        from.getIndices().ifPresent(into::putInt);
        from.getMaterial().ifPresent(into::putInt);
        Attributes.FUNNEL.funnel(from.getAttributes(), into);
      };

  int getMode();

  Optional<Integer> getIndices();

  Optional<Integer> getMaterial();

  Attributes getAttributes();

  Map<String, Object> getExtensions();
}
