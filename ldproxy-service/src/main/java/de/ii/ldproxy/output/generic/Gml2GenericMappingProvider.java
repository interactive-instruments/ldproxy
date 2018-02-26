/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.generic;

import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer;
import de.ii.ogc.wfs.proxy.WfsProxyMappingProvider;
import de.ii.xsf.logging.XSFLogger;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import static de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeAnalyzer.GML_NS_URI;

/**
 * @author zahnen
 */
public class Gml2GenericMappingProvider implements WfsProxyMappingProvider {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(Gml2GenericMappingProvider.class);
    public static final String MIME_TYPE = TargetMapping.BASE_TYPE;

    @Override
    public String getTargetType() {
        return MIME_TYPE;
    }

    @Override
    public TargetMapping getTargetMappingForFeatureType(String nsUri, String localName) {
        GenericMapping targetMapping = new GenericMapping();
        targetMapping.setEnabled(true);

        return targetMapping;
    }

    @Override
    public TargetMapping getTargetMappingForAttribute(String path, String nsUri, String localName, WfsProxyFeatureTypeAnalyzer.GML_TYPE type) {

        if ((localName.equals("id") && nsUri.startsWith(GML_NS_URI)) || localName.equals("fid")) {

            LOGGER.getLogger().debug("ID {} {} {}", nsUri, localName, type);

            if (type.isValid()) {
                GenericMapping targetMapping = new GenericMapping();
                targetMapping.setEnabled(true);
                targetMapping.setName("id");

                return targetMapping;
            }
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForProperty(String path, String nsUri, String localName, WfsProxyFeatureTypeAnalyzer.GML_TYPE type) {

        if (type.isValid()) {
            LOGGER.getLogger().debug("PROPERTY {} {}", path, type);

            GenericMapping targetMapping = new GenericMapping();
            targetMapping.setEnabled(true);
            targetMapping.setName(path);

            return targetMapping;
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForGeometry(String path, String nsUri, String localName, WfsProxyFeatureTypeAnalyzer.GML_GEOMETRY_TYPE type) {

        if (type.isValid()) {
            LOGGER.getLogger().debug("GEOMETRY {} {}", path, type);

            GenericMapping targetMapping = new GenericMapping();
            targetMapping.setEnabled(true);
            targetMapping.setGeometry(true);

            return targetMapping;
        }

        return null;
    }
}
