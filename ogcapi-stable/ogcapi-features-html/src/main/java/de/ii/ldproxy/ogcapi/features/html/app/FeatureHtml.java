/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import de.ii.xtraplatform.features.domain.FeatureBase;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Modifiable
@Value.Style(set = "*")
public interface FeatureHtml extends FeatureBase<PropertyHtml, FeatureSchema> {

  Optional<String> getItemType();

  FeatureHtml itemType(String itemType);

  @Value.Derived
  default String getSchemaOrgItemType() {
    return getItemType()
        .filter(itemType -> itemType.startsWith("http://schema.org/"))
        .map(itemType -> itemType.substring(18))
        .orElse(null);
  }

  @Override
  @Value.Default
  default String getName() {
    return getId().flatMap(id -> Optional.ofNullable(id.getFirstValue())).orElse(FeatureBase.super.getName());
  }

  @Value.Derived
  default Optional<PropertyHtml> getId() {
    return getProperties().stream().filter(property -> property.getSchema().filter(
        SchemaBase::isId).isPresent()).findFirst();
  }

  @Value.Derived
  default String getIdValue() {
    return getId().map(PropertyHtml::getFirstValue).orElse(null);
  }

  @Value.Derived
  default boolean hasObjects() {
    return getProperties().stream().anyMatch(property -> !property.isValue() && property.getSchema().filter(
        SchemaBase::isGeometry).isEmpty());
  }

  @Value.Derived
  default boolean hasGeometry() {
    return getProperties().stream().anyMatch(property -> property.getSchema().filter(
        SchemaBase::isGeometry).isPresent())
        || getProperties().stream().anyMatch(PropertyHtml::hasGeometry);
  }

  @Value.Derived
  default Optional<PropertyHtml> getGeometry() {
    return getProperties().stream()
        .filter(property -> property.getSchema().filter(SchemaBase::isGeometry).isPresent())
        .findFirst()
        .or(() -> getProperties().stream()
            .map(property -> property.getGeometry())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst());
  }

  @Value.Derived
  default String getGeoAsString() {
    Optional<PropertyHtml> geometry = getGeometry();
    return geometry
        .flatMap(geo -> {
          Optional<SimpleFeatureGeometry> geometryType = geo.getSchema()
              .flatMap(FeatureSchema::getGeometryType);
          if (geometryType.isPresent()) {
            if (geometryType.get() == SimpleFeatureGeometry.POINT) {
              List<PropertyHtml> coordinates = geo.getNestedProperties().get(0).getNestedProperties().get(0).getValues();
              String latitude = coordinates.get(0).getValue();
              String longitude = coordinates.get(1).getValue();
              return Optional.of(String.format(
                  "{ \"@type\": \"GeoCoordinates\", \"latitude\": \"%s\", \"longitude\": \"%s\" }",
                  latitude, longitude));
            }
          }

          return Optional.empty();
        })
        .orElse(null);
    /*if (Objects.nonNull(geo)) {
      if (geo.itemType.equalsIgnoreCase("http://schema.org/GeoShape")) {
        PropertyDTO geoprop = (PropertyDTO) geo.childList.get(0);
        String geomType = geoprop.itemProp;
        String coords = geoprop.values.get(0).value;
        if (Objects.nonNull(geomType) && Objects.nonNull(coords))
          return "{ \"@type\": \"GeoShape\", \"" +
              geomType + "\": \"" + coords + "\" }";
      } else if (geo.itemType.equalsIgnoreCase("http://schema.org/GeoCoordinates")) {
        String latitude = ((PropertyDTO) geo.childList.get(0)).values.get(0).value;
        String longitude = ((PropertyDTO) geo.childList.get(1)).values.get(0).value;
        if (Objects.nonNull(latitude) && Objects.nonNull(longitude))
          return "{ \"@type\": \"GeoCoordinates\", \"latitude\": \"" + latitude + "\", \"longitude\": \"" + longitude + "\" }";
      }
    }*/
  }

  default Optional<PropertyHtml> findPropertyByPath(String pathString) {
    return findPropertyByPath(PropertyHtml.PATH_SPLITTER.splitToList(pathString));
  }

  default Optional<PropertyHtml> findPropertyByPath(List<String> path) {
    return getProperties().stream()
        .filter(property -> Objects.equals(property.getPropertyPath(), path))
        .findFirst()
        .or(() -> getProperties().stream()
            .filter(property -> property.getSchema().filter(SchemaBase::isGeometry).isEmpty())
            .map(property -> property.findPropertyByPath(path))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst());
  }
}
