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
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformerGeoJson
import de.ii.xtraplatform.features.domain.FeatureProperty
import de.ii.xtraplatform.features.domain.FeatureType
import de.ii.xtraplatform.features.domain.ImmutableFeatureProperty
import de.ii.xtraplatform.features.domain.ImmutableFeatureType
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry
import spock.lang.Shared
import spock.lang.Specification

class GeoJsonWriterGeometrySpec extends Specification {

    @Shared FeatureType featureMapping = new ImmutableFeatureType.Builder().name("f1")
            .properties(ImmutableMap.of())
            .build()

    @Shared FeatureProperty propertyMapping = new ImmutableFeatureProperty.Builder().name("p1")
            .path("")
            .build()

    @Shared FeatureProperty propertyMapping2 = new ImmutableFeatureProperty.Builder().name("p2")
            .path("")
            .build()

    @Shared FeatureProperty geometryMapping = new ImmutableFeatureProperty.Builder().name("geometry")
            .path("")
            .type(FeatureProperty.Type.GEOMETRY)
            .build()

    @Shared String value1 = "val1"
    @Shared String value2 = "val2"
    @Shared String coordinates = "10 50, 11 51"

    def "GeoJson writer geometry middleware, coordinates are given"() {
        given:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        String expected = "{" + System.lineSeparator() +
                "  \"geometry\" : {" + System.lineSeparator() +
                "    \"type\" : \"MultiPolygon\"," + System.lineSeparator() +
                "    \"coordinates\" : [ [ [ [ 10, 50 ], [ 11, 51 ] ], [ [ 10, 50 ], [ 11, 51 ] ] ], [ [ [ 10, 50 ], [ 11, 51 ] ] ], [ [ [ 10, 50 ], [ 11, 51 ] ], [ [ 10, 50 ], [ 11, 51 ] ], [ [ 10, 50 ], [ 11, 51 ] ] ] ]" + System.lineSeparator() +
                "  }" + System.lineSeparator() +
                "}"
        when:
        writeFeature(outputStream, ImmutableList.of(2, 1, 2, 2, 1, 1))
        String actual = GeoJsonWriterSetupUtil.asString(outputStream)

        then:
        actual == expected
    }


    def "GeoJson writer geometry middleware, no coordinates are given"() {
        given:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        String expected = "{" + System.lineSeparator() +
                "  \"geometry\" : null" + System.lineSeparator() +
                "}"
        when:
        writeFeature(outputStream, ImmutableList.of())
        String actual = GeoJsonWriterSetupUtil.asString(outputStream)

        then:
        actual == expected
    }

    private void writeFeature(ByteArrayOutputStream outputStream,
                              List<Integer> nestingPattern) throws IOException, URISyntaxException {
        FeatureTransformationContextGeoJson transformationContext = GeoJsonWriterSetupUtil.createTransformationContext(outputStream, false)
        FeatureTransformerGeoJson transformer = new FeatureTransformerGeoJson(transformationContext, ImmutableList.of(new GeoJsonWriterGeometry()))

        transformationContext.getJson()
                .writeStartObject()

        transformer.onFeatureStart(featureMapping)
        transformer.onPropertyStart(propertyMapping, ImmutableList.of())
        transformer.onPropertyText(value1)
        transformer.onPropertyEnd()
        transformer.onGeometryStart(geometryMapping, SimpleFeatureGeometry.MULTI_POLYGON, 2)

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

        transformationContext.getJson()
                .writeEndObject()

        transformer.onEnd()
    }

}
