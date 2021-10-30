/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.features.core.domain.Geometry;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerFlatten;
import org.immutables.value.Value;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Modifiable
@Value.Style(set = "*")
public interface PropertyRoutes extends PropertyBase<PropertyRoutes, FeatureSchema> {

  @Value.Default
  default String getName() {
    return isFlattened()
    ? getSchema().map(FeatureSchema::getName).orElse("")
    : getSchema().flatMap(FeatureSchema::getLabel).orElse(getSchema().map(FeatureSchema::getName).orElse(""));
  }

  @Value.Lazy
  default boolean hasValues() {
    return isValue() || (isArray() && getNestedProperties().stream().anyMatch(PropertyBase::isValue));
  }

  @Value.Lazy
  default List<PropertyRoutes> getValues() {
    return isValue()
        ? ImmutableList.of(this)
        : isArray()
            ? getNestedProperties().stream().filter(PropertyBase::isValue).collect(Collectors.toList())
            : ImmutableList.of();
  }

  @Value.Lazy
  default String getFirstValue() {
    return hasValues() ? getValues().get(0).getValue() : null;
  }

  @Value.Lazy
  default Optional<PropertyRoutes> getGeometry() {
    return getNestedProperties().stream()
        .filter(property -> property.getSchema().filter(SchemaBase::isSpatial).isPresent())
        .findFirst()
        .or(() -> getNestedProperties().stream()
            .map(PropertyRoutes::getGeometry)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst());
  }

  @Value.Lazy
  default boolean isFlattened() {
    return getTransformed().containsKey(FeaturePropertyTransformerFlatten.TYPE);
  }

  Splitter PATH_SPLITTER = Splitter.on('.').omitEmptyStrings();

  default Optional<PropertyRoutes> findPropertyByPath(String pathString) {
    return findPropertyByPath(PATH_SPLITTER.splitToList(pathString));
  }

  default Optional<PropertyRoutes> findPropertyByPath(List<String> path) {
    return getNestedProperties().stream()
        .filter(property -> Objects.equals(property.getPropertyPath(), path))
        .findFirst()
        .or(() -> getNestedProperties().stream()
            .filter(property -> property.getSchema().filter(SchemaBase::isSpatial).isEmpty())
            .map(property -> property.findPropertyByPath(path))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst());
  }

  @Value.Lazy
  default Geometry.LineString parseGeometry() {
    if (getSchema().filter(SchemaBase::isSpatial).isEmpty() || getGeometryType().isEmpty())
      throw new IllegalStateException(String.format("Feature property with path '%s' is not a geometry: '%s'", getPropertyPath(), getSchema()));

    List<PropertyRoutes> coordinatesProperties = getNestedProperties().get(0)
                                                                    .getNestedProperties();
    switch (getGeometryType().get()) {
      case LINE_STRING:
        return Geometry.LineString.of(getCoordinates(coordinatesProperties));
      default:
        throw new IllegalStateException("Unsupported geometry type: " + getGeometryType());
    }
  }

  private Geometry.Coordinate getCoordinate(List<PropertyRoutes> coordList) {
    return Geometry.Coordinate.of(coordList.stream()
                                           .map(PropertyBase::getValue)
                                           .map(Double::parseDouble)
                                           .collect(Collectors.toUnmodifiableList()));
  }

  private List<Geometry.Coordinate> getCoordinates(List<PropertyRoutes> coordsList) {
    return coordsList.stream()
                     .map(coord -> getCoordinate(coord.getNestedProperties()))
                     .collect(Collectors.toUnmodifiableList());
  }
}
