/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import de.ii.ldproxy.ogcapi.domain.UnprocessableEntity;
import de.ii.ldproxy.ogcapi.features.core.domain.Geometry;
import de.ii.ldproxy.ogcapi.routes.domain.FeatureTransformationContextRoutes;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRoute;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRouteEnd;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRouteOverview;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRouteSegment;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRouteStart;
import de.ii.ldproxy.ogcapi.routes.domain.Route;
import de.ii.ldproxy.ogcapi.routes.domain.RouteEnd;
import de.ii.ldproxy.ogcapi.routes.domain.RouteOverview;
import de.ii.ldproxy.ogcapi.routes.domain.RouteSegment;
import de.ii.ldproxy.ogcapi.routes.domain.RouteStart;
import de.ii.xtraplatform.features.domain.FeatureObjectEncoder;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static java.lang.Math.abs;
import static java.lang.Math.round;

// TODO generalize options and make them configurable to avoid that the encoder is specific to one use case and source dataset

public class FeatureEncoderRoutes extends FeatureObjectEncoder<PropertyRoutes, FeatureRoutes> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEncoderRoutes.class);

  private final FeatureTransformationContextRoutes transformationContext;
  private final ImmutableRoute.Builder builder;
  private RouteStart start;
  private Geometry.Point lastPoint;
  private Double lastAngle;
  private List<Geometry.Coordinate> overviewGeometry;
  private List<RouteSegment> segments;
  private Double aggCost;
  private Double aggDuration;
  private Double aggLength;
  private Double aggAscent;
  private Double aggDescent;
  private ImmutableRouteSegment.Builder segmentBuilder;
  private boolean firstSegment;
  private boolean is3d;
  private boolean isReverse;
  private String speedLimitUnit;

  public FeatureEncoderRoutes(FeatureTransformationContextRoutes transformationContext) {
    this.transformationContext = transformationContext;
    this.builder = new ImmutableRoute.Builder();
  }

  @Override
  public FeatureRoutes createFeature() {
    return ModifiableFeatureRoutes.create();
  }

  @Override
  public PropertyRoutes createProperty() {
    return ModifiablePropertyRoutes.create();
  }

  @Override
  public void onStart(ModifiableContext context) {
    transformationContext.getName().ifPresent(builder::name);
    context.metadata().getNumberMatched().ifPresent(num -> LOGGER.debug("numberMatched {}", num));
    context.metadata().getNumberReturned().ifPresent(num -> LOGGER.debug("numberReturned {}", num));
    lastPoint = null;
    lastAngle = 0.0;
    firstSegment = true;
    is3d = true;
    start = null;
    segments = new ArrayList<>();
    overviewGeometry = new ArrayList<>();
    aggCost = 0.0;
    aggDuration = 0.0;
    aggLength = 0.0;
    aggAscent = 0.0;
    aggDescent = 0.0;
    speedLimitUnit = transformationContext.getSpeedLimitUnit();
  }

  @Override
  public void onFeature(FeatureRoutes feature) {
    String id = feature.getIdValue();

    List<Geometry.Coordinate> coordinates = feature.parseGeometry()
        .orElseThrow(() -> new IllegalStateException(String.format("Route segment with sequence number '%s' is without a geometry. Cannot construct route.", id)))
        .getCoordinates();

    AtomicReference<ImmutableMap.Builder<String, Object>> propertyBuilder = new AtomicReference<>(ImmutableMap.builder());
    int coordCount = coordinates.size();

    // swap curve, if necessary
    int node = feature.findPropertyByPath("node")
        .map(PropertyRoutes::getFirstValue)
        .map(Integer::parseInt)
        .orElse(Integer.MIN_VALUE);
    int target = feature.findPropertyByPath(ImmutableList.of("data", "target"))
        .map(PropertyRoutes::getFirstValue)
        .map(Integer::parseInt)
        .orElse(Integer.MIN_VALUE);
    if (node!=Integer.MIN_VALUE && target!= Integer.MIN_VALUE && node==target) {
      coordinates = Lists.reverse(coordinates);
      isReverse = true;
    } else {
      isReverse = false;
    }

    if (firstSegment) {
      firstSegment = false;
      Geometry.Coordinate firstCoord = coordinates.get(0);
      start = ImmutableRouteStart.builder()
          .id(2)
          .geometry(Geometry.Point.of(firstCoord))
          .putProperties("featureType", RouteStart.FEATURE_TYPE)
          .build();
      overviewGeometry.add(firstCoord);
      is3d = firstCoord.size()==3;
    } else {
      double angle = computeAngle(coordinates.get(0), coordinates.get(1));
      double changeDeg = Math.toDegrees(deltaAngle(lastAngle, angle));
      segments.add(segmentBuilder
                       .putProperties("instructions", changeDeg > 45 ? "left" : changeDeg < -45 ? "right" : "continue")
                       .putProperties("directionChange_deg", round(changeDeg))
                       .build());
    }

    overviewGeometry.addAll(coordinates.subList(1, coordCount));

    if (is3d) {
      AtomicReference<Double> segAscent = new AtomicReference<>(0.0);
      AtomicReference<Double> segDescent = new AtomicReference<>(0.0);
      List<Geometry.Coordinate> finalCoordinates = coordinates;
      IntStream.range(1, coordCount)
          .mapToDouble(i -> finalCoordinates.get(i).z - finalCoordinates.get(i - 1).z)
          .forEach(deltaZ -> {
            if (deltaZ > 0)
              segAscent.updateAndGet(v -> v + deltaZ);
            else
              segDescent.updateAndGet(v -> v - deltaZ);
          });
      aggAscent += segAscent.get();
      propertyBuilder.get().put("ascent_m", segAscent.get());
      aggDescent += segDescent.get();
      propertyBuilder.get().put("descent_m", segDescent.get());
    }

    lastPoint = Geometry.Point.of(coordinates.get(coordCount-1));
    lastAngle = computeAngle(coordinates.get(coordCount - 2), coordinates.get(coordCount - 1));;

    feature.findPropertyByPath("cost")
        .map(PropertyRoutes::getFirstValue)
        .map(Double::parseDouble)
        .ifPresent(cost -> {
          aggCost += cost;
        });
    feature.getProperties()
        .stream()
        .filter(p -> p.getPropertyPath().get(0).equals("data"))
        .filter(p -> p.getSchema().filter(SchemaBase::isSpatial).isEmpty())
        .forEach(p -> {
          propertyBuilder.set(processProperty(id, p, propertyBuilder.get()));
        });
    segmentBuilder = ImmutableRouteSegment.builder()
        .geometry(lastPoint)
        .id(Integer.parseInt(id)+3)
        .putProperties("featureType", RouteSegment.FEATURE_TYPE)
        .putAllProperties(propertyBuilder.get().build());
  }

  private ImmutableMap.Builder<String, Object> processProperty(String id, PropertyRoutes p, ImmutableMap.Builder<String, Object> propertyBuilder) {
    if (p.getSchema().filter(SchemaBase::isSpatial).isPresent())
      return propertyBuilder;

    SchemaBase.Type type = p.getSchema().map(FeatureSchema::getType).orElse(SchemaBase.Type.UNKNOWN);
    if (p.getType().equals(PropertyBase.Type.OBJECT)) {
      for (PropertyRoutes p2 : p.getNestedProperties()) {
        propertyBuilder = processProperty(id, p2, propertyBuilder);
      }
    } else if (!p.getType().equals(PropertyBase.Type.VALUE)) {
      LOGGER.debug("Property '{}' of segment '{}' is not a value and is ignored.", p.getName(), id);
    } else if (type.equals(SchemaBase.Type.BOOLEAN)) {
      propertyBuilder.put(p.getName(), Boolean.parseBoolean(p.getFirstValue()));
    } else if (type.equals(SchemaBase.Type.FLOAT)) {
      String name = p.getName();
      double value = Double.parseDouble(p.getFirstValue());
      if (name.equals("length_m")) {
        propertyBuilder.put(name, round(value));
        aggLength += value;
      } else if (name.equals("maxHeight_m") || name.equals("maxWeight_t")) {
        propertyBuilder.put(name, String.format(Locale.US,"%.1f",value));
      } else if (name.equals("duration_forward_s") && !isReverse) {
        propertyBuilder.put("duration_s", round(value));
        aggDuration += value;
      } else if (name.equals("duration_backward_s") && isReverse) {
        value = abs(value);
        propertyBuilder.put("duration_s", value);
        aggDuration += value;
      } else if (name.equals("maxspeed_forward") && !isReverse) {
        propertyBuilder.put("maxSpeed", round(value));
        propertyBuilder.put("maxSpeedUnit", speedLimitUnit);
      } else if (name.equals("maxspeed_backward") && isReverse) {
        propertyBuilder.put("maxSpeed", round(value));
        propertyBuilder.put("maxSpeedUnit", speedLimitUnit);
      }
    } else if (type.equals(SchemaBase.Type.INTEGER)) {
      String name = p.getName();
      int value = Integer.parseInt(p.getFirstValue());
      if (!name.equals("source") && !name.equals("target"))
        propertyBuilder.put(name, value);
    } else if (type.equals(SchemaBase.Type.STRING) || type.equals(SchemaBase.Type.DATE) || type.equals(SchemaBase.Type.DATETIME)) {
      if (!p.getFirstValue().isEmpty()) {
        propertyBuilder.put(p.getName(), p.getFirstValue());
      }
    } else
      LOGGER.debug("Property '{}' of segment '{}' property is of unsupported type '{}' and is ignored.", p.getName(), id, type);

    return propertyBuilder;
  }

  @Override
  public void onEnd(ModifiableContext context) {
    if (Objects.nonNull(start) && Objects.nonNull(lastPoint)) {
      builder.bbox(computeBbox());
      long processingDuration = (System.nanoTime() - transformationContext.getStartTimeNano()) / 1000000;
      ImmutableMap.Builder<String, Object> propertyBuilder = ImmutableMap.builder();
      if (aggLength>0.0)
        propertyBuilder.put("length_m", round(aggLength));
      if (aggDuration>0.0)
        propertyBuilder.put("duration_s", round(aggDuration));
      if (aggAscent>0.0)
        propertyBuilder.put("ascent_m", round(aggAscent));
      if (aggDescent>0.0)
        propertyBuilder.put("descent_m", round(aggDescent));
      propertyBuilder.put("processingTime", Instant.now()
          .truncatedTo(ChronoUnit.SECONDS)
          .toString());
      propertyBuilder.put("processingDuration_ms", round(processingDuration));
      builder.addFeatures(ImmutableRouteOverview.builder()
                              .id(1)
                              .geometry(Geometry.LineString.of(overviewGeometry))
                              .putProperties("featureType", RouteOverview.FEATURE_TYPE)
                              .putAllProperties(propertyBuilder.build())
                              .build(),
                          start,
                          ImmutableRouteEnd.builder()
                              .id(3)
                              .geometry(lastPoint)
                              .putProperties("featureType", RouteEnd.FEATURE_TYPE)
                              .build());
      segments.add(segmentBuilder.build());
      builder.addAllFeatures(segments);
      builder.status(Route.STATUS.successful);
    } else {
      throw new UnprocessableEntity("No route was found between the start and end location.");
    }
    byte[] result = transformationContext.getFormat()
        .getRouteAsByteArray(builder.build(), transformationContext.getApiData(), transformationContext.getOgcApiRequest());
    push(result);
  }

  private double computeAngle(Geometry.Coordinate p1, Geometry.Coordinate p2) {
    return Math.atan2(p2.y - p1.y, p2.x - p1.x);
  }

  private double deltaAngle(double angle1, double angle2) {
    double val = angle2 - angle1;
    if (val > Math.PI) {
      val -= 2. * Math.PI;
    }
    if (val < -Math.PI) {
      val += 2. * Math.PI;
    }
    return val;
  }

  private List<Double> computeBbox() {
    if (is3d) {
      Geometry.Coordinate swCorner = overviewGeometry.stream()
          .reduce((coord, coord2) -> Geometry.Coordinate.of(Math.min(coord.x, coord2.x), Math.min(coord.y, coord2.y), Math.min(coord.z, coord2.z)))
          .orElseThrow();
      Geometry.Coordinate neCorner = overviewGeometry.stream()
          .reduce((coord, coord2) -> Geometry.Coordinate.of(Math.max(coord.x, coord2.x), Math.max(coord.y, coord2.y), Math.max(coord.z, coord2.z)))
          .orElseThrow();
      return ImmutableList.of(swCorner.x, swCorner.y, swCorner.z, neCorner.x, neCorner.y, neCorner.z);
    } else {
      Geometry.Coordinate swCorner = overviewGeometry.stream()
          .reduce((coord, coord2) -> Geometry.Coordinate.of(Math.min(coord.x, coord2.x), Math.min(coord.y, coord2.y)))
          .orElseThrow();
      Geometry.Coordinate neCorner = overviewGeometry.stream()
          .reduce((coord, coord2) -> Geometry.Coordinate.of(Math.max(coord.x, coord2.x), Math.max(coord.y, coord2.y)))
          .orElseThrow();
      return ImmutableList.of(swCorner.x, swCorner.y, neCorner.x, neCorner.y);
    }
  }
}
