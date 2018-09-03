/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import de.ii.ldproxy.wfs3.api.Wfs3GenericMapping;
import de.ii.xtraplatform.feature.query.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.query.api.TargetMapping;

/**
 * @author zahnen
 */
public class GeoJsonGeometryMapping extends GeoJsonPropertyMapping {

    public enum GEO_JSON_GEOMETRY_TYPE {

        POINT("Point", SimpleFeatureGeometry.POINT),
        MULTI_POINT("MultiPoint", SimpleFeatureGeometry.MULTI_POINT),
        LINE_STRING("LineString", SimpleFeatureGeometry.LINE_STRING),
        MULTI_LINE_STRING("MultiLineString", SimpleFeatureGeometry.MULTI_LINE_STRING),
        POLYGON("Polygon", SimpleFeatureGeometry.POLYGON),
        MULTI_POLYGON("MultiPolygon", SimpleFeatureGeometry.MULTI_POLYGON),
        GEOMETRY_COLLECTION("GeometryCollection"),
        GENERIC(""),
        NONE("");

        private String stringRepresentation;
        private SimpleFeatureGeometry[] gmlTypes;

        GEO_JSON_GEOMETRY_TYPE(String stringRepresentation, SimpleFeatureGeometry... gmlType) {
            this.stringRepresentation = stringRepresentation;
            this.gmlTypes = gmlType;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public static GEO_JSON_GEOMETRY_TYPE forGmlType(SimpleFeatureGeometry gmlType) {
            for (GEO_JSON_GEOMETRY_TYPE geoJsonType : GEO_JSON_GEOMETRY_TYPE.values()) {
                for (SimpleFeatureGeometry v2: geoJsonType.gmlTypes) {
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

    public GeoJsonGeometryMapping() {
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
        Wfs3GenericMapping baseMapping = (Wfs3GenericMapping) targetMapping;

        if (!baseMapping.isEnabled()) {
            copy.enabled = baseMapping.isEnabled();
        }

        return copy;
    }
}
