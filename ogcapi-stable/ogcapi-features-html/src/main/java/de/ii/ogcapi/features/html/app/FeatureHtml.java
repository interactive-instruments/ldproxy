/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.html.domain.Geometry;
import de.ii.xtraplatform.features.domain.FeatureBase;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Modifiable
@Value.Style(set = "*")
public interface FeatureHtml extends FeatureBase<PropertyHtml, FeatureSchema> {

  Optional<String> getItemType();

  FeatureHtml itemType(String itemType);

  @Value.Default
  default boolean inCollection() {
    return false;
  }

  FeatureHtml inCollection(boolean inCollection);

  @Value.Lazy
  default String getSchemaOrgItemType() {
    return getItemType()
        .filter(itemType -> itemType.startsWith("http://schema.org/"))
        .map(itemType -> itemType.substring(18))
        .orElse(null);
  }

  @Override
  @Value.Default
  default String getName() {
    return getId()
        .flatMap(id -> Optional.ofNullable(id.getFirstValue()))
        .orElse(FeatureBase.super.getName());
  }

  @Value.Lazy
  default Optional<PropertyHtml> getId() {
    return getProperties().stream()
        .filter(property -> property.getSchema().filter(SchemaBase::isId).isPresent())
        .findFirst();
  }

  @Value.Lazy
  default String getIdValue() {
    return getId().map(PropertyHtml::getFirstValue).orElse(null);
  }

  @Value.Lazy
  default boolean hasObjects() {
    return getProperties().stream()
        .anyMatch(
            property ->
                !property.isValue()
                    && property.getSchema().filter(SchemaBase::isSpatial).isEmpty());
  }

  @Value.Lazy
  default boolean hasGeometry() {
    return getProperties().stream()
            .anyMatch(property -> property.getSchema().filter(SchemaBase::isSpatial).isPresent())
        || getProperties().stream().anyMatch(PropertyHtml::hasGeometry);
  }

  @Value.Lazy
  default Optional<PropertyHtml> getGeometry() {
    return getProperties().stream()
        .filter(property -> property.getSchema().filter(SchemaBase::isSpatial).isPresent())
        .findFirst()
        .or(
            () ->
                getProperties().stream()
                    .map(property -> property.getGeometry())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst());
  }

  @Value.Lazy
  default String getGeoAsString() {
    Optional<PropertyHtml> geometry = getGeometry();
    return geometry
        .flatMap(
            geo -> {
              Optional<SimpleFeatureGeometry> geometryType =
                  geo.getSchema().flatMap(FeatureSchema::getGeometryType);
              List<PropertyHtml> coordinates = getFirstCoordinates();
              if (geometryType.isPresent() && !coordinates.isEmpty()) {

                if ((geometryType.get() == SimpleFeatureGeometry.POINT
                    || geometryType.get() == SimpleFeatureGeometry.MULTI_POINT)) {
                  PropertyHtml point = coordinates.get(0);
                  if (point.getValues().size() > 1) {
                    String latitude = point.getValues().get(0).getValue();
                    String longitude = point.getValues().get(1).getValue();

                    return Optional.of(
                        String.format(
                            "{ \"@type\": \"GeoCoordinates\", \"latitude\": \"%s\", \"longitude\": \"%s\" }",
                            latitude, longitude));
                  }
                } else if (geometryType.get() != SimpleFeatureGeometry.ANY) {
                  String geomType;
                  switch (geometryType.get()) {
                    case POLYGON:
                    case MULTI_POLYGON:
                      geomType = "polygon";
                      break;
                    default:
                      geomType = "line";
                      break;
                  }
                  String coords =
                      coordinates.stream()
                          .map(
                              coord ->
                                  coord.getValues().size() > 1
                                      ? coord.getValues().get(1).getValue()
                                          + " "
                                          + coord.getValues().get(0).getValue()
                                      : "")
                          .collect(Collectors.joining(" "));

                  return Optional.of(
                      String.format(
                          "{ \"@type\": \"GeoShape\", \"%s\": \"%s\" }", geomType, coords));
                }
              }

              return Optional.empty();
            })
        .orElse(null);
  }

  @Value.Lazy
  default Optional<Geometry<?>> parseGeometry() {
    return getGeometry().map(PropertyHtml::parseGeometry);
  }

  @Value.Lazy
  default List<PropertyHtml> getFirstCoordinates() {
    return getGeometry()
        .flatMap(
            geometry -> {
              PropertyHtml current = geometry;

              while (!current.getNestedProperties().isEmpty() && !current.hasValues()) {
                PropertyHtml next = current.getNestedProperties().get(0);

                if (next.hasValues()) {
                  return Optional.of(current.getNestedProperties());
                }

                current = next;
              }

              return Optional.empty();
            })
        .orElse(ImmutableList.of());
  }

  default Optional<PropertyHtml> findPropertyByPath(String pathString) {
    return findPropertyByPath(PropertyHtml.PATH_SPLITTER.splitToList(pathString));
  }

  default Optional<PropertyHtml> findPropertyByPath(List<String> path) {
    return getProperties().stream()
        .filter(property -> Objects.equals(property.getPropertyPath(), path))
        .findFirst()
        .or(
            () ->
                getProperties().stream()
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
        getProperties().stream()
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
          getProperties().stream()
              .filter(property -> property.getSchema().filter(SchemaBase::isSpatial).isEmpty())
              .map(property -> property.findPropertyByPath(path))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toUnmodifiableList());
    return properties;
  }
}
