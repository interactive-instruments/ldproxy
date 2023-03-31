/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.flatgeobuf.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.features.core.domain.SfFlatConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

/**
 * @buildingBlock FLATGEOBUF
 * @examplesAll <code>
 * ```yaml
 * - buildingBlock: FLATGEOBUF
 *   enabled: true
 * ```
 *     </code>
 */
@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableFlatgeobufConfiguration.Builder.class)
public interface FlatgeobufConfiguration extends SfFlatConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableFlatgeobufConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableFlatgeobufConfiguration.Builder builder =
        ((ImmutableFlatgeobufConfiguration.Builder) source.getBuilder())
            .from(source)
            .from(this)
            .transformations(
                SfFlatConfiguration.super
                    .mergeInto((PropertyTransformations) source)
                    .getTransformations());

    return builder.build();
  }

  @Value.Check
  default FlatgeobufConfiguration alwaysFlatten() {
    Map<String, List<PropertyTransformation>> transformations = extendWithFlattenIfMissing();
    if (transformations.isEmpty()) {
      // a flatten transformation is already set
      return this;
    }

    return new ImmutableFlatgeobufConfiguration.Builder()
        .from(this)
        .transformations(transformations)
        .build();
  }
}
