/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Optional;

public interface SchemaGeneratorOpenApi {
    String getSchemaReferenceOpenApi(String collectionIdOrName, SchemaGeneratorFeature.SCHEMA_TYPE type);

    Schema getSchemaOpenApi(OgcApiDataV2 apiData, String collectionId, SchemaGeneratorFeature.SCHEMA_TYPE type);

    Schema getSchemaOpenApi(FeatureSchema featureType, FeatureTypeConfigurationOgcApi collectionData, SchemaGeneratorFeature.SCHEMA_TYPE type);

    Optional<Schema> getSchemaOpenApi(OgcApiDataV2 apiData, String collectionId, String propertyName);
}
