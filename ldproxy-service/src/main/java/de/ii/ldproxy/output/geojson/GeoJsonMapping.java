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

import de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer;
import de.ii.ogc.wfs.proxy.TargetMapping;

/**
 * @author zahnen
 */
public interface GeoJsonMapping extends TargetMapping {
    GEO_JSON_TYPE getType();

    enum GEO_JSON_TYPE {

        ID(AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE.ID),
        STRING(AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE.STRING, AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE.DATE, AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE.DATE_TIME),
        NUMBER(AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE.INT, AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE.INTEGER, AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE.DECIMAL, AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE.DOUBLE),
        GEOMETRY(),
        NONE(AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE.NONE);

        private AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE[] gmlTypes;

        GEO_JSON_TYPE(AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE... gmlType) {
            this.gmlTypes = gmlType;
        }

        public static GEO_JSON_TYPE forGmlType(AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE gmlType) {
            for (GEO_JSON_TYPE geoJsonType : GEO_JSON_TYPE.values()) {
                for (AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE v2: geoJsonType.gmlTypes) {
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
