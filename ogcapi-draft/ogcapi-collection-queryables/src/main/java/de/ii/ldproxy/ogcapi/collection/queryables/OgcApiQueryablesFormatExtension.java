/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.queryables;

import de.ii.ldproxy.ogcapi.domain.*;
import io.swagger.v3.oas.models.media.Schema;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

public interface OgcApiQueryablesFormatExtension extends FormatExtension {

    default String getPathPattern() {
        return "^\\/?collections\\/[^\\/]+\\/queryables/?$";
    }

    Response getResponse(Queryables queryables, String collectionId, OgcApiApi api, OgcApiRequestContext requestContext);

}
