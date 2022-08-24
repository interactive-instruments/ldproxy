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
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableAccessor.Builder.class)
public interface Accessor {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Accessor> FUNNEL =
      (from, into) -> {
        into.putInt(from.getBufferView());
        into.putInt(from.getByteOffset());
        into.putInt(from.getComponentType());
        into.putBoolean(from.getNormalized());
        into.putInt(from.getCount());
        from.getMin().forEach(v -> into.putDouble(v.doubleValue()));
        from.getMax().forEach(v -> into.putDouble(v.doubleValue()));
        into.putString(from.getType(), StandardCharsets.UTF_8);
      };

  int getBufferView();

  int getByteOffset();

  int getComponentType();

  @Value.Default
  default boolean getNormalized() {
    return false;
  }

  int getCount();

  List<Number> getMax();

  List<Number> getMin();

  String getType();
}
