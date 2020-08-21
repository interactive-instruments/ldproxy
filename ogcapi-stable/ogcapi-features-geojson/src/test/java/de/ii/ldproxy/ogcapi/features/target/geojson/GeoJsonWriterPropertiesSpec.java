/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.target.geojson;

import com.google.common.collect.ImmutableList;
import com.greghaskins.spectrum.Spectrum;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProperty;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.greghaskins.spectrum.dsl.specification.Specification.beforeEach;
import static com.greghaskins.spectrum.dsl.specification.Specification.context;
import static com.greghaskins.spectrum.dsl.specification.Specification.describe;
import static com.greghaskins.spectrum.dsl.specification.Specification.it;
import static com.greghaskins.spectrum.dsl.specification.Specification.xit;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

/**
 * @author zahnen
 */
@RunWith(Spectrum.class)
public class GeoJsonWriterPropertiesSpec {

    static final FeatureProperty propertyMapping = new ImmutableFeatureProperty.Builder().name("p1")
                                                                                         .path("")
                                                                                         .build();

    static final FeatureProperty propertyMapping2 = new ImmutableFeatureProperty.Builder().name("p2")
                                                                                          .path("")
                                                                                          .type(FeatureProperty.Type.INTEGER)
                                                                                          .build();

    static final String value1 = "val1";
    static final String value2 = "2";


    /*static {
        propertyMapping.setName("p1");
        propertyMapping.setType(GeoJsonMapping.GEO_JSON_TYPE.STRING);
        propertyMapping2.setName("p2");
        propertyMapping2.setType(GeoJsonMapping.GEO_JSON_TYPE.NUMBER);
    }*/

