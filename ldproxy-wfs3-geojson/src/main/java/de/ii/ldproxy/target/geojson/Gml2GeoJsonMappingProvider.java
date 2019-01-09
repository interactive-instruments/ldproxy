/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;


import de.ii.ldproxy.target.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ldproxy.target.geojson.GeoJsonMapping.GEO_JSON_TYPE;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.ii.xtraplatform.feature.provider.wfs.GmlFeatureTypeAnalyzer.GML_NS_URI;

/**
 * @author zahnen
 */
public class Gml2GeoJsonMappingProvider implements TargetMappingProviderFromGml {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gml2GeoJsonMappingProvider.class);
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

            LOGGER.debug("ID {} {} {}", nsUri, localName, type);

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
            LOGGER.debug("PROPERTY {} {}", path, dataType);

            GeoJsonPropertyMapping targetMapping = new GeoJsonPropertyMapping();
            targetMapping.setEnabled(true);
            targetMapping.setType(dataType);

            return targetMapping;
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForGeometry(String path, String nsUri, String localName, GML_GEOMETRY_TYPE type) {

        GEO_JSON_GEOMETRY_TYPE geoType = GEO_JSON_GEOMETRY_TYPE.forGmlType(type.toSimpleFeatureGeometry());

        if (geoType.isValid()) {
            LOGGER.debug("GEOMETRY {} {}", path, geoType);

            GeoJsonGeometryMapping targetMapping = new GeoJsonGeometryMapping();
            targetMapping.setEnabled(true);
            targetMapping.setType(GEO_JSON_TYPE.GEOMETRY);
            targetMapping.setGeometryType(geoType);

            return targetMapping;
        }

        return null;
    }
}
