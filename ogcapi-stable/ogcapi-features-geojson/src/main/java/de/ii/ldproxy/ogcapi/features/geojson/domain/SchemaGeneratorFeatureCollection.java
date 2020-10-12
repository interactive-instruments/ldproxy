package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.geojson.domain.SchemaGeneratorFeature;
import io.swagger.v3.oas.models.media.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides(specifications = {SchemaGeneratorFeatureCollection.class})
@Instantiate
public class SchemaGeneratorFeatureCollection {

    @Requires
    SchemaGeneratorFeature schemaGeneratorFeature;

    final static Schema GENERIC = new ObjectSchema()
            .required(ImmutableList.of("type","features"))
            .addProperties("type", new StringSchema()._enum(ImmutableList.of("FeatureCollection")))
            .addProperties("features", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/featureGeoJSON")))
            .addProperties("links", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link")))
            .addProperties("timeStamp", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/timeStamp"))
            .addProperties("numberMatched", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberMatched"))
            .addProperties("numberReturned", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberReturned"));

    public String getSchemaReferenceOpenApi() { return "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/featureCollectionGeoJSON"; }

    public Schema getSchemaOpenApi() {
        return GENERIC;
    }

    public String getSchemaReferenceOpenApi(String collectionId) { return "#/components/schemas/featureCollectionGeoJson_"+collectionId; }

    public Schema getSchemaOpenApi(OgcApiDataV2 apiData, String collectionId) {
        return new ObjectSchema()
                .required(ImmutableList.of("type","features"))
                .addProperties("type", new StringSchema()._enum(ImmutableList.of("FeatureCollection")))
                .addProperties("features", new ArraySchema().items(new Schema().$ref(schemaGeneratorFeature.getSchemaReferenceOpenApi(collectionId))))
                .addProperties("links", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link")))
                .addProperties("timeStamp", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/timeStamp"))
                .addProperties("numberMatched", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberMatched"))
                .addProperties("numberReturned", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberReturned"));
    }

    public String getSchemaReferenceByName(String name) { return "#/components/schemas/featureCollectionGeoJson_"+name; }

    public Schema getSchemaOpenApiForName(String name) {
        return new ObjectSchema()
                .required(ImmutableList.of("type","features"))
                .addProperties("type", new StringSchema()._enum(ImmutableList.of("FeatureCollection")))
                .addProperties("features", new ArraySchema().items(new Schema().$ref(schemaGeneratorFeature.getSchemaReferenceOpenApi(name)))
                .addProperties("links", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link")))
                .addProperties("timeStamp", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/timeStamp")))
                .addProperties("numberMatched", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberMatched"))
                .addProperties("numberReturned", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberReturned"));
    }
}
