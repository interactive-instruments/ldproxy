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
package de.ii.ldproxy.output.html;


import de.ii.ldproxy.output.html.MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE;
import de.ii.ldproxy.output.html.MicrodataMapping.MICRODATA_TYPE;
import de.ii.ogc.wfs.proxy.AbstractWfsProxyFeatureTypeAnalyzer;
import de.ii.ogc.wfs.proxy.TargetMapping;
import de.ii.ogc.wfs.proxy.WfsProxyService;
import de.ii.xsf.logging.XSFLogger;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 * @author zahnen
 */
public class Gml2MicrodataMapper extends AbstractWfsProxyFeatureTypeAnalyzer {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(Gml2MicrodataMapper.class);
    public static final String MIME_TYPE = "text/html";

    public Gml2MicrodataMapper(WfsProxyService proxyService) {
        super(proxyService);
    }

    @Override
    protected String getTargetType() {
        return MIME_TYPE;
    }

    @Override
    protected TargetMapping getTargetMappingForFeatureType(String nsuri, String localName) {
        LOGGER.getLogger().debug("FEATURETYPE {} {}", nsuri, localName);

        MicrodataPropertyMapping targetMapping = new MicrodataPropertyMapping();
        targetMapping.setEnabled(true);
        targetMapping.setItemType("http://schema.org/Place");

        return targetMapping;
    }

    @Override
    protected TargetMapping getTargetMappingForAttribute(String nsuri, String localName, String type, boolean required) {

        if ((localName.equals("id") && nsuri.startsWith(GML_NS_URI)) || localName.equals("fid")) {

            LOGGER.getLogger().debug("ID {} {} {}", nsuri, localName, type);

            MICRODATA_TYPE dataType = MICRODATA_TYPE.forGmlType(GML_TYPE.fromString(type));

            if (dataType.isValid()) {
                MicrodataPropertyMapping targetMapping = new MicrodataPropertyMapping();
                targetMapping.setEnabled(true);
                targetMapping.setShowInCollection(true);
                targetMapping.setName("@id");
                targetMapping.setType(dataType);

                return targetMapping;
            }
        }

        return null;
    }

    @Override
    protected TargetMapping getTargetMappingForProperty(String jsonPath, String nsuri, String localName, String type, long minOccurs, long maxOccurs, int depth, boolean isParentMultiple, boolean isComplex, boolean isObject) {

        MICRODATA_TYPE dataType = MICRODATA_TYPE.forGmlType(GML_TYPE.fromString(type));

        if (dataType.isValid()) {
            LOGGER.getLogger().debug("PROPERTY {} {}", jsonPath, dataType);

            MicrodataPropertyMapping targetMapping = new MicrodataPropertyMapping();
            targetMapping.setEnabled(true);
            targetMapping.setShowInCollection(true);
            targetMapping.setName(jsonPath);
            targetMapping.setType(dataType);

            return targetMapping;
        }

        MICRODATA_GEOMETRY_TYPE geoType = MICRODATA_GEOMETRY_TYPE.forGmlType(GML_GEOMETRY_TYPE.fromString(type));

        if (geoType.isValid()) {
            LOGGER.getLogger().debug("GEOMETRY {} {}", jsonPath, geoType);

            MicrodataGeometryMapping targetMapping = new MicrodataGeometryMapping();
            targetMapping.setEnabled(true);
            targetMapping.setShowInCollection(false);
            targetMapping.setName(jsonPath);
            targetMapping.setType(MICRODATA_TYPE.GEOMETRY);
            targetMapping.setGeometryType(geoType);

            return targetMapping;
        }

        LOGGER.getLogger().debug("NOT MAPPED {} {}", jsonPath, type);

        return null;
    }
}
