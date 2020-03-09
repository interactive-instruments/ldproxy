/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

public interface ApiDefinitionFormatExtension extends FormatExtension {

    default String getPathPattern() {
        return "^/api/?$";
    }

    Response getApiDefinitionResponse(OgcApiApiDataV2 apiData,
                                      OgcApiRequestContext wfs3Request);

    default Response getApiDefinitionFile(OgcApiApiDataV2 apiData,
                                          OgcApiRequestContext wfs3Request,
                                          String file) {
        throw new NotFoundException();
    }
}
