/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public interface SfFlatConfiguration extends ExtensionConfiguration, PropertyTransformations {

  /**
   * @langEn If the data is flattened and the feature schema includes arrays, `maxMultiplicity`
   *     properties will be created for each array property. If an instance has more values in an
   *     array, only the first values are included in the data.
   * @langDe TODO_DOCS
   * @default 3
   */
  @Nullable
  Integer getMaxMultiplicity();

  @JsonIgnore
  default Map<String, List<PropertyTransformation>> extendWithFlattenIfMissing() {
    if (!hasTransformation(
        PropertyTransformations.WILDCARD,
        transformation -> transformation.getFlatten().isPresent())) {

      return withTransformation(
          PropertyTransformations.WILDCARD,
          new ImmutablePropertyTransformation.Builder().flatten(".").build());
    }

    return ImmutableMap.of();
  }
}
