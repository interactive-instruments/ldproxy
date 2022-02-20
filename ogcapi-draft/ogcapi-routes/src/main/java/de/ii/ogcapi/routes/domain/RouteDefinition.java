/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Funnel;
import de.ii.ldproxy.ogcapi.foundation.domain.Link;
import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.cql.domain.ImmutableMultiPolygon;
import de.ii.xtraplatform.cql.domain.ImmutablePoint;
import de.ii.xtraplatform.cql.domain.ImmutablePolygon;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableRouteDefinition.Builder.class)
public interface RouteDefinition {
  RouteDefinitionInputs getInputs();

  List<Link> getLinks();

  @JsonIgnore
  @Value.Derived
  default List<List<Float>> getPoints() {
    return getInputs().getWaypoints().getValue().getCoordinates();
  }

  @JsonIgnore
  @Value.Derived
  default EpsgCrs getWaypointsCrs() {
    try {
      return EpsgCrs.fromString(getInputs().getWaypoints().getValue().getCoordRefSys());
    } catch (Throwable e) {
      throw new IllegalArgumentException(String.format("The value of 'coordRefSys' in the route definition is invalid: %s", e.getMessage()), e);
    }
  }

  @JsonIgnore
  @Value.Derived
  default Geometry.Point getStart() {
    return processWaypoint(getPoints().get(0), getWaypointsCrs());
  }

  @JsonIgnore
  @Value.Derived
  default Geometry.Point getEnd() {
    List<List<Float>> waypoints = getPoints();
    return processWaypoint(waypoints.get(waypoints.size()-1), getWaypointsCrs());
  }

  @JsonIgnore
  @Value.Derived
  default List<Geometry.Point> getWaypoints() {
    List<List<Float>> waypoints = getPoints();
    EpsgCrs crs = getWaypointsCrs();

    if (waypoints.size() <= 2) {
      return ImmutableList.of();
    }

    return IntStream.range(1, waypoints.size()-1)
        .mapToObj(i -> processWaypoint(waypoints.get(i), crs))
        .collect(Collectors.toUnmodifiableList());
  }

  @JsonIgnore
  @Value.Derived
  default Optional<Geometry.MultiPolygon> getObstacles() {
    Optional<Obstacles> obstacles = getInputs().getObstacles();
    if (obstacles.isEmpty())
      return Optional.empty();

    EpsgCrs crs;
    try {
      crs = EpsgCrs.fromString(obstacles.get().getValue().getCoordRefSys());
    } catch (Throwable e) {
      throw new IllegalArgumentException(String.format("The value of 'coordRefSys' in the route definition is invalid: %s", e.getMessage()), e);
    }

    ImmutableMultiPolygon.Builder builder = new ImmutableMultiPolygon.Builder()
        .crs(crs);

    obstacles.get()
        .getValue()
        .getCoordinates()
        .forEach(polygon -> builder.addCoordinates(
            new ImmutablePolygon.Builder()
                .crs(crs)
                .addAllCoordinates(polygon.stream()
                                       .map(ring -> ring.stream()
                                           .map(RouteDefinition::processPosition)
                                           .collect(Collectors.toUnmodifiableList()))
                                       .collect(Collectors.toUnmodifiableList()))
                .build()));

    return Optional.of(builder.build());
  }

  static Geometry.Point processWaypoint(List<Float> coord, EpsgCrs crs) {
    if (coord.size()==2)
      return Geometry.Point.of(coord.get(0), coord.get(1), crs);
    else
      return new ImmutablePoint.Builder()
          .addCoordinates(new Geometry.Coordinate(coord.get(0).doubleValue(), coord.get(1).doubleValue(), coord.get(2).doubleValue()))
          .crs(crs)
          .build();
  }

  static Geometry.Coordinate processPosition(List<Float> coord) {
    if (coord.size()==2)
      return Geometry.Coordinate.of(coord.get(0), coord.get(1));
    else
      return new Geometry.Coordinate(coord.get(0).doubleValue(), coord.get(1).doubleValue(), coord.get(2).doubleValue());
  }

  @SuppressWarnings("UnstableApiUsage")
  Funnel<RouteDefinition> FUNNEL = (from, into) -> {
    RouteDefinitionInputs.FUNNEL.funnel(from.getInputs(), into);
  };
}
