/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableMetadataEntity.Builder.class)
public interface MetadataEntity {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<MetadataEntity> FUNNEL =
      (from, into) -> {
        into.putString(from.getClazz(), StandardCharsets.UTF_8);
        from.getProperties().keySet().forEach(v -> into.putString(v, StandardCharsets.UTF_8));
      };

  @JsonProperty("class")
  String getClazz();

  Map<String, Object> getProperties();
}
