/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.core.MediaType;
import java.util.List;

@Component
@Provides
@Instantiate
public class StyleFormatSld10 implements ConformanceClass, StyleFormatExtension {

    public static final String MEDIA_TYPE_STRING = "application/vnd.ogc.sld+xml;version=1.0" ;
    static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "vnd.ogc.sld+xml", ImmutableMap.of("version", "1.0")))
            .label("OGC SLD 1.0")
            .parameter("sld10")
            .build();

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/t15/opf-styles-1/1.0/conf/sld-10");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return getExtensionConfiguration(apiData, StylesConfiguration.class).map(StylesConfiguration::getSld10Enabled)
                                                                                .orElse(false);
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public String getFileExtension() {
        return "sld10";
    }

    @Override
    public String getSpecification() {
        return "http://www.opengeospatial.org/standards/sld";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    /**
     * returns the title of a style, if a SLD 1.0 stylesheet is available at /{serviceId}/styles/{styleId}
     *
     * @param datasetData information about the service {serviceId}
     * @param styleId     the id of the style
     * @return true, if the conformance class is enabled and a stylesheet is available
     */
    @Override
    public String getTitle(OgcApiApiDataV2 datasetData, String styleId) {
        return "TODO";
    }
}
