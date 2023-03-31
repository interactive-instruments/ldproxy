/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableImplicitTiling.Builder.class)
public interface ImplicitTiling {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<ImplicitTiling> FUNNEL =
      (from, into) -> {
        into.putString(from.getSubdivisionScheme(), StandardCharsets.UTF_8);
        into.putInt(from.getAvailableLevels());
        into.putInt(from.getSubtreeLevels());
        Content.FUNNEL.funnel(from.getSubtrees(), into);
      };

  @Value.Default
  default String getSubdivisionScheme() {
    return "QUADTREE";
  }

  int getAvailableLevels();

  int getSubtreeLevels();

  Content getSubtrees();
}
