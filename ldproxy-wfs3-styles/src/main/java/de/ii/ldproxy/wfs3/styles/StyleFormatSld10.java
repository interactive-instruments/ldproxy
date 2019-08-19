/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.api.*;
import de.ii.xtraplatform.kvstore.api.KeyNotFoundException;
import de.ii.xtraplatform.kvstore.api.KeyValueStore;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class StyleFormatSld10 implements Wfs3ConformanceClass, StyleFormatExtension {

    @Requires
    private KeyValueStore keyValueStore;

    static final Wfs3MediaType MEDIA_TYPE = ImmutableWfs3MediaType.builder()
            .main(new MediaType("application", "vnd.ogc.sld+xml", ImmutableMap.of("version","1.0")))
            .label("OGC SLD 1.0")
            .build();

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/t15/opf-styles-1/1.0/conf/sld-10";
    }

    @Override
    public boolean isConformanceEnabledForService(Wfs3ServiceData serviceData) {
        return isEnabledForService(serviceData);
    }

    @Override
    public boolean isEnabledForService(Wfs3ServiceData serviceData) {
        Optional<StylesConfiguration> stylesExtension = getExtensionConfiguration(serviceData, StylesConfiguration.class);

        if (stylesExtension.isPresent() &&
                stylesExtension.get()
                        .getSld10Enabled()) {
            return true;
        }
        return false;
    }


    @Override
    public Wfs3MediaType getMediaType() {
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
     * @param serviceData information about the service {serviceId}
     * @param styleId the id of the style
     * @return true, if the conformance class is enabled and a stylesheet is available
     */
    @Override
    public String getTitle(Wfs3ServiceData serviceData, String styleId) {
        return "TODO";
    }

    /**
     * Fetch a style by id
     *
     * @param serviceData information about the service {serviceId}
     * @param styleId the local identifier of a specific style
     * @return the style in a json file
     */
    @Override
    public Response getStyle(Wfs3ServiceData serviceData, String styleId) throws NotAcceptableException {

        if (!isEnabledForService(serviceData)) {
            throw new NotAcceptableException();
        }

        KeyValueStore stylesStore = keyValueStore.getChildStore("styles")
                                                 .getChildStore(serviceData.getId());

        BufferedReader style_file = null;
        String key = styleId + "." + getFileExtension();
        if (stylesStore.containsKey(key)) {
            try {
                style_file = EndpointStyles.getStyleDocument(stylesStore, styleId, getFileExtension());
            } catch (KeyNotFoundException e1) {
                throw new ServerErrorException(500); // TODO: should not occur at this point
            } catch (IOException e2) {
                throw new ServerErrorException(500); // TODO: internal error in the styles store
            }
        } else if (stylesStore.containsKey(styleId + ".metadata")) {
            throw new NotAcceptableException();
        } else {
            throw new NotFoundException();
        }

        if (style_file==null) {
            throw new NotAcceptableException();
        }

        return Response.ok(style_file)
                       .build();
    }
}
