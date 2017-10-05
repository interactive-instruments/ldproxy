/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
