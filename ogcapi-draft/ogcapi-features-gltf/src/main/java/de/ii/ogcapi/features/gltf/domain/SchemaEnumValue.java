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
import java.util.Objects;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableSchemaEnumValue.Builder.class)
public interface SchemaEnumValue {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<SchemaEnumValue> FUNNEL =
      (from, into) -> {
        into.putString(from.getName(), StandardCharsets.UTF_8);
        if (Objects.nonNull(from.getValue())) {
          if (from.getValue() instanceof Integer) {
            into.putInt((int) from.getValue());
          } else if (from.getValue() instanceof String) {
            into.putString((String) from.getValue(), StandardCharsets.UTF_8);
          }
        }
      };

  String getName();

  Object getValue();
}
