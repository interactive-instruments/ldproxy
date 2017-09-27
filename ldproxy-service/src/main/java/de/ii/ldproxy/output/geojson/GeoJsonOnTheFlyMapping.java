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

import de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyOnTheFlyMapping;
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
