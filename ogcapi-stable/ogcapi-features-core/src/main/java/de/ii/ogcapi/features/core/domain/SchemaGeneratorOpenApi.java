/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Optional;

public interface SchemaGeneratorOpenApi {
  String getSchemaReference(String collectionIdOrName);

  Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId);

  Schema<?> getSchema(FeatureSchema featureSchema, FeatureTypeConfigurationOgcApi collectionData);

  Optional<Schema<?>> getProperty(
      FeatureSchema featureSchema,
      FeatureTypeConfigurationOgcApi collectionData,
      String propertyName);
}
