/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformationContext
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformerGeoJson
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter
import de.ii.xtraplatform.features.domain.FeatureProperty
import de.ii.xtraplatform.features.domain.FeatureType
import de.ii.xtraplatform.features.domain.ImmutableFeatureProperty
import de.ii.xtraplatform.features.domain.ImmutableFeatureType
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import java.util.function.Consumer

import static de.ii.ldproxy.ogcapi.features.geojson.app.GeoJsonWriterSetupUtil.createTransformationContext

class FeatureTransformerGeoJsonSpec extends Specification {

    @Shared FeatureType featureMapping
    @Shared FeatureProperty propertyMapping
    @Shared FeatureProperty propertyMapping2
    @Shared FeatureProperty geometryMapping
    @Shared String value1
    @Shared String value2
    @Shared String coordinates

    def setupSpec() {
        featureMapping = new ImmutableFeatureType.Builder().name("f1")
                .properties(ImmutableMap.of())
                .build()
        propertyMapping = new ImmutableFeatureProperty.Builder().name("p1")
                .path("")
                .build()
        propertyMapping2 = new ImmutableFeatureProperty.Builder().name("p2")
                .path("")
                .build()
        geometryMapping = new ImmutableFeatureProperty.Builder().name("geometry")
                .path("")
                .type(FeatureProperty.Type.GEOMETRY)
                .build()
        value1 = "val1"
        value2 = "val2"
        coordinates = "10 50, 11 51"
    }

    /**
     * Is FeatureWriter.onEvent called with the correct state?
     */
    def 'Test pipeline state'() {
        given:
        List<FeatureTransformationContext.Event> actualEvents = new ArrayList<>()
        List<FeatureTransformationContext.Event> expectedEvents = ImmutableList.of(FeatureTransformationContext.Event.START,
                FeatureTransformationContext.Event.FEATURE_START, FeatureTransformationContext.Event.PROPERTY,
                FeatureTransformationContext.Event.COORDINATES, FeatureTransformationContext.Event.GEOMETRY_END,
                FeatureTransformationContext.Event.PROPERTY, FeatureTransformationContext.Event.FEATURE_END,
                FeatureTransformationContext.Event.END)
        List<Object> actualMappings = new ArrayList<>()
        List<Object> expectedMappings = ImmutableList.of(featureMapping, propertyMapping, geometryMapping, propertyMapping2)
        List<String> actualValues = new ArrayList<>()
        List<String> expectedValues = ImmutableList.of(value1, coordinates, value2)
        FeatureTransformationContextGeoJson context = createTransformationContext(new ByteArrayOutputStream(), false)
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(context, ImmutableList.of(new GeoJsonWriter() {

            @Override
            GeoJsonWriter create() {
                return this
            }

            @Override
            int getSortPriority() {
                return 0
            }

            @Override
            void onEvent(FeatureTransformationContextGeoJson transformationContext,
                         Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
                actualEvents.add(transformationContext.getState().getEvent())

                switch (transformationContext.getState().getEvent()) {
                    case FeatureTransformationContext.Event.PROPERTY:
                    case FeatureTransformationContext.Event.COORDINATES:
                        transformationContext.getState()
                                .getCurrentFeatureProperty()
                                .ifPresent{it -> actualMappings.add(it)}
                        transformationContext.getState()
                                .getCurrentValue()
                                .ifPresent{it -> actualValues.add(it)}
                    case FeatureTransformationContext.Event.FEATURE_START:
                        transformationContext.getState()
                                .getCurrentFeatureType()
                                .ifPresent{it -> actualMappings.add(it)}
                }
            }
        }))
        when:
        writeFeature(transformer, true)
        then:
        actualEvents.equals(expectedEvents)
        actualMappings.equals(expectedMappings)
        actualValues.equals(expectedValues)
    }

    @Ignore
    def 'Test pipeline chaining'() {
        // TODO: Is every FeatureWriter in the pipeline called in the correct order?
    }

    /**
     * Is currentGeometryNestingChange set correctly?
     */
    def 'Test geometry nesting'() {
        given:
        List<Integer> expectedNestingChanges = ImmutableList.of(2, 1, 2, 2, 1, 1)
        List<Integer> actualNestingChanges = new ArrayList<>()
        FeatureTransformationContextGeoJson context = createTransformationContext(new ByteArrayOutputStream(), true)
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(context, ImmutableList.of(new GeoJsonWriter() {
            @Override
            GeoJsonWriter create() {
                return this
            }

            @Override
            int getSortPriority() {
                return 0
            }

            @Override
            void onEvent(FeatureTransformationContextGeoJson transformationContext,
                                Consumer<FeatureTransformationContextGeoJson> next) throws IOException {


                switch (transformationContext.getState()
                        .getEvent()) {
                    case FeatureTransformationContext.Event.COORDINATES:
                        actualNestingChanges.add(transformationContext.getState()
                                .getCurrentGeometryNestingChange())
                }
            }
        }))
        when:
        writeFeature(transformer, true, expectedNestingChanges)
        then:
        actualNestingChanges.equals(expectedNestingChanges)
    }

    private void writeFeature(FeatureTransformerGeoJson transformer, boolean startEnd,
                              List<Integer> nestingPattern) throws IOException {
        if (startEnd) {
            transformer.onStart(OptionalLong.empty(), OptionalLong.empty())
        }
        transformer.onFeatureStart(featureMapping)
        transformer.onPropertyStart(propertyMapping, ImmutableList.of())
        transformer.onPropertyText(value1)
        transformer.onPropertyEnd()
        transformer.onGeometryStart(geometryMapping, SimpleFeatureGeometry.MULTI_POLYGON, null)

        for (Integer depth : nestingPattern) {
            for (int i = 0; i < depth; i++) {
                transformer.onGeometryNestedStart()
            }
            transformer.onGeometryCoordinates(coordinates)
            for (int i = 0; i < depth; i++) {
                transformer.onGeometryNestedEnd()
            }
        }

        transformer.onGeometryEnd()
        transformer.onPropertyStart(propertyMapping2, ImmutableList.of())
        transformer.onPropertyText(value2)
        transformer.onPropertyEnd()
        transformer.onFeatureEnd()
        if (startEnd) {
            transformer.onEnd()
        }
    }

    private void writeFeature(FeatureTransformerGeoJson transformer, boolean startEnd) throws IOException {
        writeFeature(transformer, startEnd, ImmutableList.of(1))
    }

}
