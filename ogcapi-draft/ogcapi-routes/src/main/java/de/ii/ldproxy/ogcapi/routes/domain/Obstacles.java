/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableObstacles.Builder.class)
public interface Obstacles {
    ObstaclesValue getValue();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default String getWkt() {
        return "MULTIPOLYGON("+getValue().getCoordinates().stream().map(this::getPolygon).collect(Collectors.joining(","))+")";
    }

    default String getPolygon(List<List<List<Float>>> polygon) {
        return "("+polygon.stream().map(this::getRing).collect(Collectors.joining(","))+")";
    }

    default String getRing(List<List<Float>> ring) {
        return "("+ring.stream().map(this::getPos).collect(Collectors.joining(","))+")";
    }

    default String getPos(List<Float> pos) {
        return pos.stream().map(String::valueOf).collect(Collectors.joining(" "));
    }

    @SuppressWarnings("UnstableApiUsage")
    Funnel<Obstacles> FUNNEL = (from, into) -> {
        ObstaclesValue.FUNNEL.funnel(from.getValue(), into);
    };
}
