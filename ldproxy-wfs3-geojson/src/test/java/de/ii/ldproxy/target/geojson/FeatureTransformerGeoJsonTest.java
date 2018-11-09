/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.api.FeatureTransformationContext.Event;
import de.ii.ldproxy.wfs3.api.FeatureWriterGeoJson;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3ServiceData;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.provider.wfs.ConnectionInfo;
import de.ii.xtraplatform.feature.provider.wfs.ImmutableConnectionInfo;
import de.ii.xtraplatform.feature.provider.wfs.ImmutableFeatureProviderDataWfs;
import de.ii.xtraplatform.feature.query.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.Consumer;

import static de.ii.ldproxy.target.geojson.GeoJsonWriterSetupUtil.createTransformationContext;
import static org.testng.Assert.assertEquals;

/**
 * @author zahnen
 */
public class FeatureTransformerGeoJsonTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerGeoJsonTest.class);

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

    /**
     * Is FeatureWriter.onEvent called with the correct state?
     */
    @Test(groups = {"default"})
    public void testPipelineState() throws Exception {
        List<Event> actualEvents = new ArrayList<>();

        List<Event> expectedEvents = ImmutableList.of(Event.START, Event.FEATURE_START, Event.PROPERTY, Event.COORDINATES, Event.GEOMETRY_END, Event.PROPERTY, Event.FEATURE_END, Event.END);

        List<TargetMapping> actualMappings = new ArrayList<>();

        List<TargetMapping> expectedMappings = ImmutableList.of(featureMapping, propertyMapping, geometryMapping, propertyMapping2);

        List<String> actualValues = new ArrayList<>();

        List<String> expectedValues = ImmutableList.of(value1, coordinates, value2);

        FeatureTransformationContextGeoJson transformationContext = createTransformationContext(new ByteArrayOutputStream(), false);
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(transformationContext, ImmutableList.of(new FeatureWriterGeoJson<FeatureTransformationContextGeoJson>() {
            @Override
            public int getSortPriority() {
                return 0;
            }

            @Override
            public void onEvent(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
                actualEvents.add(transformationContext.getState()
                                                      .getEvent());

                switch (transformationContext.getState()
                                             .getEvent()) {
                    case PROPERTY:
                    case COORDINATES:
                        transformationContext.getState()
                                             .getCurrentValue()
                                             .ifPresent(actualValues::add);
                    case FEATURE_START:
                        transformationContext.getState()
                                             .getCurrentMapping()
                                             .ifPresent(actualMappings::add);
                }
            }
        }));

        writeFeature(transformer, true);

        assertEquals(actualEvents, expectedEvents);

        assertEquals(actualMappings, expectedMappings);

        assertEquals(actualValues, expectedValues);
    }

    /**
     * Is every FeatureWriter in the pipeline called in the correct order?
     */
    @Test(groups = {"default"})
    public void testPipelineChaining() throws Exception {
        //is every writer called in the correct order
    }

    /**
     * Is currentGeometryNestingChange set correctly?
     */
    @Test(groups = {"default"})
    public void testGeometryNesting() throws Exception {

        List<Integer> expectedNestingChanges = ImmutableList.of(2, 1, 2, 2, 1, 1);
        List<Integer> actualNestingChanges = new ArrayList<>();

        FeatureTransformationContextGeoJson transformationContext = createTransformationContext(new ByteArrayOutputStream(), true);
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(transformationContext, ImmutableList.of(new FeatureWriterGeoJson<FeatureTransformationContextGeoJson>() {
            @Override
            public int getSortPriority() {
                return 0;
            }

            @Override
            public void onEvent(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {


                switch (transformationContext.getState()
                                             .getEvent()) {
                    case COORDINATES:
                        actualNestingChanges.add(transformationContext.getState()
                                                                      .getCurrentGeometryNestingChange());
                }
            }
        }));

        writeFeature(transformer, true, expectedNestingChanges);


        assertEquals(actualNestingChanges, expectedNestingChanges);
    }

    private void writeFeature(FeatureTransformerGeoJson transformer, boolean startEnd, List<Integer> nestingPattern) throws IOException {
        if (startEnd) {
            transformer.onStart(OptionalLong.empty(), OptionalLong.empty());
        }
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
        if (startEnd) {
            transformer.onEnd();
        }
    }

    private void writeFeature(FeatureTransformerGeoJson transformer, boolean startEnd) throws IOException {
        writeFeature(transformer, startEnd, ImmutableList.of(1));
    }
}