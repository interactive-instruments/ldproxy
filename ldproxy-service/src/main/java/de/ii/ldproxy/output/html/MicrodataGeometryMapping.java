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
package de.ii.ldproxy.output.html;

import de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;

/**
 * @author zahnen
 */
public class MicrodataGeometryMapping extends MicrodataPropertyMapping {

    private MICRODATA_GEOMETRY_TYPE geometryType;

    public MICRODATA_GEOMETRY_TYPE getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(MICRODATA_GEOMETRY_TYPE geometryType) {
        this.geometryType = geometryType;
    }


    public enum MICRODATA_GEOMETRY_TYPE {

        POINT(GML_GEOMETRY_TYPE.POINT, GML_GEOMETRY_TYPE.MULTI_POINT),
        LINE_STRING(GML_GEOMETRY_TYPE.LINE_STRING, GML_GEOMETRY_TYPE.CURVE, GML_GEOMETRY_TYPE.MULTI_LINESTRING, GML_GEOMETRY_TYPE.MULTI_CURVE),
        POLYGON(GML_GEOMETRY_TYPE.POLYGON, GML_GEOMETRY_TYPE.SURFACE, GML_GEOMETRY_TYPE.MULTI_POLYGON, GML_GEOMETRY_TYPE.MULTI_SURFACE),
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

}
