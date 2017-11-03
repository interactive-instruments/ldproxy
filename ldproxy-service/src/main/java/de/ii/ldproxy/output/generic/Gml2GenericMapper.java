/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.output.generic;

import de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyService;
import de.ii.xsf.logging.XSFLogger;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 * @author zahnen
 */
public class Gml2GenericMapper extends AbstractWfsProxyFeatureTypeAnalyzer {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(Gml2GenericMapper.class);
    public static final String MIME_TYPE = TargetMapping.BASE_TYPE;

    public Gml2GenericMapper(WfsProxyService proxyService) {
        super(proxyService);
    }

    @Override
    protected String getTargetType() {
        return MIME_TYPE;
    }

    @Override
    protected TargetMapping getTargetMappingForFeatureType(String nsuri, String localName) {
        GenericMapping targetMapping = new GenericMapping();
        targetMapping.setEnabled(true);

        return targetMapping;
    }

    @Override
    protected TargetMapping getTargetMappingForAttribute(String nsuri, String localName, String type, boolean required) {

        if ((localName.equals("id") && nsuri.startsWith(GML_NS_URI)) || localName.equals("fid")) {

            LOGGER.getLogger().debug("ID {} {} {}", nsuri, localName, type);

            GML_TYPE dataType = GML_TYPE.fromString(type);

            if (dataType.isValid()) {
                GenericMapping targetMapping = new GenericMapping();
                targetMapping.setEnabled(true);
                targetMapping.setName("id");

                return targetMapping;
            }
        }

        return null;
    }

    @Override
    protected TargetMapping getTargetMappingForProperty(String jsonPath, String nsuri, String localName, String type, long minOccurs, long maxOccurs, int depth, boolean isParentMultiple, boolean isComplex, boolean isObject) {

        GML_TYPE dataType = GML_TYPE.fromString(type);

        if (dataType.isValid()) {
            LOGGER.getLogger().debug("PROPERTY {} {}", jsonPath, dataType);

            GenericMapping targetMapping = new GenericMapping();
            targetMapping.setEnabled(true);
            targetMapping.setName(jsonPath);

            return targetMapping;
        }

        GML_GEOMETRY_TYPE geoType = GML_GEOMETRY_TYPE.fromString(type);

        if (geoType.isValid()) {
            LOGGER.getLogger().debug("GEOMETRY {} {}", jsonPath, geoType);

            GenericMapping targetMapping = new GenericMapping();
            targetMapping.setEnabled(true);
            targetMapping.setGeometry(true);

            return targetMapping;
        }

        LOGGER.getLogger().debug("NOT MAPPED {} {}", jsonPath, type);

        return null;
    }
}
