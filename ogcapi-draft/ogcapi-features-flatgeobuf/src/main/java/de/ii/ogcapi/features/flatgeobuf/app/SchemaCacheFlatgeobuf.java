/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.flatgeobuf.app;

import static de.ii.ogcapi.features.flatgeobuf.app.CapabilityFlatgeobuf.DEFAULT_MULTIPLICITY;

import de.ii.ogcapi.features.core.domain.FanOutArrays;
import de.ii.ogcapi.features.core.domain.FeatureSchemaCache;
import de.ii.ogcapi.features.flatgeobuf.domain.FlatgeobufConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.features.domain.transform.WithTransformationsApplied;
import java.util.Optional;

public class SchemaCacheFlatgeobuf extends FeatureSchemaCache {

  public SchemaCacheFlatgeobuf() {
    super();
  }

  @Override
  protected FeatureSchema deriveSchema(
      FeatureSchema schema, OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {

    Optional<PropertyTransformations> propertyTransformations =
        collectionData
            .getExtension(FlatgeobufConfiguration.class)
            .map(configuration -> (PropertyTransformations) configuration);

    WithTransformationsApplied schemaTransformer =
        propertyTransformations
            .map(WithTransformationsApplied::new)
            .orElse(new WithTransformationsApplied());

    Optional<String> separator = schemaTransformer.getFlatteningSeparator(schema);
    if (separator.isEmpty()) return schema.accept(schemaTransformer);

    int maxMultiplicity =
        collectionData
            .getExtension(FlatgeobufConfiguration.class)
            .map(FlatgeobufConfiguration::getMaxMultiplicity)
            .orElse(DEFAULT_MULTIPLICITY);

    FanOutArrays arrayTransformer = new FanOutArrays(separator.get(), maxMultiplicity);

    return schema.accept(schemaTransformer).accept(arrayTransformer).get(0);
  }
}
