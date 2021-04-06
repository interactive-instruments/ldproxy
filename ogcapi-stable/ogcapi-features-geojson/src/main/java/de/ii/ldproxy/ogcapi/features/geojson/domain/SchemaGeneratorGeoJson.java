package de.ii.ldproxy.ogcapi.features.geojson.domain;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature;
import de.ii.xtraplatform.features.domain.FeatureSchema;

import java.util.Optional;

public interface SchemaGeneratorGeoJson {
    JsonSchemaObject getSchemaJson(OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri, SchemaGeneratorFeature.SCHEMA_TYPE type);

    JsonSchemaObject getSchemaJson(OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri, SchemaGeneratorFeature.SCHEMA_TYPE type, Optional<VERSION> version);

    JsonSchemaObject getSchemaJson(OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri, SchemaGeneratorFeature.SCHEMA_TYPE type, VERSION version);

    JsonSchemaObject getSchemaJson(FeatureSchema featureSchema, FeatureTypeConfigurationOgcApi collectionData, Optional<String> schemaUri, SchemaGeneratorFeature.SCHEMA_TYPE type, VERSION version);

    public enum VERSION {V201909, V7}
}
