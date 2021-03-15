package de.ii.ldproxy.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides(specifications = {SchemaGeneratorFeatureCollectionOpenApi.class})
@Instantiate
public class SchemaGeneratorFeatureCollectionOpenApi implements SchemaGeneratorCollectionOpenApi {

    final static Schema GENERIC = new ObjectSchema()
            .required(ImmutableList.of("type","features"))
            .addProperties("type", new StringSchema()._enum(ImmutableList.of("FeatureCollection")))
            .addProperties("features", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/featureGeoJSON")))
            .addProperties("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/Link")))
            .addProperties("timeStamp", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/timeStamp"))
            .addProperties("numberMatched", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberMatched"))
            .addProperties("numberReturned", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberReturned"));

    private final SchemaGeneratorOpenApi schemaGeneratorFeature;

    public SchemaGeneratorFeatureCollectionOpenApi(@Requires SchemaGeneratorOpenApi schemaGeneratorFeature) {
        this.schemaGeneratorFeature = schemaGeneratorFeature;
    }

    @Override
    public String getSchemaReferenceOpenApi() { return "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/featureCollectionGeoJSON"; }

    @Override
    public Schema getSchemaOpenApi() {
        return GENERIC;
    }

    @Override
    public String getSchemaReferenceOpenApi(String collectionId, SchemaGeneratorFeature.SCHEMA_TYPE type) { return "#/components/schemas/featureCollectionGeoJson_"+collectionId; }

    @Override
    public Schema getSchemaOpenApi(OgcApiDataV2 apiData, String collectionId, SchemaGeneratorFeature.SCHEMA_TYPE type) {
        return new ObjectSchema()
                .required(ImmutableList.of("type","features"))
                .addProperties("type", new StringSchema()._enum(ImmutableList.of("FeatureCollection")))
                .addProperties("features", new ArraySchema().items(new Schema().$ref(schemaGeneratorFeature.getSchemaReferenceOpenApi(collectionId, type))))
                .addProperties("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/Link")))
                .addProperties("timeStamp", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/timeStamp"))
                .addProperties("numberMatched", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberMatched"))
                .addProperties("numberReturned", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberReturned"));
    }

    @Override
    public String getSchemaReferenceByName(String name, SchemaGeneratorFeature.SCHEMA_TYPE type) { return "#/components/schemas/featureCollectionGeoJson_"+name; }

    @Override
    public Schema getSchemaOpenApiForName(String name, SchemaGeneratorFeature.SCHEMA_TYPE type) {
        return new ObjectSchema()
                .required(ImmutableList.of("type","features"))
                .addProperties("type", new StringSchema()._enum(ImmutableList.of("FeatureCollection")))
                .addProperties("features", new ArraySchema().items(new Schema().$ref(schemaGeneratorFeature.getSchemaReferenceOpenApi(name, type)))
                .addProperties("links", new ArraySchema().items(new Schema().$ref("#/components/schemas/Link")))
                .addProperties("timeStamp", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/timeStamp")))
                .addProperties("numberMatched", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberMatched"))
                .addProperties("numberReturned", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberReturned"));
    }
}
