/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.google.common.collect.ImmutableSortedMap;
import de.ii.xtraplatform.features.domain.FeatureBase;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import org.immutables.value.Value;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

@Value.Modifiable
@Value.Style(set = "*")
public interface FeatureMVT extends FeatureBase<PropertyMVT, FeatureSchema> {

  @Value.Lazy
  default Optional<PropertyMVT> getId() {
    return getProperties().stream().filter(property -> property.getSchema().filter(
        SchemaBase::isId).isPresent()).findFirst();
  }

  @Value.Lazy
  default String getIdValue() {
    return getId().map(PropertyMVT::getValue).orElse(null);
  }

  @Value.Lazy
  default SortedMap<String, Object> getPropertiesAsMap() {
    return getProperties().stream()
        .filter(PropertyBase::isValue)
        .filter(propertyMVT -> Objects.nonNull(propertyMVT.getValue()))
        .map(propertyMVT -> {
          Object value;
          switch (propertyMVT.getSchema().map(FeatureSchema::getType).orElse(Type.STRING)) {
            case BOOLEAN:
              value = "t".equalsIgnoreCase(propertyMVT.getValue()) || "true".equalsIgnoreCase(
                  propertyMVT.getValue()) || "1".equals(propertyMVT.getValue());
              break;
            case INTEGER:
              try {
                value = Long.parseLong(propertyMVT.getValue());
                break;
              } catch (Throwable e) {
                //ignore
                boolean br = true;
              }
            case FLOAT:
              try {
                value = Double.parseDouble(propertyMVT.getValue());
                break;
              } catch (Throwable e) {
                //ignore
                boolean br = true;
              }
            case DATETIME:
            case STRING:
            default:
              value = propertyMVT.getValue();
          }
          return new SimpleImmutableEntry<>(propertyMVT.getName(), value);
        })
        .collect(ImmutableSortedMap.toImmutableSortedMap(String::compareTo, Map.Entry::getKey, Map.Entry::getValue));
  }

  @Value.Lazy
  default boolean hasGeometry() {
    return getGeometry().isPresent();
  }

  @Value.Lazy
  default Optional<PropertyMVT> getGeometry() {
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
