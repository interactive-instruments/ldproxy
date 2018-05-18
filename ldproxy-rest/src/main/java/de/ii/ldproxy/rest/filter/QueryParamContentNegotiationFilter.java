/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest.filter;

import com.google.common.collect.ImmutableMap;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@PreMatching
public class QueryParamContentNegotiationFilter implements ContainerRequestFilter {
    private static final String CONTENT_TYPE_PARAMETER = "f";
    private static final String ACCEPT_HEADER = "Accept";
    private static final Map<String, MediaType> MIME_TYPES = new ImmutableMap.Builder<String, MediaType>()
            .put("json", MediaType.APPLICATION_JSON_TYPE)
            .put("jsonld", new MediaType("application","ld+json"))
            .put("geojson", new MediaType("application","geo+json"))
            .put("yaml", new MediaType("application","yaml"))
            .put("html", MediaType.TEXT_HTML_TYPE)
            .put("xml", MediaType.APPLICATION_XML_TYPE)
            .build();

    // TODO: verify, extension of core filter
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Quick check for a 'f' parameter
        if (!requestContext.getUriInfo().getQueryParameters().containsKey(CONTENT_TYPE_PARAMETER)) {
            // overwrite wildcard
            if (requestContext.getHeaderString(ACCEPT_HEADER) == null || requestContext.getHeaderString(ACCEPT_HEADER).length() == 0 || requestContext.getHeaders().getFirst(ACCEPT_HEADER).trim().equals("*/*")) {
                requestContext.getHeaders().putSingle(ACCEPT_HEADER, "text/html;q=1.0,application/xml;q=0.9,*/*;q=0.8");
            }
        } else {
            String format = requestContext.getUriInfo().getQueryParameters().getFirst(CONTENT_TYPE_PARAMETER);

            final MediaType accept = MIME_TYPES.get(format);
            if (accept != null) {
                requestContext.getHeaders().putSingle(ACCEPT_HEADER, accept.toString());
            }
        }
    }
}