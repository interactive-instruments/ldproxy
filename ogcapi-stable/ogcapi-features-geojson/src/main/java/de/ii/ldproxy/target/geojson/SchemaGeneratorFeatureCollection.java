package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import io.swagger.v3.oas.models.media.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class SchemaGeneratorFeatureCollection {

    final static Schema GENERIC = new ObjectSchema()
            .required(ImmutableList.of("type","features"))
            .addProperties("type", new StringSchema()._enum(ImmutableList.of("FeatureCollection")))
            .addProperties("features", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/featureGeoJSON")))
            .addProperties("links", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link")))
            .addProperties("timestamp", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/timestamp"))
            .addProperties("numberMatched", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberMatched"))
            .addProperties("numberReturned", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberReturned"));

    public static String referenceGeneric() { return "https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/featureCollectionGeoJSON"; }

    public static Schema getGeneric() {
        return GENERIC;
    }

    public static String referenceForCollection(String collectionId) { return "#/components/schemas/featureCollectionGeoJson_"+collectionId; }

    public Schema generateForCollection(OgcApiApiDataV2 apiData, String collectionId) {
        return new ObjectSchema()
                .required(ImmutableList.of("type","features"))
                .addProperties("type", new StringSchema()._enum(ImmutableList.of("FeatureCollection")))
                .addProperties("features", new ArraySchema().items(new Schema().$ref(SchemaGeneratorFeature.referenceForCollection(collectionId))))
                .addProperties("links", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link")))
                .addProperties("timestamp", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/timestamp"))
                .addProperties("numberMatched", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberMatched"))
                .addProperties("numberReturned", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberReturned"));
    }

    public static String referenceByName(String name) { return "#/components/schemas/featureCollectionGeoJson_"+name; }

    public Schema generateForName(String name) {
        return new ObjectSchema()
                .required(ImmutableList.of("type","features"))
                .addProperties("type", new StringSchema()._enum(ImmutableList.of("FeatureCollection")))
                .addProperties("features", new ArraySchema().items(new Schema().$ref(SchemaGeneratorFeature.referenceByName(name)))
                .addProperties("links", new ArraySchema().items(new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/link")))
                .addProperties("timestamp", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/timestamp")))
                .addProperties("numberMatched", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberMatched"))
                .addProperties("numberReturned", new Schema().$ref("https://raw.githubusercontent.com/opengeospatial/ogcapi-features/master/core/openapi/ogcapi-features-1.yaml#/components/schemas/numberReturned"));
    }
}
