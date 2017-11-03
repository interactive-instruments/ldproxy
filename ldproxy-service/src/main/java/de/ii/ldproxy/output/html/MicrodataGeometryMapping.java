/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.html;

import de.ii.ldproxy.output.generic.GenericMapping;
import de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
import de.ii.ogc.wfs.proxy.TargetMapping;

/**
 * @author zahnen
 */
public class MicrodataGeometryMapping extends MicrodataPropertyMapping {

    public enum MICRODATA_GEOMETRY_TYPE {

        POINT(GML_GEOMETRY_TYPE.POINT, GML_GEOMETRY_TYPE.MULTI_POINT),
        LINE_STRING(GML_GEOMETRY_TYPE.LINE_STRING, GML_GEOMETRY_TYPE.CURVE, GML_GEOMETRY_TYPE.MULTI_LINESTRING, GML_GEOMETRY_TYPE.MULTI_CURVE),
        POLYGON(GML_GEOMETRY_TYPE.POLYGON, GML_GEOMETRY_TYPE.SURFACE, GML_GEOMETRY_TYPE.MULTI_POLYGON, GML_GEOMETRY_TYPE.MULTI_SURFACE),
        GENERIC(GML_GEOMETRY_TYPE.GEOMETRY, GML_GEOMETRY_TYPE.ABSTRACT_GEOMETRY),
        NONE();

        private GML_GEOMETRY_TYPE[] gmlTypes;

        MICRODATA_GEOMETRY_TYPE(GML_GEOMETRY_TYPE... gmlType) {
            this.gmlTypes = gmlType;
        }

        public static MICRODATA_GEOMETRY_TYPE forGmlType(GML_GEOMETRY_TYPE gmlType) {
            for (MICRODATA_GEOMETRY_TYPE geoJsonType : MICRODATA_GEOMETRY_TYPE.values()) {
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
        MicrodataGeometryMapping copy = new MicrodataGeometryMapping(this);
        GenericMapping baseMapping = (GenericMapping) targetMapping;

        if (!baseMapping.isEnabled()) {
            copy.enabled = baseMapping.isEnabled();
        }
        if (copy.name == null && baseMapping.getName() != null) {
            copy.name = baseMapping.getName();
        }

        return copy;
    }

}
