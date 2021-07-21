/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature;
import de.ii.xtraplatform.features.domain.FeatureSchema;

import java.util.Optional;

public interface SchemaGeneratorFeatureGeoJson extends SchemaGeneratorGeoJson {

    JsonSchemaObject getSchemaJson(OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri, SchemaGeneratorFeature.SCHEMA_TYPE type);

    JsonSchemaObject getSchemaJson(OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri, SchemaGeneratorFeature.SCHEMA_TYPE type, Optional<VERSION> version);

    JsonSchemaObject getSchemaJson(OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri, SchemaGeneratorFeature.SCHEMA_TYPE type, VERSION version);

    JsonSchemaObject getSchemaJson(FeatureSchema featureSchema, FeatureTypeConfigurationOgcApi collectionData, Optional<String> schemaUri, SchemaGeneratorFeature.SCHEMA_TYPE type, VERSION version);

}
