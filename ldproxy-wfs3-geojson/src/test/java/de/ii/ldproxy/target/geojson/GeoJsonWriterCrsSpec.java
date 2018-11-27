/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableList;
import com.greghaskins.spectrum.Spectrum;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3ServiceData;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfo;
import de.ii.xtraplatform.feature.provider.wfs.ImmutableConnectionInfo;
import de.ii.xtraplatform.feature.provider.wfs.ImmutableFeatureProviderDataWfs;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

import static com.greghaskins.spectrum.dsl.specification.Specification.*;
import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

/**
 * @author zahnen
 */
@RunWith(Spectrum.class)
public class GeoJsonWriterCrsSpec {

    private final static EpsgCrs OTHER_CRS = new EpsgCrs(4258);

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
                String expected = "{\n" +
                        "  \"crs\" : \"http://www.opengis.net/def/crs/EPSG/0/4258\"\n" +
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
                                                           .serviceData(ImmutableWfs3ServiceData.builder()
                                                                                                .id("s")
                                                                                                .serviceType("WFS3")
                                                                                                .featureProvider(ImmutableFeatureProviderDataWfs.builder()
                                                                                                                                                .connectionInfo(ImmutableConnectionInfo.builder()
                                                                                                                                                                                       .uri(new URI("http://localhost"))
                                                                                                                                                                                       .method(ConnectionInfo.METHOD.GET)
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
                                                           .wfs3Request(new Wfs3RequestContext() {
                                                               @Override
                                                               public Wfs3MediaType getMediaType() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public URICustomizer getUriCustomizer() {
                                                                   return null;
                                                               }

                                                               @Override
                                                               public String getStaticUrlPrefix() {
                                                                   return null;
                                                               }
                                                           })
                                                           .limit(10)
                                                           .offset(20)
                                                           .maxAllowableOffset(0)
                                                           .state(ModifiableStateGeoJson.create())
                                                           .geoJsonConfig(new GeoJsonConfig())
                                                           .build();

    }

}