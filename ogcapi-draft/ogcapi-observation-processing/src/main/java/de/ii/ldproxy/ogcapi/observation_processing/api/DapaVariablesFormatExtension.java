/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.api;

import de.ii.ldproxy.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.observation_processing.application.Variables;

public interface DapaVariablesFormatExtension extends GenericFormatExtension {

    default String getPathPattern() {
        return "^/collections/[\\w\\-]+/variables/?$";
    }

    Object getEntity(Variables variables, String collectionId, OgcApi api, ApiRequestContext requestContext);
}
