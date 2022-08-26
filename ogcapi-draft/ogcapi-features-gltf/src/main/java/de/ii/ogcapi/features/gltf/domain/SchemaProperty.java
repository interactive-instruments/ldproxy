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
@JsonDeserialize(builder = ImmutableSchemaProperty.Builder.class)
public interface SchemaProperty {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<SchemaProperty> FUNNEL =
      (from, into) -> {
        from.getDescription().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        into.putString(from.getType(), StandardCharsets.UTF_8);
        from.getComponentType().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getEnumType().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getRequired().ifPresent(into::putBoolean);
      };

  Optional<String> getDescription();

  String getType(); // TODO use enum, not string

  Optional<String> getComponentType(); // TODO use enum, not string

  Optional<String> getEnumType(); // TODO use enum, not string

  Optional<Boolean> getRequired();
}
