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
@JsonDeserialize(builder = ImmutableBufferView.Builder.class)
public interface BufferView {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<BufferView> FUNNEL =
      (from, into) -> {
        into.putInt(from.getBuffer());
        into.putInt(from.getByteLength());
        into.putInt(from.getByteOffset());
        from.getByteStride().ifPresent(into::putInt);
        from.getTarget().ifPresent(into::putInt);
        from.getName().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
      };

  int getBuffer();

  int getByteLength();

  int getByteOffset();

  Optional<Integer> getByteStride();

  Optional<Integer> getTarget();

  Optional<String> getName();
}
