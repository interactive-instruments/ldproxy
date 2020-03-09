/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import org.immutables.value.Value;

import javax.ws.rs.core.Response;
import java.util.function.BiFunction;

@Value.Immutable
public abstract class OgcApiQueryHandler<U extends OgcApiQueryInput> {

    public static <U extends OgcApiQueryInput> OgcApiQueryHandler<U> with(Class<U> queryInputType,
                                                                          BiFunction<U, OgcApiRequestContext, Response> queryHandler) {
        return new ImmutableOgcApiQueryHandler.Builder<U>()
                .queryInputType(queryInputType)
                .queryHandler(queryHandler)
                .build();
    }

    protected abstract Class<U> getQueryInputType();

    protected abstract BiFunction<U, OgcApiRequestContext, Response> getQueryHandler();

    public boolean isValidInput(OgcApiQueryInput queryInput) {
        return getQueryInputType().isAssignableFrom(queryInput.getClass());
    }

    public Response handle(OgcApiQueryInput queryInput, OgcApiRequestContext requestContext) {
        return getQueryHandler().apply(getQueryInputType().cast(queryInput), requestContext);
    }
}
