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
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.OptionalLong;

import static com.greghaskins.spectrum.dsl.specification.Specification.beforeEach;
import static com.greghaskins.spectrum.dsl.specification.Specification.context;
import static com.greghaskins.spectrum.dsl.specification.Specification.describe;
import static com.greghaskins.spectrum.dsl.specification.Specification.it;
import static de.ii.ldproxy.target.geojson.GeoJsonWriterSetupUtil.createTransformationContext;
import static org.testng.Assert.assertEquals;

/**
 * @author zahnen
 */
@RunWith(Spectrum.class)
public class GeoJsonWriterSkeletonSpec {

    static final GeoJsonPropertyMapping featureMapping = new GeoJsonPropertyMapping();


    static {
        featureMapping.setName("f1");
    }

    {

        describe("GeoJsonWriterSkeleton middleware", () -> {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            beforeEach(outputStream::reset);

            context("given a feature collection", () -> {

                it("it should write a GeoJSON FeatureCollection", () -> {

                    writeFeature(outputStream, true);

                    String expected = "{" + System.lineSeparator() +
                            "  \"type\" : \"FeatureCollection\"," + System.lineSeparator() +
                            "  \"features\" : [ {" + System.lineSeparator() +
                            "    \"type\" : \"Feature\"" + System.lineSeparator() +
                            "  } ]" + System.lineSeparator() +
                            "}";
                    String actual = GeoJsonWriterSetupUtil.asString(outputStream);

                    assertEquals(actual, expected);

                });

            });

            context("given a single feature", () -> {

                it("it should write a GeoJSON Feature", () -> {

                    writeFeature(outputStream, false);

                    String expected = "{" + System.lineSeparator() +
                            "  \"type\" : \"Feature\"" + System.lineSeparator() +
                            "}";
                    String actual = GeoJsonWriterSetupUtil.asString(outputStream);

                    assertEquals(actual, expected);

                });

            });

        });

    }

    private void writeFeature(ByteArrayOutputStream outputStream, boolean isCollection) throws IOException, URISyntaxException {
        FeatureTransformationContextGeoJson transformationContext = createTransformationContext(outputStream, isCollection);
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(transformationContext, ImmutableList.of(new GeoJsonWriterSkeleton()));

        transformer.onStart(OptionalLong.empty(), OptionalLong.empty());
        transformer.onFeatureStart(featureMapping);
        transformer.onFeatureEnd();
        transformer.onEnd();
    }
}
