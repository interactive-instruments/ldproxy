/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.features.domain.transform.WithTransformationsApplied;
import java.util.Objects;
import java.util.Optional;

public class SchemaCacheSfFlat extends FeatureSchemaCache {

  @Override
  protected FeatureSchema deriveSchema(
      FeatureSchema schema,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      ExtensionConfiguration configuration,
      PropertyTransformations transformations) {

    WithTransformationsApplied schemaTransformer = new WithTransformationsApplied(transformations);

    Optional<String> separator = schemaTransformer.getFlatteningSeparator(schema);
    if (separator.isEmpty()) {
      return schema.accept(schemaTransformer);
    }

    int maxMultiplicity =
        Objects.requireNonNull(((SfFlatConfiguration) configuration).getMaxMultiplicity());

    FanOutArrays arrayTransformer = new FanOutArrays(separator.get(), maxMultiplicity);

    return schema.accept(schemaTransformer).accept(arrayTransformer).get(0);
  }
}