    {

        describe("GeoJson writer properties middleware", () -> {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            beforeEach(outputStream::reset);

            context("value types", () -> {

                context("if type is string", () -> {

                    it("it should write string property", () -> {

                        runTransformer(outputStream, ImmutableList.of(propertyMapping, propertyMapping2), ImmutableList.of(ImmutableList.of(), ImmutableList.of()), ImmutableList.of(value1, value2));

                        String actual = GeoJsonWriterSetupUtil.asString(outputStream);
                        String expected = "{" + System.lineSeparator() +
                                "  \"properties\" : {" + System.lineSeparator() +
                                "    \"p1\" : \"val1\"," + System.lineSeparator() +
                                "    \"p2\" : 2" + System.lineSeparator() +
                                "  }" + System.lineSeparator() +
                                "}";

                        assertEquals(actual, expected);
                    });

                });

            });

            context("nesting", () -> {

                context("if strategy is flat", () -> {

                });

                context("if strategy is nested", () -> {

                    context("one level depth", () -> {

                        it("it should write nested json objects", () -> {

                            FeatureProperty mapping1 = new ImmutableFeatureProperty.Builder().name("foto.bemerkung")
                                                                                             .path("")
                                                                                             .type(FeatureProperty.Type.STRING)
                                                                                             .build();
                            FeatureProperty mapping2 = new ImmutableFeatureProperty.Builder().name("foto.hauptfoto")
                                                                                             .path("")
                                                                                             .type(FeatureProperty.Type.STRING)
                                                                                             .build();
                            FeatureProperty mapping3 = new ImmutableFeatureProperty.Builder().name("kennung")
                                                                                             .path("")
                                                                                             .type(FeatureProperty.Type.STRING)
                                                                                             .build();

                            runTransformer(outputStream, ImmutableList.of(mapping1, mapping2, mapping3), ImmutableList.of(ImmutableList.of(), ImmutableList.of(), ImmutableList.of()));

                            String actual = GeoJsonWriterSetupUtil.asString(outputStream);
                            String expected = "{" + System.lineSeparator() +
                                    "  \"properties\" : {" + System.lineSeparator() +
                                    "    \"foto\" : {" + System.lineSeparator() +
                                    "      \"bemerkung\" : \"xyz\"," + System.lineSeparator() +
                                    "      \"hauptfoto\" : \"xyz\"" + System.lineSeparator() +
                                    "    }," + System.lineSeparator() +
                                    "    \"kennung\" : \"xyz\"" + System.lineSeparator() +
                                    "  }" + System.lineSeparator() +
                                    "}";

                            assertEquals(actual, expected);
                        });

                    });

                    context("one level depth with multiplicity", () -> {

                        //TODO
                        xit("it should write nested json objects and arrays", () -> {

                            // multiple object
                            FeatureProperty mapping1 = new ImmutableFeatureProperty.Builder().name("foto[foto].bemerkung")
                                                                                             .path("")
                                                                                             .type(FeatureProperty.Type.STRING)
                                                                                             .build();
                            List<Integer> multiplicity11 = ImmutableList.of(1);
                            List<Integer> multiplicity12 = ImmutableList.of(2);

                            FeatureProperty mapping2 = new ImmutableFeatureProperty.Builder().name("foto[foto].hauptfoto")
                                                                                             .path("")
                                                                                             .type(FeatureProperty.Type.STRING)
                                                                                             .build();
                            List<Integer> multiplicity21 = ImmutableList.of(1);
                            List<Integer> multiplicity22 = ImmutableList.of(2);

                            // multiple value
                            FeatureProperty mapping3 = new ImmutableFeatureProperty.Builder().name("fachreferenz[fachreferenz]")
                                                                                             .path("")
                                                                                             .type(FeatureProperty.Type.STRING)
                                                                                             .build();
                            List<Integer> multiplicity31 = ImmutableList.of(1);
                            List<Integer> multiplicity32 = ImmutableList.of(2);

                            //TODO if lastPropertyIsNested
                            FeatureProperty mapping4 = new ImmutableFeatureProperty.Builder().name("kennung")
                                                                                             .path("")
                                                                                             .type(FeatureProperty.Type.STRING)
                                                                                             .build();

                            ImmutableList<FeatureProperty> mappings = ImmutableList.of(mapping1, mapping2, mapping1, mapping2, mapping3, mapping3, mapping4);
                            ImmutableList<List<Integer>> multiplicities = ImmutableList.of(multiplicity11, multiplicity21, multiplicity12, multiplicity22, multiplicity31, multiplicity32, ImmutableList.of());

                            runTransformer(outputStream, mappings, multiplicities);

                            String actual = GeoJsonWriterSetupUtil.asString(outputStream);
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
                                    "}";

                            assertEquals(actual, expected);
                        });

                    });

                    context("two level depth with multiplicity", () -> {

                        xit("it should write nested json objects and arrays", () -> {

                            // multiple object
                            FeatureProperty mapping1 = new ImmutableFeatureProperty.Builder().name("raumreferenz[raumreferenz].datumAbgleich")
                                                                                             .path("")
                                                                                             .type(FeatureProperty.Type.STRING)
                                                                                             .build();
                            List<Integer> multiplicity11 = ImmutableList.of(1);

                            FeatureProperty mapping2 = new ImmutableFeatureProperty.Builder().name("raumreferenz[raumreferenz].ortsangaben[ortsangaben].kreis")
                                                                                             .path("")
                                                                                             .type(FeatureProperty.Type.STRING)
                                                                                             .build();
                            List<Integer> multiplicity21 = ImmutableList.of(1, 1);
                            List<Integer> multiplicity22 = ImmutableList.of(1, 2);

                            // multiple value
                            FeatureProperty mapping3 = new ImmutableFeatureProperty.Builder().name("raumreferenz[raumreferenz].ortsangaben[ortsangaben].flurstueckskennung[ortsangaben_flurstueckskennung]")
                                                                                             .path("")
                                                                                             .type(FeatureProperty.Type.STRING)
                                                                                             .build();
                            List<Integer> multiplicity31 = ImmutableList.of(1, 1, 1);
                            List<Integer> multiplicity32 = ImmutableList.of(1, 1, 2);
                            List<Integer> multiplicity33 = ImmutableList.of(1, 2, 1);

                            //TODO if lastPropertyIsNested
                            FeatureProperty mapping4 = new ImmutableFeatureProperty.Builder().name("kennung")
                                                                                             .path("")
                                                                                             .type(FeatureProperty.Type.STRING)
                                                                                             .build();

                            ImmutableList<FeatureProperty> mappings = ImmutableList.of(mapping1, mapping2, mapping3, mapping3, mapping2, mapping3, mapping4);
                            ImmutableList<List<Integer>> multiplicities = ImmutableList.of(multiplicity11, multiplicity21, multiplicity31, multiplicity32, multiplicity22, multiplicity33, ImmutableList.of());

                            runTransformer(outputStream, mappings, multiplicities);

                            String actual = GeoJsonWriterSetupUtil.asString(outputStream);
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
                                    "}";

                            assertEquals(actual, expected);
                        });

                    });

                });
            });

        });

    }

