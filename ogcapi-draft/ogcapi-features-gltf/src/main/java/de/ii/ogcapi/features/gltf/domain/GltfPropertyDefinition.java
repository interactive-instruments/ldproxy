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
import de.ii.ogcapi.features.gltf.domain.SchemaProperty.ComponentType;
import de.ii.ogcapi.features.gltf.domain.SchemaProperty.Type;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

/** */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableGltfPropertyDefinition.Builder.class)
public interface GltfPropertyDefinition {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<GltfPropertyDefinition> FUNNEL =
      (from, into) -> {
        into.putString(from.getType().name(), StandardCharsets.UTF_8);
        from.getComponentType().ifPresent(v -> into.putString(v.name(), StandardCharsets.UTF_8));
        into.putString(from.getStringOffsetType().name(), StandardCharsets.UTF_8);
        into.putString(from.getNoData(), StandardCharsets.UTF_8);
      };

  Type getType();

  Optional<ComponentType> getComponentType();

  @Value.Default
  default ComponentType getStringOffsetType() {
    return ComponentType.UINT32;
  }

  @Value.Default
  default String getNoData() {
    return getType() == Type.STRING ? "" : "0";
  }

  @Value.Check
  default void check() {
    Preconditions.checkState(
        getStringOffsetType() == ComponentType.UINT8
            || getStringOffsetType() == ComponentType.UINT16
            || getStringOffsetType() == ComponentType.UINT32,
        "The String Offset Type must be an 8-bit, 16-bit or 32-bit unsigned integer. Found: %s.",
        getStringOffsetType().name());
    Preconditions.checkState(
        getType() != Type.SCALAR || getComponentType().isPresent(),
        "The Component Type must be specified for a SCALAR property.");
    Preconditions.checkState(
        getType() != Type.ENUM || getComponentType().isPresent(),
        "The Component Type must be specified for a ENUM property.");
  }
}
