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
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableAttributes.Builder.class)
public interface Attributes {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Attributes> FUNNEL =
      (from, into) -> {
        from.getPosition().ifPresent(into::putInt);
        from.getNormal().ifPresent(into::putInt);
        from.getFeatureId0().ifPresent(into::putInt);
      };

  @JsonProperty("POSITION")
  Optional<Integer> getPosition();

  @JsonProperty("_FEATURE_ID_0")
  Optional<Integer> getFeatureId0();

  @JsonProperty("NORMAL")
  Optional<Integer> getNormal();
}
