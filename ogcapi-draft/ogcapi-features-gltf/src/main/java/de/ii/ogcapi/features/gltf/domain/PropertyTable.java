/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutablePropertyTable.Builder.class)
public interface PropertyTable {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<PropertyTable> FUNNEL =
      (from, into) -> {
        from.getName().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        into.putString(from.getClass_(), StandardCharsets.UTF_8);
        into.putInt(from.getCount());
        from.getProperties()
            .forEach(
                (key, value) -> {
                  into.putString(key, StandardCharsets.UTF_8);
                  Property.FUNNEL.funnel(value, into);
                });
      };

  Optional<String> getName();

  @JsonProperty("class")
  String getClass_();

  int getCount();

  Map<String, Property> getProperties();
}
