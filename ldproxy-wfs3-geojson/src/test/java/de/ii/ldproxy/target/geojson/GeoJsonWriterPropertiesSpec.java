/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableList;
import com.greghaskins.spectrum.Spectrum;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.provider.wfs.ImmutableConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureProviderDataTransformer;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
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

    static final GeoJsonPropertyMapping propertyMapping = new GeoJsonPropertyMapping();

    static final GeoJsonPropertyMapping propertyMapping2 = new GeoJsonPropertyMapping();

    static final String value1 = "val1";
    static final String value2 = "2";


    static {
        propertyMapping.setName("p1");
        propertyMapping.setType(GeoJsonMapping.GEO_JSON_TYPE.STRING);
        propertyMapping2.setName("p2");
        propertyMapping2.setType(GeoJsonMapping.GEO_JSON_TYPE.NUMBER);
    }

    {

        describe("GeoJson writer properties middleware", () -> {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            beforeEach(outputStream::reset);

            context("value types", () -> {

                context("if type is string", () -> {

                    it("it should write string property", () -> {

                        runTransformer(outputStream, ImmutableList.of(propertyMapping, propertyMapping2), ImmutableList.of(ImmutableList.of(), ImmutableList.of()), ImmutableList.of(value1, value2));

                        String actual = GeoJsonWriterSetupUtil.asString(outputStream);
                        String expected = "{\n" +
                                "  \"properties\" : {\n" +
                                "    \"p1\" : \"val1\",\n" +
                                "    \"p2\" : 2\n" +
                                "  }\n" +
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

                            //TODO: immutables
                            GeoJsonPropertyMapping mapping1 = new GeoJsonPropertyMapping();
                            mapping1.setName("foto.bemerkung");
                            mapping1.setType(GeoJsonMapping.GEO_JSON_TYPE.STRING);
                            GeoJsonPropertyMapping mapping2 = new GeoJsonPropertyMapping();
                            mapping2.setName("foto.hauptfoto");
                            mapping2.setType(GeoJsonMapping.GEO_JSON_TYPE.STRING);
                            //TODO if lastPropertyIsNested
                            GeoJsonPropertyMapping mapping3 = new GeoJsonPropertyMapping();
                            mapping3.setName("kennung");
                            mapping3.setType(GeoJsonMapping.GEO_JSON_TYPE.STRING);

                            runTransformer(outputStream, ImmutableList.of(mapping1, mapping2, mapping3), ImmutableList.of(ImmutableList.of(), ImmutableList.of(), ImmutableList.of()));

                            String actual = GeoJsonWriterSetupUtil.asString(outputStream);
                            String expected = "{\n" +
                                    "  \"properties\" : {\n" +
                                    "    \"foto\" : {\n" +
                                    "      \"bemerkung\" : \"xyz\",\n" +
                                    "      \"hauptfoto\" : \"xyz\"\n" +
                                    "    },\n" +
                                    "    \"kennung\" : \"xyz\"\n" +
                                    "  }\n" +
                                    "}";

                            assertEquals(actual, expected);
                        });

                    });

                    context("one level depth with multiplicity", () -> {

                        //TODO
                        xit("it should write nested json objects and arrays", () -> {

                            //TODO: immutables
                            // multiple object
                            GeoJsonPropertyMapping mapping1 = new GeoJsonPropertyMapping();
                            mapping1.setName("foto[foto].bemerkung");
                            mapping1.setType(GeoJsonMapping.GEO_JSON_TYPE.STRING);
                            List<Integer> multiplicity11 = ImmutableList.of(1);
                            List<Integer> multiplicity12 = ImmutableList.of(2);

                            GeoJsonPropertyMapping mapping2 = new GeoJsonPropertyMapping();
                            mapping2.setName("foto[foto].hauptfoto");
                            mapping2.setType(GeoJsonMapping.GEO_JSON_TYPE.STRING);
                            List<Integer> multiplicity21 = ImmutableList.of(1);
                            List<Integer> multiplicity22 = ImmutableList.of(2);

                            // multiple value
                            GeoJsonPropertyMapping mapping3 = new GeoJsonPropertyMapping();
                            mapping3.setName("fachreferenz[fachreferenz]");
                            mapping3.setType(GeoJsonMapping.GEO_JSON_TYPE.STRING);
                            List<Integer> multiplicity31 = ImmutableList.of(1);
                            List<Integer> multiplicity32 = ImmutableList.of(2);

                            //TODO if lastPropertyIsNested
                            GeoJsonPropertyMapping mapping4 = new GeoJsonPropertyMapping();
                            mapping4.setName("kennung");
                            mapping4.setType(GeoJsonMapping.GEO_JSON_TYPE.STRING);

                            ImmutableList<GeoJsonPropertyMapping> mappings = ImmutableList.of(mapping1, mapping2, mapping1, mapping2, mapping3, mapping3, mapping4);
                            ImmutableList<List<Integer>> multiplicities = ImmutableList.of(multiplicity11, multiplicity21, multiplicity12, multiplicity22, multiplicity31, multiplicity32, ImmutableList.of());

                            runTransformer(outputStream, mappings, multiplicities);

                            String actual = GeoJsonWriterSetupUtil.asString(outputStream);
                            String expected = "{\n" +
                                    "  \"properties\" : {\n" +
                                    "    \"foto\" : [ {\n" +
                                    "      \"bemerkung\" : \"xyz\",\n" +
                                    "      \"hauptfoto\" : \"xyz\"\n" +
                                    "    }, {\n" +
                                    "      \"bemerkung\" : \"xyz\",\n" +
                                    "      \"hauptfoto\" : \"xyz\"\n" +
                                    "    } ],\n" +
                                    "    \"fachreferenz\" : [ \"xyz\", \"xyz\" ],\n" +
                                    "    \"kennung\" : \"xyz\"\n" +
                                    "  }\n" +
                                    "}";

                            assertEquals(actual, expected);
                        });

                    });

                    context("two level depth with multiplicity", () -> {

                        xit("it should write nested json objects and arrays", () -> {

                            //TODO: immutables
                            // multiple object
                            GeoJsonPropertyMapping mapping1 = new GeoJsonPropertyMapping();
                            mapping1.setName("raumreferenz[raumreferenz].datumAbgleich");
                            mapping1.setType(GeoJsonMapping.GEO_JSON_TYPE.STRING);
                            List<Integer> multiplicity11 = ImmutableList.of(1);

                            GeoJsonPropertyMapping mapping2 = new GeoJsonPropertyMapping();
                            mapping2.setName("raumreferenz[raumreferenz].ortsangaben[ortsangaben].kreis");
                            mapping2.setType(GeoJsonMapping.GEO_JSON_TYPE.STRING);
                            List<Integer> multiplicity21 = ImmutableList.of(1, 1);
                            List<Integer> multiplicity22 = ImmutableList.of(1, 2);

                            // multiple value
                            GeoJsonPropertyMapping mapping3 = new GeoJsonPropertyMapping();
                            mapping3.setName("raumreferenz[raumreferenz].ortsangaben[ortsangaben].flurstueckskennung[ortsangaben_flurstueckskennung]");
                            mapping3.setType(GeoJsonMapping.GEO_JSON_TYPE.STRING);
                            List<Integer> multiplicity31 = ImmutableList.of(1, 1, 1);
                            List<Integer> multiplicity32 = ImmutableList.of(1, 1, 2);
                            List<Integer> multiplicity33 = ImmutableList.of(1, 2, 1);

                            //TODO if lastPropertyIsNested
                            GeoJsonPropertyMapping mapping4 = new GeoJsonPropertyMapping();
                            mapping4.setName("kennung");
                            mapping4.setType(GeoJsonMapping.GEO_JSON_TYPE.STRING);

                            ImmutableList<GeoJsonPropertyMapping> mappings = ImmutableList.of(mapping1, mapping2, mapping3, mapping3, mapping2, mapping3, mapping4);
                            ImmutableList<List<Integer>> multiplicities = ImmutableList.of(multiplicity11, multiplicity21, multiplicity31, multiplicity32, multiplicity22, multiplicity33, ImmutableList.of());

                            runTransformer(outputStream, mappings, multiplicities);

                            String actual = GeoJsonWriterSetupUtil.asString(outputStream);
                            String expected = "{\n" +
                                    "  \"properties\" : {\n" +
                                    "    \"raumreferenz\" : [ {\n" +
                                    "      \"datumAbgleich\" : \"xyz\",\n" +
                                    "      \"ortsangaben\" : [ {\n" +
                                    "        \"kreis\" : \"xyz\",\n" +
                                    "        \"flurstueckskennung\" : [ \"xyz\", \"xyz\" ]\n" +
                                    "      }, {\n" +
                                    "        \"kreis\" : \"xyz\",\n" +
                                    "        \"flurstueckskennung\" : [ \"xyz\" ]\n" +
                                    "      } ]\n" +
                                    "    } ],\n" +
                                    "    \"kennung\" : \"xyz\"\n" +
                                    "  }\n" +
                                    "}";

                            assertEquals(actual, expected);
                        });

                    });

                });
            });

        });

    }

    private void runTransformer(ByteArrayOutputStream outputStream, List<GeoJsonPropertyMapping> mappings, List<List<Integer>> multiplicities, List<String> values) throws IOException, URISyntaxException {
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

    private void runTransformer(ByteArrayOutputStream outputStream, List<GeoJsonPropertyMapping> mappings, List<List<Integer>> multiplicities) throws IOException, URISyntaxException {
        String value = "xyz";
        runTransformer(outputStream, mappings, multiplicities, IntStream.range(0, mappings.size())
                                                                        .mapToObj(i -> value)
                                                                        .collect(Collectors.toList()));
    }

    private FeatureTransformationContextGeoJson createTransformationContext(OutputStream outputStream, boolean isCollection, EpsgCrs crs) throws URISyntaxException {
        CrsTransformer crsTransformer = null;
        if (Objects.nonNull(crs)) {
            crsTransformer = mock(CrsTransformer.class);
            Mockito.when(crsTransformer.getTargetCrs())
                   .thenReturn(crs);
        }

        return ImmutableFeatureTransformationContextGeoJson.builder()
                                                           .crsTransformer(Optional.ofNullable(crsTransformer))
                                                           .serviceData(new ImmutableOgcApiDatasetData.Builder()
                                                                                                .id("s")
                                                                                                .serviceType("WFS3")
                                                                                                .featureProvider(new ImmutableFeatureProviderDataTransformer.Builder()
                                                                                                        .providerType("WFS")
                                                                                                        .connectorType("WFS")
                                                                                                                                                .connectionInfo(new ImmutableConnectionInfoWfsHttp.Builder()
                                                                                                                                                                                              .uri(new URI("http://localhost"))
                                                                                                                                                                                              .method(ConnectionInfoWfsHttp.METHOD.GET)
                                                                                                                                                                                              .version("2.0.0")
                                                                                                                                                                                              .gmlVersion("3.2.1")
                                                                                                                                                                                              .build())
                                                                                                                                                .nativeCrs(new EpsgCrs())
                                                                                                                                                .build())
                                                                                                .build())
                                                           .collectionName("xyz")
                                                           .outputStream(outputStream)
                                                           .links(ImmutableList.of())
                                                           .isFeatureCollection(isCollection)
                                                           .wfs3Request(new OgcApiRequestContext() {
                                                               @Override
                                                               public OgcApiMediaType getMediaType() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public List<OgcApiMediaType> getAlternativeMediaTypes() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public OgcApiDatasetData getDataset() {
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
                                                           })
                                                           .limit(10)
                                                           .offset(20)
                                                           .maxAllowableOffset(0)
                                                           .isHitsOnly(false)
                                                           .state(ModifiableStateGeoJson.create())
                                                           .geoJsonConfig(ImmutableGeoJsonConfig.builder().isEnabled(true).nestedObjectStrategy(FeatureTransformerGeoJson.NESTED_OBJECTS.NEST).multiplicityStrategy(FeatureTransformerGeoJson.MULTIPLICITY.ARRAY).build())
                                                           .build();

    }

}
