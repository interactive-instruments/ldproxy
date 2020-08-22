/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.google.common.collect.ImmutableList;
import com.greghaskins.spectrum.Spectrum;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformerGeoJson;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

import static com.greghaskins.spectrum.dsl.specification.Specification.beforeEach;
import static com.greghaskins.spectrum.dsl.specification.Specification.context;
import static com.greghaskins.spectrum.dsl.specification.Specification.describe;
import static com.greghaskins.spectrum.dsl.specification.Specification.it;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

/**
 * @author zahnen
 */
@RunWith(Spectrum.class)
public class GeoJsonWriterCrsSpec {

    private final static EpsgCrs DEFAULT_CRS = OgcCrs.CRS84;
    private final static EpsgCrs OTHER_CRS = EpsgCrs.of(4258);

    interface AssertTransform {
        void accept(ByteArrayOutputStream outputStream, boolean isCollection, EpsgCrs crs);
    }

    {

        final AssertTransform itShouldWriteNothing = (outputStream, isCollection, crs) -> {
            it("it should write nothing", () -> {

                runTransformer(outputStream, isCollection, crs);

                String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                String expected = "{ }";

                assertEquals(actual, expected);
            });
        };

        final AssertTransform itShouldWriteCrs = (outputStream, isCollection, crs) -> {
            it("it should write crs property", () -> {

                runTransformer(outputStream, isCollection, crs);

                String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                String expected = "{" + System.lineSeparator() +
                        "  \"crs\" : \"http://www.opengis.net/def/crs/EPSG/0/4258\"" + System.lineSeparator() +
                        "}";

                assertEquals(actual, expected);
            });
        };

        describe("GeoJson writer CRS middleware", () -> {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            beforeEach(outputStream::reset);

            context("for FeatureCollections", () -> {

                context("if target is not WGS84", () -> {
                    //runTransformer(outputStream, true, OTHER_CRS);

                    itShouldWriteCrs.accept(outputStream, true, OTHER_CRS);

                });

                context("if target is WGS84", () -> {
                    //runTransformer(outputStream, true, DEFAULT_CRS);

                    itShouldWriteNothing.accept(outputStream, true, DEFAULT_CRS);
                });

            });

            context("for single Features", () -> {

                context("if target is not WGS84", () -> {
                    //runTransformer(outputStream, false, OTHER_CRS);

                    itShouldWriteCrs.accept(outputStream, false, OTHER_CRS);

                });

                context("if target is WGS84", () -> {
                    //runTransformer(outputStream, false, DEFAULT_CRS);

                    itShouldWriteNothing.accept(outputStream, false, DEFAULT_CRS);
                });

            });

        });

        /*feature("GeoJson writer CRS middleware", () -> {

            scenarioOutline("Cucumber eating",
                    (crs, isCollection) -> {

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        final Variable<FeatureTransformationContextGeoJson> transformationContext = new Variable<>();
                        final Variable<FeatureTransformerGeoJson> transformer = new Variable<>();

                        given("target CRS is " + (Objects.equals(crs, DEFAULT_CRS) ? "" : "not ") + "WGS84", () -> {
                            transformationContext.set(createTransformationContext(outputStream, true, DEFAULT_CRS));
                            transformer.set(new FeatureTransformerGeoJson(transformationContext.get(), ImmutableList.of(new FeatureWriterGeoJsonCrs())));
                        });
                        when("a " + (isCollection ? "FeatureCollection" : "Feature") + " is written", () -> {
                            runTransformer(transformationContext.get(), transformer.get());
                        });
                        then((Objects.equals(crs, DEFAULT_CRS) ? "nothing" : "the crs property") + " is written", () -> {
                            String expected = "{ }";
                            String actual = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);

                            assertEquals(actual, expected);
                        });
                    },

                    withExamples(
                            example(DEFAULT_CRS, true),
                            example(DEFAULT_CRS, false),
                            example(OTHER_CRS, true),
                            example(OTHER_CRS, false))

            );

        });*/

    }

    private void runTransformer(FeatureTransformationContextGeoJson transformationContext, FeatureTransformerGeoJson transformer) throws IOException {
        transformationContext.getJson()
                             .writeStartObject();
        transformer.onStart(OptionalLong.empty(), OptionalLong.empty());
        transformer.onFeatureStart(null);
        transformationContext.getJson()
                             .writeEndObject();
        transformer.onEnd();
    }

    private void runTransformer(ByteArrayOutputStream outputStream, boolean isCollection, EpsgCrs crs) throws IOException, URISyntaxException {
        outputStream.reset();
        FeatureTransformationContextGeoJson transformationContext = createTransformationContext(outputStream, isCollection, crs);
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(transformationContext, ImmutableList.of(new GeoJsonWriterCrs()));

        transformationContext.getJson()
                             .writeStartObject();
        transformer.onStart(OptionalLong.empty(), OptionalLong.empty());
        transformer.onFeatureStart(null);
        transformationContext.getJson()
                             .writeEndObject();
        transformer.onEnd();
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
                                                           .defaultCrs(OgcCrs.CRS84)
                                                           .apiData(new ImmutableOgcApiDataV2.Builder()
                                                                                                .id("s")
                                                                                                .serviceType("OGC_API")
                                                                                                /*.featureProvider(new ImmutableFeatureProviderDataTransformer.Builder()
                                                                                                        .providerType("WFS")
                                                                                                        .connectorType("HTML")
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
                                                           .geoJsonConfig(new ImmutableGeoJsonConfiguration.Builder().enabled(true).nestedObjectStrategy(FeatureTransformerGeoJson.NESTED_OBJECTS.NEST).multiplicityStrategy(FeatureTransformerGeoJson.MULTIPLICITY.ARRAY).useFormattedJsonOutput(true).build())
                                                           .build();

    }

}
