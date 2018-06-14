/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.geojson;

import de.ii.ogc.wfs.proxy.WfsProxyOnTheFlyMapping;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.GmlFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
import de.ii.xtraplatform.util.xml.XMLPathTracker;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zahnen
 */
public class GeoJsonOnTheFlyMapping implements WfsProxyOnTheFlyMapping {
    List<String> paths;

    GeoJsonOnTheFlyMapping() {
        this.paths = new ArrayList<>();
    }

    @Override
    public TargetMapping getTargetMappingForFeatureType(XMLPathTracker path, String nsuri, String localName) {
        return null;
    }

    @Override
    public TargetMapping getTargetMappingForAttribute(XMLPathTracker path, String nsuri, String localName, String value) {
        if (!path.toFieldName().contains(".") && (localName.equals("id")  || localName.equals("fid"))) {

                GeoJsonPropertyMapping targetMapping = new GeoJsonPropertyMapping();
                targetMapping.setEnabled(true);
                targetMapping.setName("id");
                targetMapping.setType(GeoJsonMapping.GEO_JSON_TYPE.ID);

                return targetMapping;
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForProperty(XMLPathTracker path, String nsuri, String localName, String value) {

        // TODO: parse value to detect type
        GeoJsonMapping.GEO_JSON_TYPE dataType = GeoJsonMapping.GEO_JSON_TYPE.STRING;

        if (dataType.isValid() && !hasPath(path.toFieldNameGml())) {

            GeoJsonPropertyMapping targetMapping = new GeoJsonPropertyMapping();
            targetMapping.setEnabled(true);
            targetMapping.setName(path.toFieldNameGml());
            targetMapping.setType(dataType);


            return targetMapping;
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForGeometry(XMLPathTracker path, String nsuri, String localName) {
        GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE geoType = GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.forGmlType(GML_GEOMETRY_TYPE.fromString(localName));

        if (geoType.isValid()) {

            GeoJsonGeometryMapping targetMapping = new GeoJsonGeometryMapping();
            targetMapping.setEnabled(true);
            targetMapping.setType(GeoJsonMapping.GEO_JSON_TYPE.GEOMETRY);
            targetMapping.setGeometryType(geoType);

            paths.add(path.toFieldNameGml());

            return targetMapping;
        }

        return null;
    }

    private  boolean hasPath(String path) {
        for (String p: paths) {
            if (path.startsWith(p)) {
                return true;
            }
        }
        return false;
    }
}
