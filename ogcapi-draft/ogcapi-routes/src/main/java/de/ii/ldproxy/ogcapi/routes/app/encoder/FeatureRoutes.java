/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app.encoder;

import de.ii.ldproxy.ogcapi.features.core.domain.Geometry;
import de.ii.xtraplatform.features.domain.FeatureBase;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import org.immutables.value.Value;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Value.Modifiable
@Value.Style(set = "*")
public interface FeatureRoutes extends FeatureBase<PropertyRoutes, FeatureSchema> {

  @Override
  @Value.Default
  default String getName() {
    return getId().flatMap(id -> Optional.ofNullable(id.getFirstValue())).orElse(FeatureBase.super.getName());
  }

  @Value.Lazy
  default Optional<PropertyRoutes> getId() {
    return getProperties().stream().filter(property -> property.getSchema().filter(
        SchemaBase::isId).isPresent()).findFirst();
  }

  @Value.Lazy
  default String getIdValue() {
    return getId().map(PropertyRoutes::getFirstValue).orElse(null);
  }

  @Value.Lazy
  default Optional<PropertyRoutes> getGeometry() {
    return getProperties().stream()
        .filter(property -> property.getSchema().filter(SchemaBase::isSpatial).isPresent())
        .findFirst()
        .or(() -> getProperties().stream()
            .map(property -> property.getGeometry())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst());
  }

  @Value.Lazy
  default Optional<Geometry.LineString> parseGeometry() {
    return getGeometry().map(PropertyRoutes::parseGeometry);
  }

  default Optional<PropertyRoutes> findPropertyByPath(String pathString) {
    return findPropertyByPath(PropertyRoutes.PATH_SPLITTER.splitToList(pathString));
  }

  default Optional<PropertyRoutes> findPropertyByPath(List<String> path) {
    return getProperties().stream()
        .filter(property -> Objects.equals(property.getPropertyPath(), path))
        .findFirst()
        .or(() -> getProperties().stream()
            .filter(property -> property.getSchema().filter(SchemaBase::isSpatial).isEmpty())
            .map(property -> property.findPropertyByPath(path))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst());
  }
}
