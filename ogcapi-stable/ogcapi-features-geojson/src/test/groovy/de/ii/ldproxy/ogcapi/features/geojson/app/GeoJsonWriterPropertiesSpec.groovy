package de.ii.ldproxy.ogcapi.features.geojson.app

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.domain.*
import de.ii.ldproxy.ogcapi.features.geojson.app.GeoJsonWriterProperties
import de.ii.ldproxy.ogcapi.features.geojson.domain.*
import de.ii.xtraplatform.crs.domain.CrsTransformer
import de.ii.xtraplatform.crs.domain.EpsgCrs
import de.ii.xtraplatform.crs.domain.OgcCrs
import de.ii.xtraplatform.features.domain.FeatureProperty
import de.ii.xtraplatform.features.domain.ImmutableFeatureProperty
import org.mockito.Mockito
import spock.lang.Shared
import spock.lang.Specification

import java.util.stream.Collectors
import java.util.stream.IntStream

import static org.mockito.Mockito.mock

class GeoJsonWriterPropertiesSpec extends Specification {

    @Shared FeatureProperty propertyMapping = new ImmutableFeatureProperty.Builder().name("p1")
            .path("")
            .build()

    @Shared FeatureProperty propertyMapping2 = new ImmutableFeatureProperty.Builder().name("p2")
            .path("")
            .type(FeatureProperty.Type.INTEGER)
            .build()

    @Shared String value1 = "val1"
    @Shared String value2 = "2"

    def "GeoJson writer properties middleware, value types (String)"() {
        given:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        String expected = "{" + System.lineSeparator() +
                "  \"properties\" : {" + System.lineSeparator() +
                "    \"p1\" : \"val1\"," + System.lineSeparator() +
                "    \"p2\" : 2" + System.lineSeparator() +
                "  }" + System.lineSeparator() +
                "}"

        when:
        runTransformer(outputStream, ImmutableList.of(propertyMapping, propertyMapping2),
                ImmutableList.of(ImmutableList.of(), ImmutableList.of()), ImmutableList.of(value1, value2))
        String actual = GeoJsonWriterSetupUtil.asString(outputStream)

        then:
        actual == expected
    }

    def "GeoJson writer properties middleware, strategy is nested, one level depth"() {
        given:
        FeatureProperty mapping1 = new ImmutableFeatureProperty.Builder().name("foto.bemerkung")
                .path("")
                .type(FeatureProperty.Type.STRING)
                .build()
        FeatureProperty mapping2 = new ImmutableFeatureProperty.Builder().name("foto.hauptfoto")
                .path("")
                .type(FeatureProperty.Type.STRING)
                .build()
        FeatureProperty mapping3 = new ImmutableFeatureProperty.Builder().name("kennung")
                .path("")
                .type(FeatureProperty.Type.STRING)
                .build()
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        String expected = "{" + System.lineSeparator() +
                "  \"properties\" : {" + System.lineSeparator() +
                "    \"foto\" : {" + System.lineSeparator() +
                "      \"bemerkung\" : \"xyz\"," + System.lineSeparator() +
                "      \"hauptfoto\" : \"xyz\"" + System.lineSeparator() +
                "    }," + System.lineSeparator() +
                "    \"kennung\" : \"xyz\"" + System.lineSeparator() +
                "  }" + System.lineSeparator() +
                "}"

        when:
        runTransformer(outputStream, ImmutableList.of(mapping1, mapping2, mapping3),
                ImmutableList.of(ImmutableList.of(), ImmutableList.of(), ImmutableList.of()))
        String actual = GeoJsonWriterSetupUtil.asString(outputStream)

        then:
        actual == expected
    }

    def "GeoJson writer properties middleware, strategy is nested, one level depth with multiplicity"() {
        given:
        // multiple object
        FeatureProperty mapping1 = new ImmutableFeatureProperty.Builder().name("foto[foto].bemerkung")
                .path("")
                .type(FeatureProperty.Type.STRING)
                .build()
        List<Integer> multiplicity11 = ImmutableList.of(1)
        List<Integer> multiplicity12 = ImmutableList.of(2)

        FeatureProperty mapping2 = new ImmutableFeatureProperty.Builder().name("foto[foto].hauptfoto")
                .path("")
                .type(FeatureProperty.Type.STRING)
                .build()
        List<Integer> multiplicity21 = ImmutableList.of(1)
        List<Integer> multiplicity22 = ImmutableList.of(2)

        // multiple value
        FeatureProperty mapping3 = new ImmutableFeatureProperty.Builder().name("fachreferenz[fachreferenz]")
                .path("")
                .type(FeatureProperty.Type.STRING)
                .build()
        List<Integer> multiplicity31 = ImmutableList.of(1)
        List<Integer> multiplicity32 = ImmutableList.of(2)

        //TODO if lastPropertyIsNested
        FeatureProperty mapping4 = new ImmutableFeatureProperty.Builder().name("kennung")
                .path("")
                .type(FeatureProperty.Type.STRING)
                .build()

        ImmutableList<FeatureProperty> mappings = ImmutableList.of(mapping1, mapping2, mapping1, mapping2, mapping3, mapping3, mapping4)
        ImmutableList<List<Integer>> multiplicities = ImmutableList.of(multiplicity11, multiplicity21, multiplicity12, multiplicity22, multiplicity31, multiplicity32, ImmutableList.of())
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        String expected = "{" + System.lineSeparator() +
                "  \"properties\" : {" + System.lineSeparator() +
                "    \"foto\" : [ {" + System.lineSeparator() +
                "      \"bemerkung\" : \"xyz\"," + System.lineSeparator() +
                "      \"hauptfoto\" : \"xyz\"" + System.lineSeparator() +
                "    }, {" + System.lineSeparator() +
                "      \"bemerkung\" : \"xyz\"," + System.lineSeparator() +
                "      \"hauptfoto\" : \"xyz\"" + System.lineSeparator() +
                "    } ]," + System.lineSeparator() +
                "    \"fachreferenz\" : [ \"xyz\", \"xyz\" ]," + System.lineSeparator() +
                "    \"kennung\" : \"xyz\"" + System.lineSeparator() +
                "  }" + System.lineSeparator() +
                "}"

        when:
        runTransformer(outputStream, mappings, multiplicities)
        String actual = GeoJsonWriterSetupUtil.asString(outputStream)

        then:
        actual == expected
    }

