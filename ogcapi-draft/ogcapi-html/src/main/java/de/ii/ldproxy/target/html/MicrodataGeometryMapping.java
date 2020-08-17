/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import de.ii.ldproxy.ogcapi.collections.domain.OgcApiFeaturesGenericMapping;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.features.domain.legacy.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.SimpleFeatureGeometryFrom;

/**
 * @author zahnen
 */
@Deprecated
public class MicrodataGeometryMapping extends MicrodataPropertyMapping {

    public enum MICRODATA_GEOMETRY_TYPE implements SimpleFeatureGeometryFrom {

        POINT(SimpleFeatureGeometry.POINT, SimpleFeatureGeometry.MULTI_POINT),
        LINE_STRING(SimpleFeatureGeometry.LINE_STRING, SimpleFeatureGeometry.MULTI_LINE_STRING),
        POLYGON(SimpleFeatureGeometry.POLYGON, SimpleFeatureGeometry.MULTI_POLYGON),
        GENERIC(SimpleFeatureGeometry.ANY),
        NONE();

        private SimpleFeatureGeometry[] gmlTypes;

        MICRODATA_GEOMETRY_TYPE(SimpleFeatureGeometry... gmlType) {
            this.gmlTypes = gmlType;
        }

        public static MICRODATA_GEOMETRY_TYPE forGmlType(SimpleFeatureGeometry gmlType) {
            for (MICRODATA_GEOMETRY_TYPE geoJsonType : MICRODATA_GEOMETRY_TYPE.values()) {
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
                case LINE_STRING:
                    simpleFeatureGeometry = SimpleFeatureGeometry.LINE_STRING;
                    break;
                case POLYGON:
                    simpleFeatureGeometry = SimpleFeatureGeometry.POLYGON;
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

    private MICRODATA_GEOMETRY_TYPE geometryType;
    private static final String PROPERTY_NAME = "geo";

    public MicrodataGeometryMapping() {
    }

    public MicrodataGeometryMapping(MicrodataGeometryMapping mapping) {
        super(mapping);
        this.geometryType = mapping.geometryType;
    }

    public MICRODATA_GEOMETRY_TYPE getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(MICRODATA_GEOMETRY_TYPE geometryType) {
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
        super.mergeCopyWithBase(targetMapping);

        MicrodataGeometryMapping copy = new MicrodataGeometryMapping(this);
        OgcApiFeaturesGenericMapping baseMapping = (OgcApiFeaturesGenericMapping) targetMapping;

        if (!baseMapping.isEnabled()) {
            copy.enabled = baseMapping.isEnabled();
        }
        if (copy.name == null && baseMapping.getName() != null) {
            copy.name = baseMapping.getName();
        }

        return copy;
    }

}
