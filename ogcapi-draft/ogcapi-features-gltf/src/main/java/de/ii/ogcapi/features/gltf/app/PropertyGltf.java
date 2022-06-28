/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.html.domain.Geometry;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerFlatten;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Modifiable
@Value.Style(set = "*")
public interface PropertyGltf extends PropertyBase<PropertyGltf, FeatureSchema> {

  Optional<String> getItemType();

  PropertyGltf itemType(String itemType);

  Optional<String> getItemProp();

  PropertyGltf itemProp(String itemProp);

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
    return isValue()
        || (isArray() && getNestedProperties().stream().anyMatch(PropertyBase::isValue));
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
  default boolean isFirstObject() {
    return getParent().isPresent()
        && getParent().get().isArray()
        && Objects.equals(getParent().get().getNestedProperties().get(0), this);
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
  default boolean hasGeometry() {
    return getNestedProperties().stream()
            .anyMatch(property -> property.getSchema().filter(SchemaBase::isSpatial).isPresent())
        || getNestedProperties().stream().anyMatch(PropertyGltf::hasGeometry);
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
  default boolean isFlattened() {
    return getTransformed().containsKey(FeaturePropertyTransformerFlatten.TYPE);
  }

  Splitter PATH_SPLITTER = Splitter.on('.').omitEmptyStrings();

  default Optional<PropertyGltf> findPropertyByPath(String pathString) {
    return findPropertyByPath(PATH_SPLITTER.splitToList(pathString));
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

  @Value.Lazy
  default Geometry<?> parseGeometry() {
    if (getSchema().filter(SchemaBase::isSpatial).isEmpty() || getGeometryType().isEmpty()) {
      throw new IllegalStateException(
          String.format(
              "Feature property with path '%s' is not a geometry: '%s'",
              getPropertyPath(), getSchema()));
    }

    List<PropertyGltf> coordinatesProperties = getNestedProperties().get(0).getNestedProperties();
    switch (getGeometryType().get()) {
      case POINT:
        return Geometry.Point.of(getCoordinate(coordinatesProperties));
      case MULTI_POINT:
        return Geometry.MultiPoint.of(
            coordinatesProperties.stream()
                .map(coord -> Geometry.Point.of(getCoordinate(coord.getNestedProperties())))
                .collect(Collectors.toUnmodifiableList()));
      case LINE_STRING:
        return Geometry.LineString.of(getCoordinates(coordinatesProperties));
      case MULTI_LINE_STRING:
        return Geometry.MultiLineString.of(
            coordinatesProperties.stream()
                .map(line -> Geometry.LineString.of(getCoordinates(line.getNestedProperties())))
                .collect(Collectors.toUnmodifiableList()));
      case POLYGON:
        return Geometry.Polygon.of(
            coordinatesProperties.stream()
                .map(ring -> Geometry.LineString.of(getCoordinates(ring.getNestedProperties())))
                .collect(Collectors.toUnmodifiableList()));
      case MULTI_POLYGON:
        return Geometry.MultiPolygon.of(
            coordinatesProperties.stream()
                .map(
                    polygon ->
                        Geometry.Polygon.of(
                            polygon.getNestedProperties().stream()
                                .map(
                                    ring ->
                                        Geometry.LineString.of(
                                            getCoordinates(ring.getNestedProperties())))
                                .collect(Collectors.toUnmodifiableList())))
                .collect(Collectors.toUnmodifiableList()));
      default:
        throw new IllegalStateException("Unsupported geometry type: " + getGeometryType());
    }
  }

  private Geometry.Coordinate getCoordinate(List<PropertyGltf> coordList) {
    return Geometry.Coordinate.of(
        coordList.stream()
            .map(PropertyBase::getValue)
            .filter(Objects::nonNull)
            .map(Double::parseDouble)
            .collect(Collectors.toUnmodifiableList()));
  }

  private List<Geometry.Coordinate> getCoordinates(List<PropertyGltf> coordsList) {
    return coordsList.stream()
        .map(coord -> Geometry.Coordinate.of(getCoordinate(coord.getNestedProperties())))
        .collect(Collectors.toUnmodifiableList());
  }
}
