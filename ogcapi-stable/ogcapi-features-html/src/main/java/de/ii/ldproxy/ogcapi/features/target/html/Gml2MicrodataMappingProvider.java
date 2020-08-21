/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.target.html;


import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.features.domain.legacy.TargetMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.ii.xtraplatform.feature.provider.wfs.GmlFeatureTypeAnalyzer.GML_NS_URI;

/**
 * @author zahnen
 */
public class Gml2MicrodataMappingProvider implements TargetMappingProviderFromGml {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gml2MicrodataMappingProvider.class);
    public static final String MIME_TYPE = "text/html";

    @Override
    public String getTargetType() {
        return MIME_TYPE;
    }

    @Override
    public TargetMapping getTargetMappingForFeatureType(String nsUri, String localName) {
        LOGGER.debug("FEATURETYPE {} {}", nsUri, localName);

        MicrodataPropertyMapping targetMapping = new MicrodataPropertyMapping();
        targetMapping.setEnabled(true);
        targetMapping.setItemType("http://schema.org/Place");

        return targetMapping;
    }

    @Override
    public TargetMapping getTargetMappingForAttribute(String path, String nsUri, String localName, GML_TYPE type) {

        if ((localName.equals("id") && nsUri.startsWith(GML_NS_URI)) || localName.equals("fid")) {

            LOGGER.debug("ID {} {} {}", nsUri, localName, type);

            MicrodataMapping.MICRODATA_TYPE dataType = MicrodataMapping.MICRODATA_TYPE.forGmlType(type);

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
    public TargetMapping getTargetMappingForProperty(String path, String nsUri, String localName, GML_TYPE type,
                                                     boolean isMultiple) {

        MicrodataMapping.MICRODATA_TYPE dataType = MicrodataMapping.MICRODATA_TYPE.forGmlType(type);

        if (dataType.isValid()) {
            LOGGER.debug("PROPERTY {} {}", path, dataType);

            MicrodataPropertyMapping targetMapping = new MicrodataPropertyMapping();
            targetMapping.setEnabled(true);
            targetMapping.setShowInCollection(true);
            targetMapping.setType(dataType);

            if (dataType == MicrodataMapping.MICRODATA_TYPE.DATE && getTargetType().equals(MIME_TYPE)) {
                // TODO: move default format to global config
                targetMapping.setFormat("eeee, d MMMM yyyy[', 'HH:mm:ss[' 'z]]");
            }

            return targetMapping;
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForGeometry(String path, String nsUri, String localName, GML_GEOMETRY_TYPE type) {

        MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE geoType = MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE.forGmlType(type.toSimpleFeatureGeometry());

        if (geoType.isValid()) {
            LOGGER.debug("GEOMETRY {} {}", path, geoType);

            MicrodataGeometryMapping targetMapping = new MicrodataGeometryMapping();
            targetMapping.setEnabled(true);
            targetMapping.setShowInCollection(false);
            targetMapping.setType(MicrodataMapping.MICRODATA_TYPE.GEOMETRY);
            targetMapping.setGeometryType(geoType);

            return targetMapping;
        }

        return null;
    }
}
