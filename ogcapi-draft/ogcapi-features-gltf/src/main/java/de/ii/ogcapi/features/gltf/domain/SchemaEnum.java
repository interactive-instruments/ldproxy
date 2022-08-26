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
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableSchemaEnum.Builder.class)
public interface SchemaEnum {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<SchemaEnum> FUNNEL =
      (from, into) -> {
        from.getName().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getDescription().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getValueType().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getValues().stream().forEach(v -> SchemaEnumValue.FUNNEL.funnel(v, into));
      };

  Optional<String> getName();

  Optional<String> getDescription();

  Optional<String> getValueType();

  List<SchemaEnumValue> getValues();
}
