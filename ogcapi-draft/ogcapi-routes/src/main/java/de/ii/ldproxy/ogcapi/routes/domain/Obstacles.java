/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import de.ii.ldproxy.ogcapi.features.html.domain.Geometry;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableObstacles.Builder.class)
public abstract class Obstacles {
    public final String getType() { return "MultiPolygon"; }
    public abstract List<List<List<List<Float>>>> getCoordinates();
    @Value.Default
    public String getCoordRefSys() { return OgcCrs.CRS84_URI; }

    @Value.Check
    void check() {
        Preconditions.checkState(getType().equals("MultiPolygon"), "Waypoints is not a MultiPolygon geometry. Found: {}.", getType());
        Preconditions.checkState(getCoordinates().size()>=1, "At least one polygon is required. Found: {}.", getCoordinates().size());
        getCoordinates().stream()
            .flatMap(Collection::stream)
            .flatMap(Collection::stream)
            .forEach(position -> {
            Preconditions.checkState(position.size()>=2, "At least two coordinates are required per position. Found: {}.", position.size());
            Preconditions.checkState(position.size()<=3, "At most three coordinates are required per position. Found: {}.", position.size());
        });
        List<Integer> dimensions = getCoordinates().stream()
            .flatMap(Collection::stream)
            .flatMap(Collection::stream)
            .map(List::size)
            .distinct()
            .sorted()
            .collect(Collectors.toUnmodifiableList());
        Preconditions.checkState(dimensions.size()==1, "All coordinates must have the same dimension. Found: {}.", dimensions);
        Preconditions.checkState(dimensions.stream().min(Comparator.naturalOrder()).orElse(2)>=2, "At least two coordinates are required per position. Found: {}.", dimensions.stream().min(Comparator.naturalOrder()).get());
        Preconditions.checkState(dimensions.stream().max(Comparator.naturalOrder()).orElse(3)<=3, "At most three coordinates are required per position. Found: {}.", dimensions.stream().max(Comparator.naturalOrder()).get());
    }
}