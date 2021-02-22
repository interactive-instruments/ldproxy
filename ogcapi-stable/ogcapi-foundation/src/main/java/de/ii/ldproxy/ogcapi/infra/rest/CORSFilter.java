/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.util.Objects;

@Component
@Provides
@Instantiate
public class CORSFilter implements ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CORSFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        if (!requestContext.getUriInfo()
                           .getPath()
                           .startsWith("admin") &&
            // OPTIONS requests have their own endpoint
            !requestContext.getMethod()
                           .equalsIgnoreCase("OPTIONS")) {

            String secFetchMode = requestContext.getHeaderString("Sec-Fetch-Mode");
            String origin = requestContext.getHeaderString("Origin");
            if (!Objects.requireNonNullElse(origin, "").isEmpty() ||
                Objects.requireNonNullElse(secFetchMode, "").equalsIgnoreCase("cors")) {
                responseContext.getHeaders()
                        .add("Access-Control-Allow-Origin", "*");
                responseContext.getHeaders()
                        .add("Access-Control-Allow-Credentials", "true");
                String headers = "Link, Content-Crs"; // TODO add additional headers
                if (requestContext.getMethod().equalsIgnoreCase("POST"))
                    headers += ", Location";
                if (requestContext.getMethod().equalsIgnoreCase("PATCH"))
                    headers += ", Accept-Patch";
                responseContext.getHeaders()
                        .add("Access-Control-Expose-Headers", headers);
            }
        }
    }
}
