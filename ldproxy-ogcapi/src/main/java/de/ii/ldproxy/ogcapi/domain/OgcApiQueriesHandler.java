/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Objects;

public interface OgcApiQueriesHandler<T extends OgcApiQueryIdentifier> {

    Map<T, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers();

    default Response handle(T queryIdentifier, OgcApiQueryInput queryInput,
                            OgcApiRequestContext requestContext) {

        OgcApiQueryHandler<? extends OgcApiQueryInput> queryHandler = getQueryHandlers().get(queryIdentifier);

       if (Objects.isNull(queryHandler)) {
           throw new IllegalStateException("No query handler found for " + queryIdentifier);
       }

        if (!queryHandler.isValidInput(queryInput)) {
            throw new IllegalArgumentException();
        }

        return queryHandler.handle(queryInput, requestContext);
    }

}
