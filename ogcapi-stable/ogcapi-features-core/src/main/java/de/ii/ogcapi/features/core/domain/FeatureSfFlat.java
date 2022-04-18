/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableSortedMap;
import de.ii.xtraplatform.features.domain.FeatureBase;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import org.immutables.value.Value;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.stream.Collectors;

@Value.Modifiable
@Value.Style(set = "*")
public interface FeatureSfFlat extends FeatureBase<PropertySfFlat, FeatureSchema> {

  @Value.Lazy
  default Optional<PropertySfFlat> getId() {
    return getProperties().stream().filter(property -> property.getSchema().filter(
        SchemaBase::isId).isPresent()).findFirst();
  }

  @Value.Lazy
  default String getIdValue() {
    return getId().map(PropertySfFlat::getValue).orElse(null);
  }

  @Value.Lazy
  default SortedMap<String, Object> getPropertiesAsMap() {
    return getProperties().stream()
        .map(property -> new SimpleImmutableEntry<>(property.getName(), getValue(property, false)))
        .filter(entry -> Objects.nonNull(entry.getValue()))
        .collect(ImmutableSortedMap.toImmutableSortedMap(String::compareTo, Map.Entry::getKey, Map.Entry::getValue));
  }

  // Since properties must be "flat" (no arrays or objects), we map any arrays or objects to
  // a string using a JSON representation of arrays and objects.
  private Object getValue(PropertySfFlat property, boolean withQuotes) {
    switch (property.getType()) {
      case VALUE:
        switch (property.getSchema().map(FeatureSchema::getType).orElse(Type.UNKNOWN)) {
          case BOOLEAN:
            return "t".equalsIgnoreCase(property.getValue())
                || "true".equalsIgnoreCase(property.getValue())
                || "1".equals(property.getValue());

          case INTEGER:
            try {
              return Long.parseLong(Objects.requireNonNull(property.getValue()));
            } catch (Throwable e) {
              //ignore
              return null;
            }

          case FLOAT:
            try {
              return Double.parseDouble(Objects.requireNonNull(property.getValue()));
            } catch (Throwable e) {
              //ignore
              return null;
            }

          case DATE:
          case DATETIME:
          case STRING:
          case UNKNOWN:
            return withQuotes ? "'"+property.getValue()+"'" : property.getValue();

          case GEOMETRY:
            // geometries are handled separately, ignore them in this map
          default:
            return null;
        }

      case OBJECT:
        return property.getSchema()
            .map(FeatureSchema::getType)
            .orElse(Type.UNKNOWN)
            .equals(Type.GEOMETRY) ? null : getObjectAsString(property);

      case ARRAY:
        return getArrayAsString(property);
    }

    return null;
  }

  private String getObjectAsString(PropertySfFlat property) {
    String value = property.getNestedProperties()
        .stream()
        .map(p -> {
          Object val = getValue(p, true);
          if (Objects.isNull(val))
            return null;
          return String.format("'%s': %s", p.getName(), val);
        })
        .filter(Objects::nonNull)
        .collect(Collectors.joining(", "));
    if (value.isBlank())
      return null;
    return String.format("{ %s }", value);
  }

  private String getArrayAsString(PropertySfFlat property) {
    String value = property.getNestedProperties()
        .stream()
        .map(p -> {
          Object val = getValue(p, true);
          if (Objects.isNull(val))
            return null;
          return val.toString();
        })
        .filter(Objects::nonNull)
        .collect(Collectors.joining(", "));
    if (value.isBlank())
      return null;
    return String.format("[ %s ]", value);
  }

  @Value.Lazy
  default boolean hasGeometry() {
    return getGeometry().isPresent();
  }

  @Value.Lazy
  default Optional<PropertySfFlat> getGeometry() {
    return getProperties().stream()
        .filter(property -> property.getSchema()
            .filter(SchemaBase::isSpatial)
            .filter(SchemaBase::isPrimaryGeometry)
            .isPresent())
        .findFirst();
  }

  default Optional<Geometry> getJtsGeometry(GeometryFactory geometryFactory) {
    return getGeometry().flatMap(geometry -> geometry.getJtsGeometry(geometryFactory));
  }
}
