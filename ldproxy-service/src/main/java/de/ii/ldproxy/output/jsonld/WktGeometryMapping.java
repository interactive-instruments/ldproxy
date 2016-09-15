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
