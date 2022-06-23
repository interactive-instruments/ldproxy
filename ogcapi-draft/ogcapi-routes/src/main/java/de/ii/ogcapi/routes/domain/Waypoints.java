/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableWaypoints.Builder.class)
public interface Waypoints {
  WaypointsValue getValue();

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Waypoints> FUNNEL =
      (from, into) -> {
        WaypointsValue.FUNNEL.funnel(from.getValue(), into);
      };
}
