/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableObstaclesWrapper.Builder.class)
public interface ObstaclesWrapper {
    Obstacles getValue();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default String getWkt() {
        return "TODO";
        /*
        List<String> polygons = getValue().getCoordinates()
            .stream()
            .map(polygon -> "("+polygon.stream().map(ring -> "("+ring.stream().map(pos -> "("+String.join(" ",pos.stream().map(val -> String.))+")")+")")+")")

        return "MULTIPOLYGON"*/
    }
}
