/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.html;


import de.ii.ldproxy.output.html.MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE;
import de.ii.ldproxy.output.html.MicrodataMapping.MICRODATA_TYPE;
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
public class Gml2MicrodataMappingProvider implements WfsProxyMappingProvider {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(Gml2MicrodataMappingProvider.class);
    public static final String MIME_TYPE = "text/html";

    @Override
    public String getTargetType() {
        return MIME_TYPE;
    }

    @Override
    public TargetMapping getTargetMappingForFeatureType(String nsUri, String localName) {
        LOGGER.getLogger().debug("FEATURETYPE {} {}", nsUri, localName);

        MicrodataPropertyMapping targetMapping = new MicrodataPropertyMapping();
        targetMapping.setEnabled(true);
        targetMapping.setItemType("http://schema.org/Place");

        return targetMapping;
    }

    @Override
    public TargetMapping getTargetMappingForAttribute(String path, String nsUri, String localName, GML_TYPE type) {

        if ((localName.equals("id") && nsUri.startsWith(GML_NS_URI)) || localName.equals("fid")) {

            LOGGER.getLogger().debug("ID {} {} {}", nsUri, localName, type);

            MICRODATA_TYPE dataType = MICRODATA_TYPE.forGmlType(type);

            if (dataType.isValid()) {
                MicrodataPropertyMapping targetMapping = new MicrodataPropertyMapping();
                targetMapping.setEnabled(true);
                targetMapping.setShowInCollection(true);
                targetMapping.setType(dataType);

                return targetMapping;
            }
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForProperty(String path, String nsUri, String localName, GML_TYPE type) {

        MICRODATA_TYPE dataType = MICRODATA_TYPE.forGmlType(type);

        if (dataType.isValid()) {
            LOGGER.getLogger().debug("PROPERTY {} {}", path, dataType);

            MicrodataPropertyMapping targetMapping = new MicrodataPropertyMapping();
            targetMapping.setEnabled(true);
            targetMapping.setShowInCollection(true);
            targetMapping.setType(dataType);

            if (dataType == MICRODATA_TYPE.DATE && getTargetType().equals(MIME_TYPE)) {
                // TODO: move default format to global config
                targetMapping.setFormat("eeee, d MMMM yyyy[', 'HH:mm:ss[' 'z]]");
            }

            return targetMapping;
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForGeometry(String path, String nsUri, String localName, GML_GEOMETRY_TYPE type) {

        MICRODATA_GEOMETRY_TYPE geoType = MICRODATA_GEOMETRY_TYPE.forGmlType(type);

        if (geoType.isValid()) {
            LOGGER.getLogger().debug("GEOMETRY {} {}", path, geoType);

            MicrodataGeometryMapping targetMapping = new MicrodataGeometryMapping();
            targetMapping.setEnabled(true);
            targetMapping.setShowInCollection(false);
            targetMapping.setType(MICRODATA_TYPE.GEOMETRY);
            targetMapping.setGeometryType(geoType);

            return targetMapping;
        }

        return null;
    }
}
