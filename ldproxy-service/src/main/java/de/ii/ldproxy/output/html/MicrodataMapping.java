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

import de.ii.ogc.wfs.proxy.TargetMapping;

import static de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer.GML_TYPE;

/**
 * @author zahnen
 */
public interface MicrodataMapping extends TargetMapping {
    MICRODATA_TYPE getType();
    boolean isShowInCollection();
    String getItemType();
    String getItemProp();
    String getSparqlQuery();

    enum MICRODATA_TYPE {

        ID(GML_TYPE.ID),
        STRING(GML_TYPE.STRING, GML_TYPE.DATE, GML_TYPE.DATE_TIME),
        NUMBER(GML_TYPE.INT, GML_TYPE.INTEGER, GML_TYPE.DECIMAL, GML_TYPE.DOUBLE),
        GEOMETRY(),
        NONE(GML_TYPE.NONE);

        private GML_TYPE[] gmlTypes;

        MICRODATA_TYPE(GML_TYPE... gmlType) {
            this.gmlTypes = gmlType;
        }

        public static MICRODATA_TYPE forGmlType(GML_TYPE gmlType) {
            for (MICRODATA_TYPE geoJsonType : MICRODATA_TYPE.values()) {
                for (GML_TYPE v2: geoJsonType.gmlTypes) {
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
