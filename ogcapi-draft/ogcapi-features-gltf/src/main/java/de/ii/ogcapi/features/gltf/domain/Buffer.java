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
@JsonDeserialize(builder = ImmutableBuffer.Builder.class)
public interface Buffer {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Buffer> FUNNEL =
      (from, into) -> {
        into.putInt(from.getByteLength());
        from.getUri().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getName().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
      };

  int getByteLength();

  Optional<String> getUri();

  Optional<String> getName();
}
