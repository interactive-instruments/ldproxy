/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.geojson;

import de.ii.ldproxy.output.generic.GenericMapping;
import de.ii.ogc.wfs.proxy.TargetMapping;

import static de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;

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

    GeoJsonGeometryMapping() {
    }

    GeoJsonGeometryMapping(GeoJsonGeometryMapping mapping) {
        super(mapping);
        this.geometryType = mapping.geometryType;
    }

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

    @Override
    public TargetMapping mergeCopyWithBase(TargetMapping targetMapping) {
        GeoJsonGeometryMapping copy = new GeoJsonGeometryMapping(this);
        GenericMapping baseMapping = (GenericMapping) targetMapping;

        if (!baseMapping.isEnabled()) {
            copy.enabled = baseMapping.isEnabled();
        }

        return copy;
    }
}
