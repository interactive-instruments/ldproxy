package de.ii.ldproxy.ogcapi.features.geojson.app

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.domain.*
import de.ii.ldproxy.ogcapi.features.geojson.app.GeoJsonWriterCrs
import de.ii.ldproxy.ogcapi.features.geojson.domain.*
import de.ii.xtraplatform.crs.domain.CrsTransformer
import de.ii.xtraplatform.crs.domain.EpsgCrs
import de.ii.xtraplatform.crs.domain.OgcCrs
import org.mockito.Mockito
import spock.lang.Shared
import spock.lang.Specification

import java.nio.charset.StandardCharsets

import static org.mockito.Mockito.mock

class GeoJsonWriterCrsSpec2 extends Specification {

    @Shared EpsgCrs DEFAULT_CRS = OgcCrs.CRS84
    @Shared EpsgCrs OTHER_CRS = EpsgCrs.of(4258)

    def "GeoJson writer CRS middleware for FeatureCollections if target is WGS84"() {
        given:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        boolean isCollection = true
        EpsgCrs crs = DEFAULT_CRS
        String expected = "{ }"

        when:
        runTransformer(outputStream, isCollection, crs)
        String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8)

        then:
        actual == expected
    }

    def "GeoJson writer CRS middleware for FeatureCollections if target is not WGS84"() {
        given:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        boolean isCollection = true
        EpsgCrs crs = OTHER_CRS
        String expected = "{" + System.lineSeparator() +
                "  \"crs\" : \"http://www.opengis.net/def/crs/EPSG/0/4258\"" + System.lineSeparator() +
                "}"

        when:
        runTransformer(outputStream, isCollection, crs)
        String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8)

        then:
        actual == expected
    }


    def "GeoJson writer CRS middleware for single Features if target is WGS84"() {
        given:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        boolean isCollection = false
        EpsgCrs crs = DEFAULT_CRS
        String expected = "{ }"

        when:
        runTransformer(outputStream, isCollection, crs)
        String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8)

        then:
        actual == expected
    }

    def "GeoJson writer CRS middleware for single Features if target is not WGS84"() {
        given:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        boolean isCollection = false
        EpsgCrs crs = OTHER_CRS
        String expected = "{" + System.lineSeparator() +
                "  \"crs\" : \"http://www.opengis.net/def/crs/EPSG/0/4258\"" + System.lineSeparator() +
                "}"

        when:
        runTransformer(outputStream, isCollection, crs)
        String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8)

        then:
        actual == expected
    }

    private static void runTransformer(ByteArrayOutputStream outputStream, boolean isCollection, EpsgCrs crs) throws IOException, URISyntaxException {
        outputStream.reset()
        FeatureTransformationContextGeoJson transformationContext = createTransformationContext(outputStream, isCollection, crs)
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(transformationContext, ImmutableList.of(new GeoJsonWriterCrs()))

        transformationContext.getJson()
                .writeStartObject()
        transformer.onStart(OptionalLong.empty(), OptionalLong.empty())
        transformer.onFeatureStart(null)
        transformationContext.getJson()
                .writeEndObject()
        transformer.onEnd()
    }

    private static FeatureTransformationContextGeoJson createTransformationContext(OutputStream outputStream, boolean isCollection, EpsgCrs crs) throws URISyntaxException {
        CrsTransformer crsTransformer = null
        if (Objects.nonNull(crs)) {
            crsTransformer = mock(CrsTransformer.class)
            Mockito.when(crsTransformer.getTargetCrs())
                    .thenReturn(crs)
        }

        return ImmutableFeatureTransformationContextGeoJson.builder()
                .crsTransformer(Optional.ofNullable(crsTransformer))
                .defaultCrs(OgcCrs.CRS84)
                .apiData(new ImmutableOgcApiDataV2.Builder()
                        .id("s")
                        .serviceType("OGC_API")
                        .build())
                .collectionId("xyz")
                .outputStream(outputStream)
                .links(ImmutableList.of())
                .isFeatureCollection(isCollection)
                .ogcApiRequest(new ApiRequestContext() {
                    @Override
                    ApiMediaType getMediaType() {
                        return null
                    }

                    @Override
                    List<ApiMediaType> getAlternateMediaTypes() {
                        return null
                    }

                    @Override
                    Optional<Locale> getLanguage() {
                        return Optional.empty()
                    }

                    @Override
                    OgcApi getApi() {
                        return null
                    }

                    @Override
                    URICustomizer getUriCustomizer() {
                        return new URICustomizer()
                    }

                    @Override
                    String getStaticUrlPrefix() {
                        return null
                    }

                    @Override
                    Map<String, String> getParameters() {
                        return null
                    }
                })
                .limit(10)
                .offset(20)
                .maxAllowableOffset(0)
                .isHitsOnly(false)
                .state(ModifiableStateGeoJson.create())
                .geoJsonConfig(new ImmutableGeoJsonConfiguration.Builder()
                        .enabled(true)
                        .nestedObjectStrategy(FeatureTransformerGeoJson.NESTED_OBJECTS.NEST)
                        .multiplicityStrategy(FeatureTransformerGeoJson.MULTIPLICITY.ARRAY)
                        .useFormattedJsonOutput(true)
                        .build())
                .build()

    }
}
