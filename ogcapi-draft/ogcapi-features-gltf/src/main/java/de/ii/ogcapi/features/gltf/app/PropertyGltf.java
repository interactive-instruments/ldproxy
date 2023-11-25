/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import static de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry.MULTI_POLYGON;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.html.domain.Geometry;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Modifiable
@Value.Style(set = "*")
public interface PropertyGltf extends PropertyBase<PropertyGltf, FeatureSchema> {

  Splitter PATH_SPLITTER = Splitter.on('.').omitEmptyStrings();

  Optional<String> getItemType();

  PropertyGltf itemType(String itemType);

  Optional<String> getItemProp();

  PropertyGltf itemProp(String itemProp);

  @Override
  @Value.Default
  default String getName() {
    return isFlattened()
        ? getSchema().map(FeatureSchema::getName).orElse("")
        : getSchema()
            .flatMap(FeatureSchema::getLabel)
            .orElse(getSchema().map(FeatureSchema::getName).orElse(""));
  }

  @Value.Lazy
  default boolean hasValues() {
    return isValue() || isArray() && getNestedProperties().stream().anyMatch(PropertyBase::isValue);
  }

  @Value.Lazy
  default List<PropertyGltf> getValues() {
    return isValue()
        ? ImmutableList.of(this)
        : isArray()
            ? getNestedProperties().stream()
                .filter(PropertyBase::isValue)
                .collect(Collectors.toList())
            : ImmutableList.of();
  }

  @Value.Lazy
  default String getFirstValue() {
    return hasValues() ? getValues().get(0).getValue() : null;
  }

  @Value.Lazy
  default List<PropertyGltf> getObjects() {
    return getNestedProperties().stream()
        .filter(
            property ->
                !property.isValue() && property.getSchema().filter(SchemaBase::isSpatial).isEmpty())
        .collect(Collectors.toList());
  }

  @Value.Lazy
  default boolean isInArray() {
    return getParent().isPresent() && getParent().get().isArray();
  }

  @Value.Lazy
  @Override
  default boolean isObject() {
    return PropertyBase.super.isObject() && getSchema().filter(SchemaBase::isSpatial).isEmpty();
  }

  @Value.Lazy
  default boolean isNull() {
    return Objects.isNull(getValue());
  }

  @Value.Lazy
  default Optional<PropertyGltf> getGeometry() {
    return getNestedProperties().stream()
        .filter(property -> property.getSchema().filter(SchemaBase::isSpatial).isPresent())
        .findFirst()
        .or(
            () ->
                getNestedProperties().stream()
                    .map(PropertyGltf::getGeometry)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst());
  }

  @Value.Lazy
  default String getLastPathSegment() {
    return getPropertyPath().get(getPropertyPath().size() - 1);
  }

  default Optional<PropertyGltf> findPropertyByPath(List<String> path) {
    return getNestedProperties().stream()
        .filter(property -> Objects.equals(property.getPropertyPath(), path))
        .findFirst()
        .or(
            () ->
                getNestedProperties().stream()
                    .filter(
                        property -> property.getSchema().filter(SchemaBase::isSpatial).isEmpty())
                    .map(property -> property.findPropertyByPath(path))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst());
  }

  default Optional<Geometry.MultiPolygon> getMultiPolygon() {
    if (getGeometryType().isEmpty()) {
      return Optional.empty();
    } else if (getGeometryType().orElse(SimpleFeatureGeometry.ANY) != MULTI_POLYGON) {
      throw new IllegalStateException(
          "Unexpected geometry type for property '"
              + getName()
              + "', MultiPolygon required: "
              + getGeometryType());
    }
    return Optional.of(
        Geometry.MultiPolygon.of(
            getNestedProperties().get(0).getNestedProperties().stream()
                .map(
                    polygon ->
                        Geometry.Polygon.of(
                            polygon.getNestedProperties().stream()
                                .map(
                                    ring ->
                                        Geometry.LineString.of(
                                            getCoordinates(ring.getNestedProperties())))
                                .collect(Collectors.toUnmodifiableList())))
                .collect(Collectors.toUnmodifiableList())));
  }

  private static Geometry.Coordinate getCoordinate(List<PropertyGltf> coordList) {
    return Geometry.Coordinate.of(
        coordList.stream()
            .map(PropertyBase::getValue)
            .filter(Objects::nonNull)
            .map(Double::parseDouble)
            .collect(Collectors.toUnmodifiableList()));
  }

  private static List<Geometry.Coordinate> getCoordinates(List<PropertyGltf> coordsList) {
    return coordsList.stream()
        .map(coord -> Geometry.Coordinate.of(getCoordinate(coord.getNestedProperties())))
        .collect(Collectors.toUnmodifiableList());
  }
}
