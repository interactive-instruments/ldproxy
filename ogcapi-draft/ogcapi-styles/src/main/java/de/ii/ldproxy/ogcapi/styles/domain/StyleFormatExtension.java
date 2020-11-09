/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;

import de.ii.ldproxy.ogcapi.domain.*;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * ApiExtension for a style encoding at /{serviceId}/styles/{styleId}
 */
public interface StyleFormatExtension extends FormatExtension {

    @Override
    default boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(StylesConfiguration.class)
                      .filter(StylesConfiguration::getEnabled)
                      .filter(config -> config.getStyleEncodings().contains(this.getMediaType().label()))
                      .isPresent();
    }

    @Override
    default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }

    @Override
    default String getPathPattern() {
        return "^/?styles/[^\\/]+/?$";
    }

    /**
     *
     * @return the file extension used for the stylesheets in the style store
     */
    String getFileExtension();

    /**
     *
     * @return the specification URL for the style metadata
     */
    String getSpecification();

    /**
     *
     * @return the version for the style metadata
     */
    String getVersion();

    /**
     * returns the title of a style
     *
     * @param datasetData information about the service {serviceId}
     * @param styleId the id of the style
     * @return the title of the style, if applicable, or null
     */
    default String getTitle(OgcApiDataV2 datasetData, String styleId) { return styleId; }

    /**
     *
     * @return {@code true}, if the style cannot be stored in the style encoding, but only be derived from an existing style encoding
     */
    default boolean getDerived() { return false; }

    /**
     *
     * @param stylesheet the stylesheet content
     * @param api
     * @param requestContext
     * @return the response
     */
    Response getStyleResponse(String styleId, File stylesheet, List<Link> links,
                              OgcApi api, ApiRequestContext requestContext) throws IOException;

};