    def "GeoJson writer properties middleware, strategy is nested, two level depth with multiplicity"() {
        given:
        // multiple object
        FeatureProperty mapping1 = new ImmutableFeatureProperty.Builder().name("raumreferenz[raumreferenz].datumAbgleich")
                .path("")
                .type(FeatureProperty.Type.STRING)
                .build()
        List<Integer> multiplicity11 = ImmutableList.of(1)

        FeatureProperty mapping2 = new ImmutableFeatureProperty.Builder().name("raumreferenz[raumreferenz].ortsangaben[ortsangaben].kreis")
                .path("")
                .type(FeatureProperty.Type.STRING)
                .build()
        List<Integer> multiplicity21 = ImmutableList.of(1, 1)
        List<Integer> multiplicity22 = ImmutableList.of(1, 2)

        // multiple value
        FeatureProperty mapping3 = new ImmutableFeatureProperty.Builder().name("raumreferenz[raumreferenz].ortsangaben[ortsangaben].flurstueckskennung[ortsangaben_flurstueckskennung]")
                .path("")
                .type(FeatureProperty.Type.STRING)
                .build()
        List<Integer> multiplicity31 = ImmutableList.of(1, 1, 1)
        List<Integer> multiplicity32 = ImmutableList.of(1, 1, 2)
        List<Integer> multiplicity33 = ImmutableList.of(1, 2, 1)

        //TODO if lastPropertyIsNested
        FeatureProperty mapping4 = new ImmutableFeatureProperty.Builder().name("kennung")
                .path("")
                .type(FeatureProperty.Type.STRING)
                .build()

        ImmutableList<FeatureProperty> mappings = ImmutableList.of(mapping1, mapping2, mapping3, mapping3, mapping2, mapping3, mapping4)
        ImmutableList<List<Integer>> multiplicities = ImmutableList.of(multiplicity11, multiplicity21, multiplicity31, multiplicity32, multiplicity22, multiplicity33, ImmutableList.of())
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        String expected = "{" + System.lineSeparator() +
                "  \"properties\" : {" + System.lineSeparator() +
                "    \"raumreferenz\" : [ {" + System.lineSeparator() +
                "      \"datumAbgleich\" : \"xyz\"," + System.lineSeparator() +
                "      \"ortsangaben\" : [ {" + System.lineSeparator() +
                "        \"kreis\" : \"xyz\"," + System.lineSeparator() +
                "        \"flurstueckskennung\" : [ \"xyz\", \"xyz\" ]" + System.lineSeparator() +
                "      }, {" + System.lineSeparator() +
                "        \"kreis\" : \"xyz\"," + System.lineSeparator() +
                "        \"flurstueckskennung\" : [ \"xyz\" ]" + System.lineSeparator() +
                "      } ]" + System.lineSeparator() +
                "    } ]," + System.lineSeparator() +
                "    \"kennung\" : \"xyz\"" + System.lineSeparator() +
                "  }" + System.lineSeparator() +
                "}"

        when:
        runTransformer(outputStream, mappings, multiplicities)
        String actual = GeoJsonWriterSetupUtil.asString(outputStream)

        then:
        actual == expected
    }

    private static void runTransformer(ByteArrayOutputStream outputStream, List<FeatureProperty> mappings,
                                       List<List<Integer>> multiplicities,
                                       List<String> values) throws IOException, URISyntaxException {
        outputStream.reset()
        FeatureTransformationContextGeoJson transformationContext = createTransformationContext(outputStream, true, null)
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(transformationContext, ImmutableList.of(new GeoJsonWriterProperties()))

        transformationContext.getJson()
                .writeStartObject()

        transformer.onStart(OptionalLong.empty(), OptionalLong.empty())
        transformer.onFeatureStart(null)

        for (int i = 0; i < mappings.size(); i++) {
            transformer.onPropertyStart(mappings.get(i), multiplicities.get(i))
            transformer.onPropertyText(values.get(i))
            transformer.onPropertyEnd()
        }

        transformer.onFeatureEnd()

        transformationContext.getJson()
                .writeEndObject()
        transformer.onEnd()
    }

    private static void runTransformer(ByteArrayOutputStream outputStream, List<FeatureProperty> mappings,
                                       List<List<Integer>> multiplicities) throws IOException, URISyntaxException {
        String value = "xyz"
        runTransformer(outputStream, mappings, multiplicities, IntStream.range(0, mappings.size())
                .mapToObj{i -> value}
                .collect(Collectors.toList()))
    }

    private static FeatureTransformationContextGeoJson createTransformationContext(OutputStream outputStream,
                                                                                   boolean isCollection,
                                                                                   EpsgCrs crs) throws URISyntaxException {
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
