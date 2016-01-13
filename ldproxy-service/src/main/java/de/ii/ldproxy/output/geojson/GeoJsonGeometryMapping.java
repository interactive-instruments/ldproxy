/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.ldproxy.output.geojson;

import static de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;

/**
 * @author zahnen
 */
public class GeoJsonGeometryMapping extends GeoJsonPropertyMapping {

    public enum GEO_JSON_GEOMETRY_TYPE {

        POINT("Point", GML_GEOMETRY_TYPE.POINT),
        MULTI_POINT("MultiPoint", GML_GEOMETRY_TYPE.MULTI_POINT),
        LINE_STRING("LineString", GML_GEOMETRY_TYPE.LINE_STRING, GML_GEOMETRY_TYPE.CURVE),
        MULTI_LINE_STRING("MultiLineString", GML_GEOMETRY_TYPE.MULTI_LINESTRING, GML_GEOMETRY_TYPE.MULTI_CURVE),
        POLYGON("Polygon", GML_GEOMETRY_TYPE.POLYGON, GML_GEOMETRY_TYPE.SURFACE),
        MULTI_POLYGON("MultiPolygon", GML_GEOMETRY_TYPE.MULTI_POLYGON, GML_GEOMETRY_TYPE.MULTI_SURFACE),
        GEOMETRY_COLLECTION("GeometryCollection"),
        GENERIC("", GML_GEOMETRY_TYPE.GEOMETRY, GML_GEOMETRY_TYPE.ABSTRACT_GEOMETRY),
        NONE("");

        private String stringRepresentation;
        private GML_GEOMETRY_TYPE[] gmlTypes;

        GEO_JSON_GEOMETRY_TYPE(String stringRepresentation, GML_GEOMETRY_TYPE... gmlType) {
            this.stringRepresentation = stringRepresentation;
            this.gmlTypes = gmlType;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public static GEO_JSON_GEOMETRY_TYPE forGmlType(GML_GEOMETRY_TYPE gmlType) {
            for (GEO_JSON_GEOMETRY_TYPE geoJsonType : GEO_JSON_GEOMETRY_TYPE.values()) {
                for (GML_GEOMETRY_TYPE v2: geoJsonType.gmlTypes) {
                    if (v2 == gmlType) {
                        return geoJsonType;
                    }
                }
            }

            return NONE;
        }

        public boolean isValid() {
            return this != NONE;
        }
    }

    private static final String PROPERTY_NAME = "geometry";
    private GEO_JSON_GEOMETRY_TYPE geometryType;

    public GEO_JSON_GEOMETRY_TYPE getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(GEO_JSON_GEOMETRY_TYPE geometryType) {
        this.geometryType = geometryType;
    }

    @Override
    public String getName() {
        return PROPERTY_NAME;
    }

    @Override
    public void setName(String name) {
    }
}
