/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.ii.ldproxy.ogcapi.collections.domain.legacy.OgcApiFeaturesGenericMapping;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.features.domain.legacy.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.SimpleFeatureGeometryFrom;

/**
 * @author zahnen
 */
@Deprecated
public class GeoJsonGeometryMapping extends GeoJsonPropertyMapping {

    public enum GEO_JSON_GEOMETRY_TYPE implements SimpleFeatureGeometryFrom {

        POINT("Point", SimpleFeatureGeometry.POINT),
        MULTI_POINT("MultiPoint", SimpleFeatureGeometry.MULTI_POINT),
        LINE_STRING("LineString", SimpleFeatureGeometry.LINE_STRING),
        MULTI_LINE_STRING("MultiLineString", SimpleFeatureGeometry.MULTI_LINE_STRING),
        POLYGON("Polygon", SimpleFeatureGeometry.POLYGON),
        MULTI_POLYGON("MultiPolygon", SimpleFeatureGeometry.MULTI_POLYGON),
        GEOMETRY_COLLECTION("GeometryCollection"),
        GENERIC("Generic", SimpleFeatureGeometry.ANY),
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

        @Override
        public SimpleFeatureGeometry toSimpleFeatureGeometry() {
            SimpleFeatureGeometry simpleFeatureGeometry = SimpleFeatureGeometry.NONE;

            switch (this) {

                case POINT:
                    simpleFeatureGeometry = SimpleFeatureGeometry.POINT;
                    break;
                case MULTI_POINT:
                    simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_POINT;
                    break;
                case LINE_STRING:
                    simpleFeatureGeometry = SimpleFeatureGeometry.LINE_STRING;
                    break;
                case MULTI_LINE_STRING:
                    simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_LINE_STRING;
                    break;
                case POLYGON:
                    simpleFeatureGeometry = SimpleFeatureGeometry.POLYGON;
                    break;
                case MULTI_POLYGON:
                    simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_POLYGON;
                    break;
                case GEOMETRY_COLLECTION:
                    simpleFeatureGeometry = SimpleFeatureGeometry.GEOMETRY_COLLECTION;
                    break;
                case GENERIC:
                    simpleFeatureGeometry = SimpleFeatureGeometry.ANY;
                    break;
                case NONE:
                    break;
            }

            return simpleFeatureGeometry;
        }

        public boolean isValid() {
            return this != NONE;
        }
    }

    private static final String PROPERTY_NAME = "geometry";
    private GEO_JSON_GEOMETRY_TYPE geometryType;
    private Boolean mustReversePolygon;

    public GeoJsonGeometryMapping() {
    }

    GeoJsonGeometryMapping(GeoJsonGeometryMapping mapping) {
        super(mapping);
        this.geometryType = mapping.geometryType;
        this.mustReversePolygon = mapping.mustReversePolygon;
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

    @JsonProperty(value = "mustReversePolygon")
    public Boolean getMustReversePolygon() {
        return mustReversePolygon;
    }

    public void setMustReversePolygon(Boolean mustReversePolygon) {
        this.mustReversePolygon = mustReversePolygon;
    }

    @Override
    public TargetMapping mergeCopyWithBase(TargetMapping targetMapping) {
        super.mergeCopyWithBase(targetMapping);

        GeoJsonGeometryMapping copy = new GeoJsonGeometryMapping(this);
        OgcApiFeaturesGenericMapping baseMapping = (OgcApiFeaturesGenericMapping) targetMapping;

        if (!baseMapping.isEnabled()) {
            copy.enabled = baseMapping.isEnabled();
        }

        return copy;
    }
}
