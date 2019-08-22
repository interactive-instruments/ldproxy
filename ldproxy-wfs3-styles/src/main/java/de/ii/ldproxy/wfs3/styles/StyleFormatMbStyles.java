/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Component
@Provides
@Instantiate
public class StyleFormatMbStyles implements ConformanceClass, StyleFormatExtension {

    public static final String MEDIA_TYPE_STRING = "application/vnd.mapbox.style+json" ;
    static final OgcApiMediaType MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .main(new MediaType("application", "vnd.mapbox.style+json"))
            .label("Mapbox Style")
            .parameter("mbs")
            .metadata(MediaType.APPLICATION_JSON_TYPE)
            .build();

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/t15/opf-styles-1/1.0/conf/mapbox-styles";
    }

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData datasetData) {
        return getExtensionConfiguration(datasetData, StylesConfiguration.class).map(StylesConfiguration::getMbStyleEnabled).orElse(false);
    }

    @Override
    public OgcApiMediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public String getFileExtension() {
        return "mbs";
    }

    @Override
    public String getSpecification() {
        return "https://docs.mapbox.com/mapbox-gl-js/style-spec/";
    }

    @Override
    public String getVersion() {
        return "8";
    }

    /**
     * returns the title of a style, if a Mapbox Style stylesheet is available at /{serviceId}/styles/{styleId}
     *
     * @param datasetData information about the service {serviceId}
     * @param styleId the id of the style
     * @return true, if the conformance class is enabled and a stylesheet is available
     */
    @Override
    public String getTitle(OgcApiDatasetData datasetData, String styleId) {
        return "TODO";
    }
}
