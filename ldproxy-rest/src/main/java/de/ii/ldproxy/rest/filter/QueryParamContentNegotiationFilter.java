/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
            .put("geojson", new MediaType("application","vnd.geo+json"))
            .put("html", MediaType.TEXT_HTML_TYPE)
            .put("xml", MediaType.APPLICATION_XML_TYPE)
            .build();

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        // Quick check for a 'f' parameter
        if (!request.getQueryParameters().containsKey(CONTENT_TYPE_PARAMETER)) {
            // overwrite wildcard
            if (request.getRequestHeader(ACCEPT_HEADER).size() == 1 && request.getRequestHeader(ACCEPT_HEADER).get(0).trim().equals("*/*")) {
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