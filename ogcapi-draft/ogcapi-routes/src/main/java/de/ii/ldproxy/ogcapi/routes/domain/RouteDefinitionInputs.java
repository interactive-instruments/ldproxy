/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableRouteDefinitionInputs.Builder.class)
public interface RouteDefinitionInputs {
    Waypoints getWaypoints();
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<String> getName();
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<String> getPreference();
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<String> getMode();
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<Double> getWeight();
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<Double> getHeight();
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<Obstacles> getObstacles();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<String> getAdditionalFlags();

    @SuppressWarnings("UnstableApiUsage")
    Funnel<RouteDefinitionInputs> FUNNEL = (from, into) -> {
        Waypoints.FUNNEL.funnel(from.getWaypoints(), into);
        from.getName().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getPreference().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getMode().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
        from.getHeight().ifPresent(into::putDouble);
        from.getWeight().ifPresent(into::putDouble);
        from.getObstacles().ifPresent(val -> Obstacles.FUNNEL.funnel(val, into));
        from.getAdditionalFlags().forEach(val -> into.putString(val, StandardCharsets.UTF_8));
    };
}
