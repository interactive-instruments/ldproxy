/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;

/**
 * Extension for a style encoding at /{serviceId}/styles/{styleId}
 */
public interface StyleFormatExtension extends FormatExtension {

    @Override
    default boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return true;
    }

    default String getPathPattern() {
        return "^\\/?(?:styles(\\/[^\\/]+(\\/metadata)?)?)?$";
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
    String getTitle(OgcApiDatasetData datasetData, String styleId);

};
