/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.cql.domain.ImmutablePoint;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.immutables.value.Value;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableRouteDefinitionWrapper.Builder.class)
public interface RouteDefinitionWrapper {
  RouteDefinition getInputs();

  @Value.Derived
  default List<List<Float>> getPoints() {
    return getInputs().getWaypoints().getValue().getCoordinates();
  }

  @Value.Derived
  default EpsgCrs getCrs() {
    try {
      return EpsgCrs.fromString(getInputs().getWaypoints().getValue().getCoordRefSys());
    } catch (Throwable e) {
      throw new IllegalArgumentException(String.format("The value of 'coordRefSys' in the route definition is invalid: %s", e.getMessage()), e);
    }
  }

  @Value.Derived
  default Geometry.Point getStart() {
    return processWaypoint(getPoints().get(0), getCrs());
  }

  @Value.Derived
  default Geometry.Point getEnd() {
    List<List<Float>> waypoints = getPoints();
    return processWaypoint(waypoints.get(waypoints.size()-1), getCrs());
  }

  @Value.Derived
  default List<Geometry.Point> getWaypoints() {
    List<List<Float>> waypoints = getPoints();
    EpsgCrs crs = getCrs();
    return IntStream.range(1, waypoints.size())
        .mapToObj(i -> processWaypoint(waypoints.get(i), crs))
        .collect(Collectors.toUnmodifiableList());
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
}
