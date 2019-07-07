package de.ii.ldproxy.target.geojson;

public class JsonNestingStrategyFactory {

    public static JsonNestingStrategy getNestingStrategy(FeatureTransformerGeoJson.NESTED_OBJECTS nestedObjects, FeatureTransformerGeoJson.MULTIPLICITY multiplicity) {

        if (nestedObjects == FeatureTransformerGeoJson.NESTED_OBJECTS.FLATTEN && multiplicity == FeatureTransformerGeoJson.MULTIPLICITY.SUFFIX) {
            return new JsonNestingStrategyFlattenSuffix();
        }

        return new JsonNestingStrategyNestArray();
    }
}
