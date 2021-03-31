package de.ii.ldproxy.ogcapi.features.core.domain;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import io.swagger.v3.oas.models.media.Schema;

public interface SchemaGeneratorCollectionOpenApi {
    String getSchemaReferenceOpenApi();

    Schema getSchemaOpenApi();

    String getSchemaReferenceOpenApi(String collectionId, SchemaGeneratorFeature.SCHEMA_TYPE type);

    Schema getSchemaOpenApi(OgcApiDataV2 apiData, String collectionId, SchemaGeneratorFeature.SCHEMA_TYPE type);

    String getSchemaReferenceByName(String name, SchemaGeneratorFeature.SCHEMA_TYPE type);

    Schema getSchemaOpenApiForName(String name, SchemaGeneratorFeature.SCHEMA_TYPE type);
}
