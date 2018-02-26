/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.geojson;


import de.ii.ldproxy.output.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ldproxy.output.geojson.GeoJsonMapping.GEO_JSON_TYPE;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer.GML_TYPE;
import de.ii.ogc.wfs.proxy.WfsProxyMappingProvider;
import de.ii.xsf.logging.XSFLogger;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import static de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer.GML_NS_URI;

/**
 * @author zahnen
 */
public class Gml2GeoJsonMappingProvider implements WfsProxyMappingProvider {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(Gml2GeoJsonMappingProvider.class);
    public static final String MIME_TYPE = "application/geo+json";

    @Override
    public String getTargetType() {
        return MIME_TYPE;
    }

    @Override
    public TargetMapping getTargetMappingForFeatureType(String nsUri, String localName) {
        return null;
    }

    @Override
    public TargetMapping getTargetMappingForAttribute(String path, String nsUri, String localName, GML_TYPE type) {

        if ((localName.equals("id") && nsUri.startsWith(GML_NS_URI)) || localName.equals("fid")) {

            LOGGER.getLogger().debug("ID {} {} {}", nsUri, localName, type);

            GEO_JSON_TYPE dataType = GEO_JSON_TYPE.forGmlType(type);

            if (dataType.isValid()) {
                GeoJsonPropertyMapping targetMapping = new GeoJsonPropertyMapping();
                targetMapping.setEnabled(true);
                targetMapping.setType(dataType);

                return targetMapping;
            }
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForProperty(String path, String nsUri, String localName, GML_TYPE type) {

        GEO_JSON_TYPE dataType = GEO_JSON_TYPE.forGmlType(type);

        if (dataType.isValid()) {
            LOGGER.getLogger().debug("PROPERTY {} {}", path, dataType);

            GeoJsonPropertyMapping targetMapping = new GeoJsonPropertyMapping();
            targetMapping.setEnabled(true);
            targetMapping.setType(dataType);

            return targetMapping;
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForGeometry(String path, String nsUri, String localName, GML_GEOMETRY_TYPE type) {

        GEO_JSON_GEOMETRY_TYPE geoType = GEO_JSON_GEOMETRY_TYPE.forGmlType(type);

        if (geoType.isValid()) {
            LOGGER.getLogger().debug("GEOMETRY {} {}", path, geoType);

            GeoJsonGeometryMapping targetMapping = new GeoJsonGeometryMapping();
            targetMapping.setEnabled(true);
            targetMapping.setType(GEO_JSON_TYPE.GEOMETRY);
            targetMapping.setGeometryType(geoType);

            return targetMapping;
        }

        return null;
    }
}
