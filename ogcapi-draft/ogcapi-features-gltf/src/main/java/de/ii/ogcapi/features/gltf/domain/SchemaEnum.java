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
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableSchemaEnum.Builder.class)
public interface SchemaEnum {

  EnumSet<ComponentType> VALUE_TYPES =
      EnumSet.of(
          ComponentType.INT8,
          ComponentType.UINT8,
          ComponentType.INT16,
          ComponentType.UINT16,
          ComponentType.INT32,
          ComponentType.UINT32,
          ComponentType.INT64,
          ComponentType.UINT64);

  @SuppressWarnings("UnstableApiUsage")
  Funnel<SchemaEnum> FUNNEL =
      (from, into) -> {
        from.getName().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getDescription().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        into.putString(from.getValueType().name(), StandardCharsets.UTF_8);
        from.getValues().forEach(v -> SchemaEnumValue.FUNNEL.funnel(v, into));
      };

  Optional<String> getName();

  Optional<String> getDescription();

  @Value.Default
  default ComponentType getValueType() {
    return ComponentType.UINT16;
  }

  List<SchemaEnumValue> getValues();

  @Value.Check
  default void check() {
    Preconditions.checkState(
        VALUE_TYPES.contains(getValueType()),
        "The Value Type of an enumeration must be an integer. Found: %s.",
        getValueType().name());
  }
}
