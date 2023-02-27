/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import de.ii.xtraplatform.features.domain.FeatureBase;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Modifiable
@Value.Style(set = "*")
public interface FeatureGltf extends FeatureBase<PropertyGltf, FeatureSchema> {

  Optional<String> getItemType();

  FeatureGltf itemType(String itemType);

  @Value.Default
  default boolean inCollection() {
    return false;
  }

  @Override
  @Value.Default
  default String getName() {
    return getId()
        .flatMap(id -> Optional.ofNullable(id.getFirstValue()))
        .orElse(FeatureBase.super.getName());
  }

  @Value.Lazy
  default Optional<PropertyGltf> getId() {
    return getProperties().stream()
        .filter(property -> property.getSchema().filter(SchemaBase::isId).isPresent())
        .findFirst();
  }

  default Optional<PropertyGltf> findPropertyByPath(String pathString) {
    return findPropertyByPath(PropertyGltf.PATH_SPLITTER.splitToList(pathString));
  }

  default Optional<PropertyGltf> findPropertyByPath(List<String> path) {
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
}
