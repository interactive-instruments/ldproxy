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


import de.ii.ldproxy.output.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ldproxy.output.geojson.GeoJsonMapping.GEO_JSON_TYPE;
import de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyService;
import de.ii.xsf.logging.XSFLogger;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 * @author zahnen
 */
public class Gml2GeoJsonMapper extends AbstractWfsProxyFeatureTypeAnalyzer {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(Gml2GeoJsonMapper.class);
    public static final String MIME_TYPE = "application/geo+json";

    public Gml2GeoJsonMapper(WfsProxyService proxyService) {
        super(proxyService);
    }

    @Override
    protected String getTargetType() {
        return MIME_TYPE;
    }

    @Override
    protected TargetMapping getTargetMappingForFeatureType(String nsuri, String localName) {
        return null;
    }

    @Override
    protected TargetMapping getTargetMappingForAttribute(String nsuri, String localName, String type, boolean required) {

        if ((localName.equals("id") && nsuri.startsWith(GML_NS_URI)) || localName.equals("fid")) {

            LOGGER.getLogger().debug("ID {} {} {}", nsuri, localName, type);

            GEO_JSON_TYPE dataType = GEO_JSON_TYPE.forGmlType(GML_TYPE.fromString(type));

            if (dataType.isValid()) {
                GeoJsonPropertyMapping targetMapping = new GeoJsonPropertyMapping();
                targetMapping.setEnabled(true);
                targetMapping.setName("id");
                targetMapping.setType(dataType);

                return targetMapping;
            }
        }

        return null;
    }

    @Override
    protected TargetMapping getTargetMappingForProperty(String jsonPath, String nsuri, String localName, String type, long minOccurs, long maxOccurs, int depth, boolean isParentMultiple, boolean isComplex, boolean isObject) {

        GEO_JSON_TYPE dataType = GEO_JSON_TYPE.forGmlType(GML_TYPE.fromString(type));

        if (dataType.isValid()) {
            LOGGER.getLogger().debug("PROPERTY {} {}", jsonPath, dataType);

            GeoJsonPropertyMapping targetMapping = new GeoJsonPropertyMapping();
            targetMapping.setEnabled(true);
            targetMapping.setName(jsonPath);
            targetMapping.setType(dataType);

            return targetMapping;
        }

        GEO_JSON_GEOMETRY_TYPE geoType = GEO_JSON_GEOMETRY_TYPE.forGmlType(GML_GEOMETRY_TYPE.fromString(type));

        if (geoType.isValid()) {
            LOGGER.getLogger().debug("GEOMETRY {} {}", jsonPath, geoType);

            GeoJsonGeometryMapping targetMapping = new GeoJsonGeometryMapping();
            targetMapping.setEnabled(true);
            targetMapping.setType(GEO_JSON_TYPE.GEOMETRY);
            targetMapping.setGeometryType(geoType);

            return targetMapping;
        }

        LOGGER.getLogger().debug("NOT MAPPED {} {}", jsonPath, type);

        return null;
    }
}
