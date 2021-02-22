/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.api;

import de.ii.ldproxy.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.Processing;

public interface DapaOverviewFormatExtension extends GenericFormatExtension {

    default String getPathPattern() {
        String DAPA_PATH_ELEMENT = "processes";
        return "^/collections/[\\w\\-]+/"+ DAPA_PATH_ELEMENT+"/?$";
    }

    Object getEntity(Processing processList, String collectionId, OgcApi api, ApiRequestContext requestContext);
}
