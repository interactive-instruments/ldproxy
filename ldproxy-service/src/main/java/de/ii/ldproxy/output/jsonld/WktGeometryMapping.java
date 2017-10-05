/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.jsonld;

import de.ii.ldproxy.output.html.MicrodataPropertyMapping;
import de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;

/**
 * @author zahnen
 */
public class WktGeometryMapping extends MicrodataPropertyMapping {

    private WKT_GEOMETRY_TYPE geometryType;

    public WKT_GEOMETRY_TYPE getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(WKT_GEOMETRY_TYPE geometryType) {
        this.geometryType = geometryType;
    }

    public enum WKT_GEOMETRY_TYPE {

        POINT(GML_GEOMETRY_TYPE.POINT),
        LINESTRING(GML_GEOMETRY_TYPE.LINE_STRING, GML_GEOMETRY_TYPE.CURVE),
        POLYGON(GML_GEOMETRY_TYPE.POLYGON, GML_GEOMETRY_TYPE.SURFACE),
        MULTIPOINT(GML_GEOMETRY_TYPE.POINT),
        MULTILINESTRING(GML_GEOMETRY_TYPE.MULTI_LINESTRING, GML_GEOMETRY_TYPE.MULTI_CURVE),
        MULTIPOLYGON(GML_GEOMETRY_TYPE.MULTI_POLYGON, GML_GEOMETRY_TYPE.MULTI_SURFACE),
        GENERIC(GML_GEOMETRY_TYPE.ABSTRACT_GEOMETRY, GML_GEOMETRY_TYPE.GEOMETRY),
        NONE();

        private GML_GEOMETRY_TYPE[] gmlTypes;

        WKT_GEOMETRY_TYPE(GML_GEOMETRY_TYPE... gmlType) {
            this.gmlTypes = gmlType;
        }

        public static WKT_GEOMETRY_TYPE forGmlType(GML_GEOMETRY_TYPE gmlType) {
            for (WKT_GEOMETRY_TYPE wktType : WKT_GEOMETRY_TYPE.values()) {
                for (GML_GEOMETRY_TYPE v2: wktType.gmlTypes) {
                    if (v2 == gmlType) {
                        return wktType;
                    }
                }
            }

            return NONE;
        }

        public boolean isValid() {
            return this != NONE;
        }
    }
}
