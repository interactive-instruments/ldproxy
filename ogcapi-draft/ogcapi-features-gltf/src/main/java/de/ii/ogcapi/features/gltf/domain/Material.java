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
@JsonDeserialize(builder = ImmutableMaterial.Builder.class)
public interface Material {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Material> FUNNEL =
      (from, into) -> {
        PbrMetallicRoughness.FUNNEL.funnel(from.getPbrMetallicRoughness(), into);
        from.getName().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getDoubleSided().ifPresent(into::putBoolean);
      };

  PbrMetallicRoughness getPbrMetallicRoughness();

  Optional<String> getName();

  Optional<Boolean> getDoubleSided();
}
