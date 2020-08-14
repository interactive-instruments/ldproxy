/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import de.ii.xtraplatform.crs.domain.EpsgCrs;

import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.*;

public interface OgcApiQueriesHandler<T extends OgcApiQueryIdentifier> {

    Map<T, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers();

    default Response handle(T queryIdentifier, OgcApiQueryInput queryInput,
                            OgcApiRequestContext requestContext) {

        OgcApiQueryHandler<? extends OgcApiQueryInput> queryHandler = getQueryHandlers().get(queryIdentifier);

       if (Objects.isNull(queryHandler)) {
           throw new IllegalStateException("No query handler found for " + queryIdentifier);
       }

        if (!queryHandler.isValidInput(queryInput)) {
            throw new RuntimeException(MessageFormat.format("Invalid query handler {0} for query input of class {1}.", queryHandler.getClass().getSimpleName(), queryInput.getClass().getSimpleName()));
        }

        return queryHandler.handle(queryInput, requestContext);
    }

    default Response.ResponseBuilder prepareSuccessResponse(OgcApiApi api,
                                                            OgcApiRequestContext requestContext) {
        return prepareSuccessResponse(api, requestContext, null);
    }

    default Response.ResponseBuilder prepareSuccessResponse(OgcApiApi api,
                                                            OgcApiRequestContext requestContext,
                                                            List<OgcApiLink> links) {
        return prepareSuccessResponse(api, requestContext, null, null);
    }

    default Response.ResponseBuilder prepareSuccessResponse(OgcApiApi api,
                                                            OgcApiRequestContext requestContext,
                                                            List<OgcApiLink> links,
                                                            EpsgCrs crs) {
        Response.ResponseBuilder response = Response.ok()
                                                    .type(requestContext
                                                        .getMediaType()
                                                        .type());

        Optional<Locale> language = requestContext.getLanguage();
        if (language.isPresent())
            response.language(language.get());

        if (links != null)
            // skip URI templates in the header as these are not RFC 8288 links
            links.stream()
                    .filter(link -> link.getTemplated()==null || !link.getTemplated())
                    .forEach(link -> response.links(link.getLink()));

        if (crs != null)
            response.header("Content-Crs", "<" + crs.toUriString() + ">");

        return response;
    }

}
