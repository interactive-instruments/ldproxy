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
import de.ii.xtraplatform.feature.query.api.SimpleFeatureGeometry;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static com.greghaskins.spectrum.dsl.specification.Specification.*;
import static org.testng.Assert.assertEquals;

/**
 * @author zahnen
 */
@RunWith(Spectrum.class)
public class GeoJsonWriterGeometrySpec {

    static final GeoJsonPropertyMapping featureMapping = new GeoJsonPropertyMapping();

    static final GeoJsonPropertyMapping propertyMapping = new GeoJsonPropertyMapping();

    static final GeoJsonPropertyMapping propertyMapping2 = new GeoJsonPropertyMapping();

    static final GeoJsonGeometryMapping geometryMapping = new GeoJsonGeometryMapping();

    static final String value1 = "val1";
    static final String value2 = "val2";
    static final String coordinates = "10 50, 11 51";


    static {
        featureMapping.setName("f1");
        propertyMapping.setName("p1");
        propertyMapping2.setName("p2");
        geometryMapping.setGeometryType(GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.MULTI_POLYGON);
    }

    {

        describe("GeoJson writer geometry middleware", () -> {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            beforeEach(outputStream::reset);

            context("if coordinates are given", () -> {

                it("it should write a valid GeoJSON geometry", () -> {

                    writeFeature(outputStream, ImmutableList.of(2, 1, 2, 2, 1, 1));

                    String expected = "{\n" +
                            "  \"geometry\" : {\n" +
                            "    \"type\" : \"MultiPolygon\",\n" +
                            "    \"coordinates\" : [ [ [ [ 10, 50 ], [ 11, 51 ] ], [ [ 10, 50 ], [ 11, 51 ] ] ], [ [ [ 10, 50 ], [ 11, 51 ] ] ], [ [ [ 10, 50 ], [ 11, 51 ] ], [ [ 10, 50 ], [ 11, 51 ] ], [ [ 10, 50 ], [ 11, 51 ] ] ] ]\n" +
                            "  }\n" +
                            "}";
                    String actual = GeoJsonWriterSetupUtil.asString(outputStream);

                    assertEquals(actual, expected);
                });

            });

            context("if no coordinates are given", () -> {

                it("it should write a null GeoJSON geometry", () -> {

                    writeFeature(outputStream, ImmutableList.of());

                    String expected = "{\n" +
                            "  \"geometry\" : null\n" +
                            "}";
                    String actual = GeoJsonWriterSetupUtil.asString(outputStream);

                    assertEquals(actual, expected);
                });

            });

            context("test buffering", () -> {

            });

        });

    }

    private void writeFeature(ByteArrayOutputStream outputStream, List<Integer> nestingPattern) throws IOException, URISyntaxException {
        FeatureTransformationContextGeoJson transformationContext = GeoJsonWriterSetupUtil.createTransformationContext(outputStream, false);
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(transformationContext, ImmutableList.of(new GeoJsonWriterGeometry()));

        transformationContext.getJson()
                             .writeStartObject();

        transformer.onFeatureStart(featureMapping);
        transformer.onPropertyStart(propertyMapping, ImmutableList.of());
        transformer.onPropertyText(value1);
        transformer.onPropertyEnd();
        transformer.onGeometryStart(geometryMapping, SimpleFeatureGeometry.MULTI_POLYGON, null);

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
