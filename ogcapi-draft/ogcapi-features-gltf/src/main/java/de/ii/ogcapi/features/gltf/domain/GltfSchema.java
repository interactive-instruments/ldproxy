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
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableGltfSchema.Builder.class)
public interface GltfSchema {

  String SCHEMA_REF = "#/components/schemas/Metadata3dSchema";

  @SuppressWarnings("UnstableApiUsage")
  Funnel<GltfSchema> FUNNEL =
      (from, into) -> {
        into.putString(from.getId(), StandardCharsets.UTF_8);
        from.getName().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getDescription().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getEnums()
            .entrySet()
            .forEach(
                entry -> {
                  into.putString(entry.getKey(), StandardCharsets.UTF_8);
                  SchemaEnum.FUNNEL.funnel(entry.getValue(), into);
                });
        from.getClasses()
            .entrySet()
            .forEach(
                entry -> {
                  into.putString(entry.getKey(), StandardCharsets.UTF_8);
                  SchemaClass.FUNNEL.funnel(entry.getValue(), into);
                });
      };

  String getId();

  Optional<String> getName();

  Optional<String> getDescription();

  Optional<String> getVersion();

  Map<String, SchemaEnum> getEnums();

  Map<String, SchemaClass> getClasses();
}
