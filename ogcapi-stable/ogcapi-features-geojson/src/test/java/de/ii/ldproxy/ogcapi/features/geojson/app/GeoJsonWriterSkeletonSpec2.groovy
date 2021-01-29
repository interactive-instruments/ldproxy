package de.ii.ldproxy.ogcapi.features.geojson.app

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformerGeoJson
import de.ii.xtraplatform.features.domain.FeatureType
import de.ii.xtraplatform.features.domain.ImmutableFeatureType
import spock.lang.Shared
import spock.lang.Specification

class GeoJsonWriterSkeletonSpec2 extends Specification {

    @Shared FeatureType featureMapping = new ImmutableFeatureType.Builder().name("f1")
            .build()

    def "GeoJsonWriterSkeleton middleware given a feature collection"() {
        given:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        String expected = "{" + System.lineSeparator() +
                "  \"type\" : \"FeatureCollection\"," + System.lineSeparator() +
                "  \"features\" : [ {" + System.lineSeparator() +
                "    \"type\" : \"Feature\"" + System.lineSeparator() +
                "  } ]" + System.lineSeparator() +
                "}"


        when:
        writeFeature(outputStream, true)
        String actual = GeoJsonWriterSetupUtil.asString(outputStream)

        then:
        actual == expected
    }


    def "GeoJsonWriterSkeleton middleware given a single feature"() {
        given:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        String expected = "{" + System.lineSeparator() +
                "  \"type\" : \"Feature\"" + System.lineSeparator() +
                "}"


        when:
        writeFeature(outputStream, false)
        String actual = GeoJsonWriterSetupUtil.asString(outputStream)

        then:
        actual == expected
    }


    private void writeFeature(ByteArrayOutputStream outputStream,
                              boolean isCollection) throws IOException, URISyntaxException {
        FeatureTransformationContextGeoJson transformationContext = GeoJsonWriterSetupUtil.createTransformationContext(outputStream, isCollection)
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(transformationContext, ImmutableList.of(new GeoJsonWriterSkeleton()))

        transformer.onStart(OptionalLong.empty(), OptionalLong.empty())
        transformer.onFeatureStart(featureMapping)
        transformer.onFeatureEnd()
        transformer.onEnd()
    }

}
