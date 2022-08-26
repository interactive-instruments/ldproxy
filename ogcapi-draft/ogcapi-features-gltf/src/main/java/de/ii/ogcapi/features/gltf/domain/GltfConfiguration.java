/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableGltfConfiguration.Builder.class)
public interface GltfConfiguration extends ExtensionConfiguration, PropertyTransformations {

  // TODO support value arrays
  enum GLTF_TYPE {
    INT8,
    UINT8,
    INT16,
    UINT16,
    INT32,
    UINT32,
    INT64,
    UINT64,
    FLOAT32,
    FLOAT64,
    STRING,
    BOOLEAN
  }

  @Nullable
  Boolean getMeshQuantization();

  @Value.Derived
  default boolean useMeshQuantization() {
    return Boolean.TRUE.equals(getMeshQuantization());
  }

  @Nullable
  Boolean getWithNormals();

  @Value.Derived
  default boolean writeNormals() {
    return Boolean.TRUE.equals(getWithNormals());
  }

  @Nullable
  Boolean getPolygonOrientationNotGuaranteed();

  @Value.Derived
  default boolean polygonOrientationIsNotGuaranteed() {
    return Boolean.TRUE.equals(getPolygonOrientationNotGuaranteed());
  }

  Map<String, GLTF_TYPE> getProperties();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableGltfConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    return new ImmutableGltfConfiguration.Builder()
        .from(source)
        .from(this)
        .transformations(
            PropertyTransformations.super
                .mergeInto((PropertyTransformations) source)
                .getTransformations())
        .build();
  }
}
