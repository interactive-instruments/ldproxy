/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableRouteDefinitionInputs.Builder.class)
public interface RouteDefinitionInputs {
    Optional<String> getName();
    Optional<String> getPreference();
    Optional<String> getMode();
    Optional<Double> getWeight();
    Optional<Double> getHeight();
    Optional<Obstacles> getObstacles();
    List<String> getAdditionalFlags();
    WaypointsWrapper getWaypoints();
}
