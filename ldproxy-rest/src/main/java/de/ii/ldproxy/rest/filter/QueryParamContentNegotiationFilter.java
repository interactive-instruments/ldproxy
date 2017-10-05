/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest.filter;

import com.google.common.collect.ImmutableMap;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 *
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class QueryParamContentNegotiationFilter implements ContainerRequestFilter {

    private static final String CONTENT_TYPE_PARAMETER = "f";
    private static final String ACCEPT_HEADER = "Accept";
    private static final Map<String, MediaType> MIME_TYPES = new ImmutableMap.Builder<String, MediaType>()
            .put("json", MediaType.APPLICATION_JSON_TYPE)
            .put("jsonld", new MediaType("application","ld+json"))
            .put("geojson", new MediaType("application","geo+json"))
            .put("html", MediaType.TEXT_HTML_TYPE)
            .put("xml", MediaType.APPLICATION_XML_TYPE)
            .build();

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        // Quick check for a 'f' parameter
        if (!request.getQueryParameters().containsKey(CONTENT_TYPE_PARAMETER)) {
            // overwrite wildcard
            if (request.getRequestHeader(ACCEPT_HEADER) == null || request.getRequestHeader(ACCEPT_HEADER).size() == 0 ||(request.getRequestHeader(ACCEPT_HEADER).size() == 1 && request.getRequestHeader(ACCEPT_HEADER).get(0).trim().equals("*/*"))) {
                request.getRequestHeaders().putSingle(ACCEPT_HEADER, "text/html;q=1.0,application/xml;q=0.9,*/*;q=0.8");
            }

            return request;
        }

        String format = request.getQueryParameters().getFirst(CONTENT_TYPE_PARAMETER);

        final MediaType accept = MIME_TYPES.get(format);
        if (accept != null) {
            request.getRequestHeaders().putSingle(ACCEPT_HEADER, accept.toString());
        }

        return request;
    }
}