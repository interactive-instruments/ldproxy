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
import java.util.EnumSet;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableSchemaProperty.Builder.class)
public interface SchemaProperty {

  enum Type {
    SCALAR,
    VEC2,
    VEC3,
    VEC4,
    MAT2,
    MAT3,
    MAT4,
    STRING,
    BOOLEAN,
    ENUM
  }

  enum ComponentType {
    INT8,
    UINT8,
    INT16,
    UINT16,
    INT32,
    UINT32,
    INT64,
    UINT64,
    FLOAT32,
    FLOAT64
  }

  EnumSet<Type> SUPPORTED_TYPES = EnumSet.of(Type.SCALAR, Type.STRING, Type.ENUM);

  @SuppressWarnings("UnstableApiUsage")
  Funnel<SchemaProperty> FUNNEL =
      (from, into) -> {
        from.getDescription().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        into.putString(from.getType().name(), StandardCharsets.UTF_8);
        from.getComponentType().ifPresent(v -> into.putString(v.name(), StandardCharsets.UTF_8));
        from.getEnumType().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getRequired().ifPresent(into::putBoolean);
      };

  Optional<String> getDescription();

  Type getType();

  Optional<ComponentType> getComponentType();

  Optional<String> getEnumType();

  Optional<String> getNoData();

  Optional<Boolean> getRequired();

  @Value.Check
  default void check() {
    Preconditions.checkState(
        SUPPORTED_TYPES.contains(getType()),
        "Currently only SCALAR, STRING and ENUM types are supported. Found: %s.",
        getType().name());
  }
}
