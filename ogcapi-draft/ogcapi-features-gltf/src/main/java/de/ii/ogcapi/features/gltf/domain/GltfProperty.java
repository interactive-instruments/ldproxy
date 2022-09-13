/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableGltfProperty.Builder.class)
public interface GltfProperty {

  // TODO support value arrays
  enum GLTF_TYPE {
    INT8,
    UINT8,
    INT16,
    UINT16,
    INT32,
    UINT32,
    INT64,
    UINT64,
    FLOAT32,
    FLOAT64,
    STRING,
    BOOLEAN
  }

  @SuppressWarnings("UnstableApiUsage")
  Funnel<GltfProperty> FUNNEL =
      (from, into) -> {
        into.putString(from.getType().name(), StandardCharsets.UTF_8);
        into.putString(from.getOffsetType().name(), StandardCharsets.UTF_8);
        from.getNoData().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
      };

  GLTF_TYPE getType();

  @Value.Default
  default GLTF_TYPE getOffsetType() {
    return GLTF_TYPE.UINT32;
  }

  Optional<String> getNoData();

  Optional<Boolean> getUseCode();

  @Value.Derived
  @Value.Auxiliary
  default boolean shouldUseCode() {
    return Boolean.TRUE.equals(getUseCode().orElse(false));
  }

  @Value.Check
  default void check() {
    Preconditions.checkState(
        getOffsetType() == GLTF_TYPE.UINT8
            || getOffsetType() == GLTF_TYPE.UINT16
            || getOffsetType() == GLTF_TYPE.UINT32,
        "The String Offset Type must be an 8-bit, 16-bit or 32-bit unsigned integer. Found: %s.",
        getOffsetType().name());
  }
}
