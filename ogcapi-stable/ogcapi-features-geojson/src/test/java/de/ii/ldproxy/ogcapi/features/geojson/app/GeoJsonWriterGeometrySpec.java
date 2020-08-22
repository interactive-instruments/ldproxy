/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.greghaskins.spectrum.Spectrum;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProperty;
import de.ii.xtraplatform.features.domain.ImmutableFeatureType;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static com.greghaskins.spectrum.dsl.specification.Specification.beforeEach;
import static com.greghaskins.spectrum.dsl.specification.Specification.context;
import static com.greghaskins.spectrum.dsl.specification.Specification.describe;
import static com.greghaskins.spectrum.dsl.specification.Specification.it;
import static org.testng.Assert.assertEquals;

/**
 * @author zahnen
 */
@RunWith(Spectrum.class)
public class GeoJsonWriterGeometrySpec {

    static final FeatureType featureMapping = new ImmutableFeatureType.Builder().name("f1")
                                                                                .properties(ImmutableMap.of())
                                                                                .build();

    static final FeatureProperty propertyMapping = new ImmutableFeatureProperty.Builder().name("p1")
                                                                                         .path("")
                                                                                         .build();

    static final FeatureProperty propertyMapping2 = new ImmutableFeatureProperty.Builder().name("p2")
                                                                                          .path("")
                                                                                          .build();

    static final FeatureProperty geometryMapping = new ImmutableFeatureProperty.Builder().name("geometry")
                                                                                         .path("")
                                                                                         .type(FeatureProperty.Type.GEOMETRY)
                                                                                         .build();

    static final String value1 = "val1";
    static final String value2 = "val2";
    static final String coordinates = "10 50, 11 51";


    /*static {
        featureMapping.setName("f1");
        propertyMapping.setName("p1");
        propertyMapping2.setName("p2");
        geometryMapping.setGeometryType(GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.MULTI_POLYGON);
    }*/

    {

        describe("GeoJson writer geometry middleware", () -> {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            beforeEach(outputStream::reset);

            context("if coordinates are given", () -> {

                it("it should write a valid GeoJSON geometry", () -> {

                    writeFeature(outputStream, ImmutableList.of(2, 1, 2, 2, 1, 1));

                    String expected = "{" + System.lineSeparator() +
                            "  \"geometry\" : {" + System.lineSeparator() +
                            "    \"type\" : \"MultiPolygon\"," + System.lineSeparator() +
                            "    \"coordinates\" : [ [ [ [ 10, 50 ], [ 11, 51 ] ], [ [ 10, 50 ], [ 11, 51 ] ] ], [ [ [ 10, 50 ], [ 11, 51 ] ] ], [ [ [ 10, 50 ], [ 11, 51 ] ], [ [ 10, 50 ], [ 11, 51 ] ], [ [ 10, 50 ], [ 11, 51 ] ] ] ]" + System.lineSeparator() +
                            "  }" + System.lineSeparator() +
                            "}";
                    String actual = GeoJsonWriterSetupUtil.asString(outputStream);

                    assertEquals(actual, expected);
                });

            });

            context("if no coordinates are given", () -> {

                it("it should write a null GeoJSON geometry", () -> {

                    writeFeature(outputStream, ImmutableList.of());

                    String expected = "{" + System.lineSeparator() +
                            "  \"geometry\" : null" + System.lineSeparator() +
                            "}";
                    String actual = GeoJsonWriterSetupUtil.asString(outputStream);

                    assertEquals(actual, expected);
                });

            });

            context("test buffering", () -> {

            });

        });

    }

    private void writeFeature(ByteArrayOutputStream outputStream,
                              List<Integer> nestingPattern) throws IOException, URISyntaxException {
        FeatureTransformationContextGeoJson transformationContext = GeoJsonWriterSetupUtil.createTransformationContext(outputStream, false);
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(transformationContext, ImmutableList.of(new GeoJsonWriterGeometry()));

        transformationContext.getJson()
                             .writeStartObject();

        transformer.onFeatureStart(featureMapping);
        transformer.onPropertyStart(propertyMapping, ImmutableList.of());
        transformer.onPropertyText(value1);
        transformer.onPropertyEnd();
        transformer.onGeometryStart(geometryMapping, SimpleFeatureGeometry.MULTI_POLYGON, 2);

        for (Integer depth : nestingPattern) {
            for (int i = 0; i < depth; i++) {
                transformer.onGeometryNestedStart();
            }
            transformer.onGeometryCoordinates(coordinates);
            for (int i = 0; i < depth; i++) {
                transformer.onGeometryNestedEnd();
            }
        }

        transformer.onGeometryEnd();
        transformer.onPropertyStart(propertyMapping2, ImmutableList.of());
        transformer.onPropertyText(value2);
        transformer.onPropertyEnd();
        transformer.onFeatureEnd();

        transformationContext.getJson()
                             .writeEndObject();

        transformer.onEnd();
    }

}
