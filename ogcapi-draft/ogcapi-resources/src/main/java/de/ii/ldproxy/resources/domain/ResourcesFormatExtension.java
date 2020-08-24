/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources.domain;

import de.ii.ldproxy.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.resources.app.Resources;

import javax.ws.rs.core.Response;

public interface ResourcesFormatExtension extends GenericFormatExtension {

    @Override
    default String getPathPattern() {
        return "^/resources/?$";
    }

    Response getResourcesResponse(Resources resources,
                                  OgcApi api,
                                  ApiRequestContext requestContext);
}
