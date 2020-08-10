/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.api;

import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiRequestContext;
import de.ii.ldproxy.ogcapi.observation_processing.application.Variables;

public interface ObservationProcessingOutputFormatVariables extends FormatExtension {

    default String getPathPattern() {
        String DAPA_PATH_ELEMENT = "dapa";
        return "^/collections/[\\w\\-]+/"+DAPA_PATH_ELEMENT+"/variables/?$";
    }

    Object getEntity(Variables variables, String collectionId, OgcApiApi api, OgcApiRequestContext requestContext);
}
