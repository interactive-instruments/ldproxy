/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.csv.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.features.core.domain.SfFlatConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

/**
 * @title CSV
 */
@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableCsvConfiguration.Builder.class)
public interface CsvConfiguration extends SfFlatConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableCsvConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableCsvConfiguration.Builder builder =
        ((ImmutableCsvConfiguration.Builder) source.getBuilder())
            .from(source)
            .from(this)
            .transformations(
                SfFlatConfiguration.super
                    .mergeInto((PropertyTransformations) source)
                    .getTransformations());

    return builder.build();
  }

  @JsonIgnore
  @Value.Check
  default CsvConfiguration alwaysFlatten() {
    Map<String, List<PropertyTransformation>> transformations = extendWithFlattenIfMissing();
    if (transformations.isEmpty()) {
      // a flatten transformation is already set
      return this;
    }

    return new ImmutableCsvConfiguration.Builder()
        .from(this)
        .transformations(transformations)
        .build();
  }
}
