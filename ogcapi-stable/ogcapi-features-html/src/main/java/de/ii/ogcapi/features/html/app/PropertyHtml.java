/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.app;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.html.domain.Geometry;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Modifiable
@Value.Style(set = "*")
public interface PropertyHtml extends PropertyBase<PropertyHtml, FeatureSchema> {

  Optional<String> getItemType();

  PropertyHtml itemType(String itemType);

  Optional<String> getItemProp();

  PropertyHtml itemProp(String itemProp);

  @Value.Lazy
  default String getSchemaOrgItemType() {
    return getItemType()
        .filter(itemType -> itemType.startsWith("http://schema.org/"))
        .map(itemType -> itemType.substring(18))
        .orElse("");
  }

  @Value.Default
  default String getName() {
    return isFlattened()
        ? getSchema().map(FeatureSchema::getName).orElse("")
        : getSchema()
            .flatMap(FeatureSchema::getLabel)
            .orElse(getSchema().map(FeatureSchema::getName).orElse(""));
  }

  @Value.Default
  default String getDescription() {
    return getSchema().flatMap(FeatureSchema::getDescription).orElse("");
  }

  @Value.Lazy
  default String getOriginalName() {
    return PropertyBase.super.getName();
  }

  @Value.Lazy
  default boolean hasValues() {
    return isValue()
        || (isArray() && getNestedProperties().stream().anyMatch(PropertyBase::isValue));
  }

  @Value.Lazy
  default List<PropertyHtml> getValues() {
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
  default List<PropertyHtml> getObjects() {
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
  default boolean isUrl() {
    return Objects.nonNull(getValue())
        && (getValue().startsWith("http://") || getValue().startsWith("https://"));
  }

  @Value.Lazy
  default boolean isImageUrl() {
    return Objects.nonNull(getValue())
        && isUrl()
        && (getValue().toLowerCase().endsWith(".png")
            || getValue().toLowerCase().endsWith(".jpg")
            || getValue().toLowerCase().endsWith(".jpeg")
            || getValue().toLowerCase().endsWith(".gif"));
  }

  @Value.Lazy
  default boolean isLevel1() {
    return isFlattened() || getLevel() == 1;
  }

  @Value.Lazy
  default boolean isLevel2() {
    return !isFlattened() && getLevel() == 2;
  }

  @Value.Lazy
  default boolean isLevel3() {
    return !isFlattened() && getLevel() == 3;
  }

  @Value.Lazy
  default boolean isLevel4() {
    return !isFlattened() && getLevel() == 4;
  }

  @Value.Lazy
  default boolean isLevel5() {
    return !isFlattened() && getLevel() >= 5;
  }

  @Value.Lazy
  default boolean hasGeometry() {
    return getNestedProperties().stream()
            .anyMatch(property -> property.getSchema().filter(SchemaBase::isSpatial).isPresent())
        || getNestedProperties().stream().anyMatch(PropertyHtml::hasGeometry);
  }

  @Value.Lazy
  default Optional<PropertyHtml> getGeometry() {
    return getNestedProperties().stream()
        .filter(property -> property.getSchema().filter(SchemaBase::isSpatial).isPresent())
        .findFirst()
        .or(
            () ->
                getNestedProperties().stream()
                    .map(PropertyHtml::getGeometry)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst());
  }

  Splitter PATH_SPLITTER = Splitter.on('.').omitEmptyStrings();

  enum PATH_COMPARISON {
    INCOMPATIBLE,
    SUB_PATH,
    EQUAL
  }

  // 0: incompatible
  // 1: sub-path
  // 2: equal
  static PATH_COMPARISON pathCompatible(List<String> propertyPath, List<String> referencePath) {
    if (propertyPath.size() > referencePath.size()) return PATH_COMPARISON.INCOMPATIBLE;
    for (int i = 0; i < propertyPath.size(); i++) {
      if (!Objects.equals(propertyPath.get(i), referencePath.get(i)))
        return PATH_COMPARISON.INCOMPATIBLE;
    }
    return propertyPath.size() == referencePath.size()
        ? PATH_COMPARISON.EQUAL
        : PATH_COMPARISON.SUB_PATH;
  }

  default Optional<PropertyHtml> findPropertyByPath(String pathString) {
    return findPropertyByPath(PATH_SPLITTER.splitToList(pathString));
  }

  default Optional<PropertyHtml> findPropertyByPath(List<String> path) {
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

  default List<PropertyHtml> findPropertiesByPath(String pathString) {
    return findPropertiesByPath(PropertyHtml.PATH_SPLITTER.splitToList(pathString));
  }

  default List<PropertyHtml> findPropertiesByPath(List<String> path) {
    List<PropertyHtml> properties =
        getNestedProperties().stream()
            .map(
                property -> {
                  switch (PropertyHtml.pathCompatible(property.getPropertyPath(), path)) {
                    case SUB_PATH:
                      return property.findPropertiesByPath(path);
                    case EQUAL:
                      return ImmutableList.of(property);
                  }
                  return ImmutableList.<PropertyHtml>of();
                })
            .flatMap(Collection::stream)
            .collect(Collectors.toUnmodifiableList());
    if (properties.isEmpty())
      properties =
          getNestedProperties().stream()
              .filter(property -> property.getSchema().filter(SchemaBase::isSpatial).isEmpty())
              .map(property -> property.findPropertyByPath(path))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toUnmodifiableList());
    return properties;
  }

  @Value.Lazy
  default Geometry<?> parseGeometry() {
    if (getSchema().filter(SchemaBase::isSpatial).isEmpty() || getGeometryType().isEmpty())
      throw new IllegalStateException(
          String.format(
              "Feature property with path '%s' is not a spatial geometry: '%s'",
              getPropertyPath(), getSchema()));

    List<PropertyHtml> coordinatesProperties = getNestedProperties().get(0).getNestedProperties();
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

  private Geometry.Coordinate getCoordinate(List<PropertyHtml> coordList) {
    return Geometry.Coordinate.of(
        coordList.stream()
            .map(PropertyBase::getValue)
            .map(Double::parseDouble)
            .collect(Collectors.toUnmodifiableList()));
  }

  private List<Geometry.Coordinate> getCoordinates(List<PropertyHtml> coordsList) {
    return coordsList.stream()
        .map(coord -> Geometry.Coordinate.of(getCoordinate(coord.getNestedProperties())))
        .collect(Collectors.toUnmodifiableList());
  }
}
