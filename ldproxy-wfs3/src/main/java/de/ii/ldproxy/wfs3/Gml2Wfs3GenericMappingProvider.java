/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import de.ii.ldproxy.wfs3.api.Wfs3GenericMapping;
import de.ii.ldproxy.wfs3.api.Wfs3GenericMapping.GENERIC_TYPE;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.ii.xtraplatform.feature.provider.wfs.GmlFeatureTypeAnalyzer.GML_NS_URI;

/**
 * @author zahnen
 */
public class Gml2Wfs3GenericMappingProvider implements TargetMappingProviderFromGml {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gml2Wfs3GenericMappingProvider.class);
    public static final String MIME_TYPE = TargetMapping.BASE_TYPE;

    private boolean hasSpatialField;
    private boolean hasTemporalField;

    @Override
    public String getTargetType() {
        return MIME_TYPE;
    }

    @Override
    public TargetMapping getTargetMappingForFeatureType(String nsUri, String localName) {
        Wfs3GenericMapping targetMapping = new Wfs3GenericMapping();
        targetMapping.setEnabled(true);

        this.hasSpatialField = false;
        this.hasTemporalField = false;

        return targetMapping;
    }

    @Override
    public TargetMapping getTargetMappingForAttribute(String path, String nsUri, String localName, GML_TYPE type) {

        if ((localName.equals("id") && nsUri.startsWith(GML_NS_URI)) || localName.equals("fid")) {

            LOGGER.debug("ID {} {} {}", nsUri, localName, type);

            GENERIC_TYPE dataType = GENERIC_TYPE.forGmlType(type);

            if (dataType.isValid()) {
                Wfs3GenericMapping targetMapping = new Wfs3GenericMapping();
                targetMapping.setEnabled(true);
                targetMapping.setName("id");
                targetMapping.setType(dataType);

                return targetMapping;
            }
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForProperty(String path, String nsUri, String localName, GML_TYPE type) {

        GENERIC_TYPE dataType = GENERIC_TYPE.forGmlType(type);

        if (dataType.isValid()) {
            LOGGER.debug("PROPERTY {} {}", path, dataType);

            Wfs3GenericMapping targetMapping = new Wfs3GenericMapping();
            targetMapping.setEnabled(true);
            targetMapping.setName(path);
            targetMapping.setType(dataType);

            if (dataType.equals(GENERIC_TYPE.TEMPORAL) && !hasTemporalField) {
                targetMapping.setFilterable(true);
                this.hasTemporalField = true;
            }

            return targetMapping;
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForGeometry(String path, String nsUri, String localName, GML_GEOMETRY_TYPE type) {

        if (type.isValid()) {
            LOGGER.debug("GEOMETRY {} {}", path, type);

            Wfs3GenericMapping targetMapping = new Wfs3GenericMapping();
            targetMapping.setEnabled(true);
            targetMapping.setType(GENERIC_TYPE.SPATIAL);
            //TODO needed for reverse filter lookup
            targetMapping.setName("geometry");

            if (!hasSpatialField) {
                targetMapping.setFilterable(true);
                this.hasSpatialField = true;
            }

            return targetMapping;
        }

        return null;
    }
}