    private void runTransformer(ByteArrayOutputStream outputStream, List<FeatureProperty> mappings,
                                List<List<Integer>> multiplicities,
                                List<String> values) throws IOException, URISyntaxException {
        outputStream.reset();
        FeatureTransformationContextGeoJson transformationContext = createTransformationContext(outputStream, true, null);
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(transformationContext, ImmutableList.of(new GeoJsonWriterProperties()));

        transformationContext.getJson()
                             .writeStartObject();

        transformer.onStart(OptionalLong.empty(), OptionalLong.empty());
        transformer.onFeatureStart(null);

        for (int i = 0; i < mappings.size(); i++) {
            transformer.onPropertyStart(mappings.get(i), multiplicities.get(i));
            transformer.onPropertyText(values.get(i));
            transformer.onPropertyEnd();
        }

        transformer.onFeatureEnd();

        transformationContext.getJson()
                             .writeEndObject();
        transformer.onEnd();
    }

    private void runTransformer(ByteArrayOutputStream outputStream, List<FeatureProperty> mappings,
                                List<List<Integer>> multiplicities) throws IOException, URISyntaxException {
        String value = "xyz";
        runTransformer(outputStream, mappings, multiplicities, IntStream.range(0, mappings.size())
                                                                        .mapToObj(i -> value)
                                                                        .collect(Collectors.toList()));
    }

    private FeatureTransformationContextGeoJson createTransformationContext(OutputStream outputStream,
                                                                            boolean isCollection,
                                                                            EpsgCrs crs) throws URISyntaxException {
        CrsTransformer crsTransformer = null;
        if (Objects.nonNull(crs)) {
            crsTransformer = mock(CrsTransformer.class);
            Mockito.when(crsTransformer.getTargetCrs())
                   .thenReturn(crs);
        }

        return ImmutableFeatureTransformationContextGeoJson.builder()
                                                           .crsTransformer(Optional.ofNullable(crsTransformer))
                                                           .defaultCrs(OgcCrs.CRS84)
                                                           .apiData(new ImmutableOgcApiDataV2.Builder()
                                                                   .id("s")
                                                                   .serviceType("OGC_API")
                                                                   /*.featureProvider(new ImmutableFeatureProviderDataTransformer.Builder()
                                                                           .providerType("WFS")
                                                                           .connectorType("WFS")
                                                                                                                   .connectionInfo(new ImmutableConnectionInfoWfsHttp.Builder()
                                                                                                                                                                 .uri(new URI("http://localhost"))
                                                                                                                                                                 .method(ConnectionInfoWfsHttp.METHOD.GET)
                                                                                                                                                                 .version("2.0.0")
                                                                                                                                                                 .gmlVersion("3.2.1")
                                                                                                                                                                 .build())
                                                                                                                   .nativeCrs(new EpsgCrs())
                                                                                                                   .build())*/
                                                                   .build())
                                                           .collectionId("xyz")
                                                           .outputStream(outputStream)
                                                           .links(ImmutableList.of())
                                                           .isFeatureCollection(isCollection)
                                                           .ogcApiRequest(new ApiRequestContext() {
                                                               @Override
                                                               public ApiMediaType getMediaType() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public List<ApiMediaType> getAlternateMediaTypes() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public Optional<Locale> getLanguage() {
                                                                   return Optional.empty();
                                                               }

                                                               @Override
                                                               public OgcApi getApi() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public URICustomizer getUriCustomizer() {
                                                                   return new URICustomizer();
                                                               }

                                                               @Override
                                                               public String getStaticUrlPrefix() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public Map<String, String> getParameters() {
                                                                   return null;
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
                                                           .build();

    }

}
